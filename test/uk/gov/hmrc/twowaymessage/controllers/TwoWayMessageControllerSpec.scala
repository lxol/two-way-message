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

package uk.gov.hmrc.twowaymessage.controllers

import java.util.UUID

import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import com.codahale.metrics.SharedMetricRegistries
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.Inside.inside
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.gform.dms.DmsMetadata
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.twowaymessage.model._
import uk.gov.hmrc.twowaymessage.services.TwoWayMessageService

import scala.concurrent.Future
import scala.concurrent.duration.Duration

class TwoWayMessageControllerSpec extends WordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar {

  val mockMessageService = mock[TwoWayMessageService]

  val injector = new GuiceApplicationBuilder()
    .overrides(bind[TwoWayMessageService].to(mockMessageService))
    .injector()

  val controller = injector.instanceOf[TwoWayMessageController]
  implicit val hc: HeaderCarrier = mock[HeaderCarrier]

  val twoWayMessageGood = Json.parse("""
                                       |    {
                                       |      "contactDetails": {
                                       |         "email":"someEmail@test.com"
                                       |      },
                                       |      "subject":"QUESTION",
                                       |      "content":"SGVsbG8gV29ybGQ="
                                       |    }""".stripMargin)

  val twoWayMessageBadContent = Json.parse("""
                                             |    {
                                             |     "subject":"QUESTION"
                                             |    }""".stripMargin)

  val twoWayMessageReplyGood = Json.parse("""
                                            |    {
                                            |     "subject":"answer",
                                            |     "content":"Some base64-encoded HTML"
                                            |    }""".stripMargin)

  "TwoWayMessageController" should {

    "return 201 (Created) when a message is successfully created in the message service " in {
      val nino = Nino("AB123456C")
      when(mockMessageService.post(anyString, org.mockito.ArgumentMatchers.eq(nino), any[TwoWayMessage],any[DmsMetadata])(
        any[HeaderCarrier]))
        .thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))
      val result = await(controller.validateAndPostMessage("p800", nino, twoWayMessageGood))
      result.header.status shouldBe Status.CREATED
    }

    "return 201 (Created) when an advisor reply is successfully created in the message service " in {
      when(mockMessageService.postAdvisorReply(any[TwoWayMessageReply], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))
      val result = await(controller.validateAndPostAdvisorResponse(twoWayMessageReplyGood, "replyToId"))
      result.header.status shouldBe Status.CREATED
    }

    "return 400 (Bad Request) if the advisor reply body is not as per the definition " in {
      val result = await(controller.validateAndPostAdvisorResponse(twoWayMessageBadContent, "replyToId"))
      result.header.status shouldBe Status.BAD_REQUEST
    }

    "return 201 (Created) when an customer reply is successfully created in the message service " in {
      when(mockMessageService.postCustomerReply(any[TwoWayMessageReply], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))
      val result = await(controller.validateAndPostCustomerResponse(twoWayMessageReplyGood, "replyToId"))
      result.header.status shouldBe Status.CREATED
    }

    "return 400 (Bad Request) if the customer reply body is not as per the definition " in {
      val result = await(controller.validateAndPostCustomerResponse(twoWayMessageBadContent, "replyToId"))
      result.header.status shouldBe Status.BAD_REQUEST
    }

    "return 200 (Ok) when metadata for a valid message id is requested correctly" in {
      val dummyMetadata = MessageMetadata("123", TaxEntity("abc", TaxIdWithName("a","b")), "subject", MetadataDetails(None,None,None))
      when(mockMessageService.getMessageMetadata(any[String])(any[HeaderCarrier])).thenReturn(Future.successful(Some(dummyMetadata)))
      val result = await(controller.getRecipientMetadata("123")(FakeRequest()))
      result.header.status shouldBe Status.OK
    }

    "return 404 (Not Found) when metadata for a invalid message id is requested correctly" in {
      //val dummyMetadata = MessageMetadata("123", TaxEntity("abc", TaxIdWithName("a","b")), "subject", MetadataDetails(None,None,None))
      when(mockMessageService.getMessageMetadata(any[String])(any[HeaderCarrier])).thenReturn(Future.successful(None))
      val result = await(controller.getRecipientMetadata("123")(FakeRequest()))
      result.header.status shouldBe Status.NOT_FOUND
    }

    SharedMetricRegistries.clear
  }

}