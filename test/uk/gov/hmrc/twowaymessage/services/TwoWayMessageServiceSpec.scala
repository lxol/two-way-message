/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.twowaymessage.services


import com.codahale.metrics.SharedMetricRegistries
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.mvc.Http
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.twowaymessage.connectors.MessageConnector
import uk.gov.hmrc.twowaymessage.model._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.parsing.json.JSONObject

class TwoWayMessageServiceSpec extends WordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar {

  implicit val mockExecutionContext = mock[ExecutionContext]
  implicit val mockHeaderCarrier = mock[HeaderCarrier]
  val mockMessageConnector = mock[MessageConnector]


  lazy val mockhttpClient = mock[HttpClient]
  lazy val mockServiceConfig = mock[ServicesConfig]

  val injector = new GuiceApplicationBuilder()
    .overrides(bind[MessageConnector].to(mockMessageConnector))
    .injector()


  val messageService = injector.instanceOf[TwoWayMessageService]

  "TwoWayMessageService.post" should {

    val twoWayMessageExample = TwoWayMessage(
      Recipient(
        TaxIdentifier(
          "HMRC_ID",
          "AB123456C"
        ),
        "someEmail@test.com"
      ),
      "Question",
      Some("SGVsbG8gV29ybGQ="),
      Option.empty
    )

    val twoWayMessageReplyExample = TwoWayMessage(
      Recipient(
        TaxIdentifier(
          "HMRC_ID",
          "AB123456C"
        ),
        "someEmail@test.com"
      ),
      "Question",
      Some("SGVsbG8gV29ybGQ="),
      Option.apply("replyId")
    )

    "return 201 (Created) when a message is successfully created by the message service" in {
      when(mockMessageConnector
        .postMessage(any[Message])(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(
            HttpResponse(Http.Status.CREATED, Some(Json.parse("{\"id\":\"5c18eb2e6f0000100204b161\"}")))
          )
        )

      val messageResult = await(messageService.post(twoWayMessageExample))
      messageResult.header.status shouldBe 201
    }

    "return 502 (Bad Gateway) when posting a message to the message service fails" in {
      when(mockMessageConnector.postMessage(any[Message])(any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(Http.Status.BAD_REQUEST)))
      val messageResult = await(messageService.post(twoWayMessageExample))
      messageResult.header.status shouldBe 502
    }

    SharedMetricRegistries.clear
  }

  "TwoWayMessageService.postReply" should {

    val messageMetadata = MessageMetadata(
      "5c18eb166f0000110204b160",
      TaxEntity(
        "REGIME",
        TaxIdWithName("HMRC-NI", "AB123456C"),
        Some("someEmail@test.com")
      ),
      "SUBJECT")

    "return 201 (Created) when a message is successfully created by the message service" in {

      when(mockMessageConnector.getMessageMetadata(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(messageMetadata))

      when(mockMessageConnector
        .postMessage(any[Message])(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(
            HttpResponse(Http.Status.CREATED, Some(Json.parse("{\"id\":\"5c18eb2e6f0000100204b161\"}")))))

      val messageResult = await(messageService.postReply(
        TwoWayMessageReply("Some content"), "some-reply-to-message-id"))
      messageResult.header.status shouldBe 201
    }

    "return 502 (Bad Gateway) when posting a message to the message service fails" in {

      when(mockMessageConnector.getMessageMetadata(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(messageMetadata))

      when(mockMessageConnector.postMessage(any[Message])(any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(Http.Status.BAD_REQUEST)))

      val messageResult = await(messageService.postReply(
        TwoWayMessageReply("Some content"), "some-reply-to-message-id"))
      messageResult.header.status shouldBe 502
    }

    SharedMetricRegistries.clear
  }

  "Generated JSON" should {

    "be correct for a two-way message posted by a customer" in {
      val expected =
        Message(
          ExternalRef("123412342314", "2WSM"),
          Recipient(TaxIdentifier("nino", "AB123456C"), "email@test.com"),
          "2wsm-customer",
          "QUESTION",
          "some base64-encoded-html",
          Details("2WSM-question")
        )

      val originalMessage = TwoWayMessage(
        Recipient(
          TaxIdentifier("nino", "AB123456C"),
          "email@test.com"
        ),
        "QUESTION",
        Option.apply("some base64-encoded-html"))

      val actual = messageService.createJsonForMessage("123412342314", originalMessage)
      assert(actual.equals(expected))
    }

    "be correct for a two-way message replied to by an advisor" in {
      val expected = Message(
       ExternalRef("some-random-id", "2WSM"),
         Recipient(TaxIdentifier("nino", "AB123456C"), "email@test.com"),
         "2wsm-advisor",
         "RE: QUESTION",
         "some base64-encoded-html",
         Details("2WSM-reply", Option.apply("reply-to-id"))
       )

      val metadata = MessageMetadata(
       "mongo-id",
       TaxEntity("regime", TaxIdWithName("nino", "AB123456C"), Some("email@test.com")),
       "QUESTION")

      val reply = TwoWayMessageReply("some base64-encoded-html")
      val actual = messageService.createJsonForReply("some-random-id", metadata, reply, "reply-to-id")
      assert(actual.equals(expected))
  }
 }
}
