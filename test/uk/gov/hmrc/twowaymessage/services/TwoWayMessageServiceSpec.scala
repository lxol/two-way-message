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
import play.api.test.Helpers._
import play.mvc.Http
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.twowaymessage.connectors.MessageConnector
import uk.gov.hmrc.twowaymessage.model.{Message, Recipient, TaxIdentifier, TwoWayMessage}

import scala.concurrent.{ExecutionContext, Future}

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

  val twoWayMessageExample = TwoWayMessage(
    Recipient(
      TaxIdentifier(
        "HMRC_ID",
        "AB123456C"
      ),
      "someEmail@test.com"
    ),
    "Question",
    Some("SGVsbG8gV29ybGQ=")
  )

  "TwoWayMessageService should" should {

    "return 201 (Created) when a message is successfully created in the message service " in {
      when(mockMessageConnector.postMessage(any[Message])(any[HeaderCarrier])).thenReturn(Future.successful(HttpResponse(Http.Status.OK)))
      val messageResult = await(messageService.post(twoWayMessageExample))
      messageResult.header.status shouldBe 201
    }

    "return 502 (Bad Gateway) if any error is received from the message service " in {
      when(mockMessageConnector.postMessage(any[Message])(any[HeaderCarrier])).thenReturn(Future.successful(HttpResponse(Http.Status.BAD_REQUEST)))
      val messageResult = await(messageService.post(twoWayMessageExample))
      messageResult.header.status shouldBe 502
    }

    SharedMetricRegistries.clear
  }

}
