/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.gform.dms.DmsMetadata
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.twowaymessage.assets.Fixtures
import uk.gov.hmrc.twowaymessage.connector.mocks.MockAuthConnector
import uk.gov.hmrc.twowaymessage.model.MessageType.MessageType
import uk.gov.hmrc.twowaymessage.model._
import uk.gov.hmrc.twowaymessage.services.TwoWayMessageService

import scala.concurrent.Future
import scala.concurrent.duration.Duration

class TwoWayMessageControllerSpec
    extends WordSpec with Matchers with GuiceOneAppPerSuite with Fixtures with MockitoSugar with MockAuthConnector {

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
      val name = Name(Option("Firstname"), Option("Surname"))
      when(
        mockMessageService
          .post(anyString, org.mockito.ArgumentMatchers.eq(nino), any[TwoWayMessage], any[DmsMetadata], any[Name])(
            any[HeaderCarrier]))
        .thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))
      val result = await(controller.validateAndPostMessage("p800", nino, twoWayMessageGood, name))
      result.header.status shouldBe Status.CREATED
    }

    "return 201 (Created) when an adviser reply is successfully created in the message service " in {
      when(mockMessageService.postAdviserReply(any[TwoWayMessageReply], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))
      val result = await(controller.validateAndPostAdviserResponse(twoWayMessageReplyGood, "replyToId"))
      result.header.status shouldBe Status.CREATED
    }

    "return 400 (Bad Request) if the adviser reply body is not as per the definition " in {
      val result = await(controller.validateAndPostAdviserResponse(twoWayMessageBadContent, "replyToId"))
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
      val dummyMetadata = MessageMetadata(
        "123",
        TaxEntity("abc", TaxIdWithName("a", "b")),
        "subject",
        MetadataDetails(None, None, None),
        None,
        Some("08 May 2019"))
      when(mockMessageService.getMessageMetadata(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(dummyMetadata)))
      val result = await(controller.getRecipientMetadata("123")(FakeRequest()))
      result.header.status shouldBe Status.OK
    }

    "return 404 (Not Found) when metadata for a invalid message id is requested correctly" in {
      //val dummyMetadata = MessageMetadata("123", TaxEntity("abc", TaxIdWithName("a","b")), "subject", MetadataDetails(None,None,None))
      when(mockMessageService.getMessageMetadata(any[String])(any[HeaderCarrier])).thenReturn(Future.successful(None))
      val result = await(controller.getRecipientMetadata("123")(FakeRequest()))
      result.header.status shouldBe Status.NOT_FOUND
    }

    "return 200 (Ok) when message content for a valid message id is requested correctly" in {
      val dummyContent = "dummy content"
      when(mockMessageService.getMessageContentBy(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(dummyContent)))
      val result = await(controller.getRecipientMessageContentBy("123")(FakeRequest()))
      result.header.status shouldBe Status.OK
    }

    "return 404 (Not Found) when content for a invalid message id is requested correctly" in {
      when(mockMessageService.getMessageContentBy(any[String])(any[HeaderCarrier])).thenReturn(Future.successful(None))
      val result = await(controller.getRecipientMessageContentBy("123")(FakeRequest()))
      result.header.status shouldBe Status.NOT_FOUND
    }

    "return 200 (Ok) when messages are  requested correctly" in {
      when(mockMessageService.findMessagesBy(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(List(testConversationItem, testConversationItem))))
      val result = await(controller.getMessagesListBy("123")(FakeRequest()))
      result.header.status shouldBe Status.OK
    }
    "return 400 () when messages are  requested incorrectly" in {
      when(mockMessageService.findMessagesBy(any[String])(any[HeaderCarrier])).thenReturn(Future.successful(Left("")))
      val result = await(controller.getMessagesListBy("123")(FakeRequest()))
      result.header.status shouldBe Status.BAD_REQUEST
    }

    "return 200 (Ok) when message list size is requested correctly" in {
      when(mockMessageService.findMessagesBy(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(List(testConversationItem, testConversationItem))))
      val result = await(controller.getMessagesListSizeBy("123")(FakeRequest()))
      result.header.status shouldBe Status.OK
    }
    "return 400 () when message list size is requested incorrectly" in {
      when(mockMessageService.findMessagesBy(any[String])(any[HeaderCarrier])).thenReturn(Future.successful(Left("")))
      val result = await(controller.getMessagesListSizeBy("123")(FakeRequest()))
      result.header.status shouldBe Status.BAD_REQUEST
    }

    "return response time when response time is requested for valid enquiry type" in {
      val result: Future[Result] = controller.getCurrentResponseTime("p800")(FakeRequest())
      contentAsString(result) shouldEqual """{"responseTime":"5 days"}"""
    }

    "return 404 when response time requested for invalid enquiry type" in {
      val result = await(controller.getCurrentResponseTime("IP4U")(FakeRequest()))
      result.header.status shouldBe 404
    }

    "return 200 when p800 enquiryType is requested" in {
      val result = controller.getEnquiryTypeDetails("p800")(FakeRequest())
      await(result).header.status shouldBe Status.OK
      Json.parse(contentAsString(result)) shouldBe
        Json.parse("""{
                     |"displayName":"P800 underpayment",
                     |"responseTime":"5 days"
                     |}""".stripMargin)
    }

    "return 200 when p800-overpayment enquiryType is requested" in {
      val result = controller.getEnquiryTypeDetails("p800-overpayment")(FakeRequest())
      await(result).header.status shouldBe Status.OK
      Json.parse(contentAsString(result)) shouldBe
        Json.parse("""{
                     |"displayName":"P800 overpayment enquiry",
                     |"responseTime":"5 days"
                     |}""".stripMargin)
    }

    "return 200 when p800-paid enquiryType is requested" in {
      val result = controller.getEnquiryTypeDetails("p800-paid")(FakeRequest())
      await(result).header.status shouldBe Status.OK
      Json.parse(contentAsString(result)) shouldBe
        Json.parse("""{
                     |"displayName":"P800 overpayment paid enquiry",
                     |"responseTime":"5 days"
                     |}""".stripMargin)
    }

    "return 200 when p800-processing enquiryType is requested" in {
      val result = controller.getEnquiryTypeDetails("p800-processing")(FakeRequest())
      await(result).header.status shouldBe Status.OK
      Json.parse(contentAsString(result)) shouldBe
        Json.parse("""{
                     |"displayName":"P800 overpayment processing enquiry",
                     |"responseTime":"5 days"
                     |}""".stripMargin)
    }

    "return 200 when p800-sent enquiryType is requested" in {
      val result = controller.getEnquiryTypeDetails("p800-sent")(FakeRequest())
      await(result).header.status shouldBe Status.OK
      Json.parse(contentAsString(result)) shouldBe
        Json.parse("""{
                     |"displayName":"P800 overpayment sent enquiry",
                     |"responseTime":"5 days"
                     |}""".stripMargin)
    }

    "return 200 when p800-not-available enquiryType is requested" in {
      val result = controller.getEnquiryTypeDetails("p800-not-available")(FakeRequest())
      await(result).header.status shouldBe Status.OK
      Json.parse(contentAsString(result)) shouldBe
        Json.parse("""{
                     |"displayName":"P800 overpayment not available enquiry",
                     |"responseTime":"5 days"
                     |}""".stripMargin)
    }

    "return 200 when p800-underpayment enquiryType is requested" in {
      val result = controller.getEnquiryTypeDetails("p800-underpayment")(FakeRequest())
      await(result).header.status shouldBe Status.OK
      Json.parse(contentAsString(result)) shouldBe
        Json.parse("""{
                     |"displayName":"P800 underpayment",
                     |"responseTime":"5 days"
                     |}""".stripMargin)
    }

    "return 404 when incorrect enquiryType is requested" in {
      val result = controller.getEnquiryTypeDetails("i am not valid")(FakeRequest())
      await(result).header.status shouldBe Status.NOT_FOUND
    }

    SharedMetricRegistries.clear
  }

}
