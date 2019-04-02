/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.twowaymessage.connectors

import com.codahale.metrics.SharedMetricRegistries
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Mode
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Json, JsSuccess}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.twowaymessage.model._
import uk.gov.hmrc.twowaymessage.model.MessageMetadataFormat._

import scala.concurrent.ExecutionContext

class MessageConnectorSpec extends WordSpec with WithWireMock with Matchers with GuiceOneAppPerSuite with MockitoSugar {

  lazy val mockhttpClient = mock[HttpClient]
  implicit lazy val mockHeaderCarrier = new HeaderCarrier()
  lazy val mockServiceConfig = mock[ServicesConfig]
  lazy implicit val ec = mock[ExecutionContext]

  val injector = new GuiceApplicationBuilder()
    .overrides(bind[Mode].to(Mode.Test))
    .injector()

  val messageConnector = injector.instanceOf[MessageConnector]

  val messageExample = Message(
    ExternalRef(
      "123412342314",
      "2WSM-CUSTOMER"
    ),
    Recipient(
      TaxIdentifier(
        "HMRC-NI",
        "AB123456C"
      ),
      "someEmail@test.com"
    ),
    MessageType.Customer,
    "SUBJECT",
    "SGVsbG8gV29ybGQ=",
    Details(FormId.Question)
  )

  "POST message connector" should {

    "return 201" in {
      val jsonResponseBody =
        """
          |{
          |   "externalRef":{
          |      "id":"123412342314",
          |      "source":"2WSM-CUSTOMER"
          |   },
          |   "recipient":{
          |      "taxIdentifier":{
          |         "name":"HMRC-NI",
          |         "value":"AB123456C"
          |      },
          |      "email":"someEmail@test.com"
          |   },
          |   "messageType":"2wsm-customer",
          |   "subject":"SUBJECT",
          |   "content":"SGVsbG8gV29ybGQ=",
          |   "details":{
          |      "formId":"2WSM-question"
          |   }
          |}
        """.stripMargin

      givenThat(
        post(urlEqualTo("/messages"))
          .withRequestBody(equalToJson(jsonResponseBody))
          .willReturn(aResponse().withStatus(Status.CREATED)))

      val result = await(messageConnector.postMessage(messageExample)(new HeaderCarrier()))
      result.status shouldBe (201)
    }
    SharedMetricRegistries.clear
  }

  "GET message metadata via message connector" should {

    "returns 200 successfully for a valid replyTo message identifier" in {
      val jsonResponseBody =
        """
          |{
          |   "id": "5c18eb166f0000110204b160",
          |   "recipient": {
          |      "regime": "REGIME",
          |      "identifier": {
          |         "name":"HMRC-NI",
          |         "value":"AB123456C"
          |      },
          |      "email":"someEmail@test.com"
          |   },
          |   "subject":"SUBJECT",
          |   "details": {
          |     "threadId":"5d12eb115f0000000205c150",
          |     "enquiryType":"p800",
          |     "adviser": {
          |       "pidId":"adviser-id"
          |     }
          |   }
          |}
        """.stripMargin

      val replyTo = "replyToId"
      givenThat(
        get(urlEqualTo(s"/messages/$replyTo/metadata"))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withBody(jsonResponseBody)))

      val httpResult = await(messageConnector.getMessageMetadata(replyTo)(new HeaderCarrier()))
      httpResult.status shouldBe (200)
      Json.parse(httpResult.body).validate[MessageMetadata] shouldBe a[JsSuccess[MessageMetadata]]
    }
    SharedMetricRegistries.clear
  }
}

trait WithWireMock extends BeforeAndAfterAll with BeforeAndAfterEach {
  suite: Suite =>

  def dependenciesPort = 8910

  lazy val wireMockServer = new WireMockServer(wireMockConfig().port(dependenciesPort))

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMockServer.start()
    WireMock.configureFor(dependenciesPort)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    wireMockServer.resetMappings()
    wireMockServer.resetRequests()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMockServer.stop()
  }

}
