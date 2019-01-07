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

package uk.gov.hmrc.twowaymessage.services

import com.codahale.metrics.SharedMetricRegistries
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.HttpEntity.Strict
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

    val ninoId = "AB123456C"
    val twoWayMessageExample = TwoWayMessage(
      "someEmail@test.com",
      "Question",
      Some("SGVsbG8gV29ybGQ="),
      Option.empty
    )

    val twoWayMessageReplyExample = TwoWayMessage(
      "someEmail@test.com",
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

      val messageResult = await(messageService.post(ninoId, twoWayMessageExample))
      messageResult.header.status shouldBe 201
    }

    "return 502 (Bad Gateway) when posting a message to the message service fails" in {
      when(mockMessageConnector.postMessage(any[Message])(any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(Http.Status.BAD_REQUEST)))
      val messageResult = await(messageService.post(ninoId, twoWayMessageExample))
      messageResult.header.status shouldBe 502
    }

    SharedMetricRegistries.clear
  }

  "TwoWayMessageService.postAdvisorReply" should {

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

      val messageResult = await(messageService.postAdvisorReply(
        TwoWayMessageReply("Some content"), "some-reply-to-message-id"))
      messageResult.header.status shouldBe 201
    }

    "return 502 (Bad Gateway) when posting a message to the message service fails with a 409 Conflict" in {

      when(mockMessageConnector.getMessageMetadata(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(messageMetadata))

      val postMessageResponse = HttpResponse(Http.Status.CONFLICT,
        responseString = Some("POST of 'http://localhost:8910/messages' returned 409. Response body: '{\"reason\":\"Duplicated message content or external reference ID\"}'"))

      when(mockMessageConnector.postMessage(any[Message])(any[HeaderCarrier]))
        .thenReturn(Future.successful(postMessageResponse))

      val messageResult = await(messageService.postAdvisorReply(
        TwoWayMessageReply("Some content"), "some-reply-to-message-id"))
      messageResult.header.status shouldBe 502
      messageResult.body.asInstanceOf[Strict].data.utf8String shouldBe
        "{\"error\":409,\"message\":\"POST of 'http://localhost:8910/messages' returned 409. Response body: '{\\\"reason\\\":\\\"Duplicated message content or external reference ID\\\"}'\"}"
    }

    "return 502 (Bad Gateway) when posting a message to the message service and getMessageMetadata fails with a 400 Bad Request (Invalid ID format)" in {

      when(mockMessageConnector.getMessageMetadata(any[String])(any[HeaderCarrier]))
        .thenReturn(
          Future.failed(
            new uk.gov.hmrc.http.BadRequestException(
              "GET of 'http://localhost:8910/messages/5c2dec526900006b000d53b/metadata' returned 400 (Bad Request). Response body '{ \"status\": 400, \"message\": \"A client error occurred: ID 5c2dec526900006b000d53b was invalid\" } '")
          )
        )

      val messageResult = await(messageService.postCustomerReply(
        TwoWayMessageReply("Some content"), "some-reply-to-message-id"))
      messageResult.header.status shouldBe 502
      messageResult.body.asInstanceOf[Strict].data.utf8String shouldBe
        "{\"error\":400,\"message\":\"GET of 'http://localhost:8910/messages/5c2dec526900006b000d53b/metadata' returned 400 (Bad Request). Response body '{ \\\"status\\\": 400, \\\"message\\\": \\\"A client error occurred: ID 5c2dec526900006b000d53b was invalid\\\" } '\"}"
    }

    SharedMetricRegistries.clear
  }

  "TwoWayMessageService.postCustomerReply" should {

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

      val messageResult = await(messageService.postCustomerReply(
        TwoWayMessageReply("Some content"), "some-reply-to-message-id"))
      messageResult.header.status shouldBe 201
    }

    "return 502 (Bad Gateway) when posting a message to the message service fails with a 409 Conflict" in {

      when(mockMessageConnector.getMessageMetadata(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(messageMetadata))

      val postMessageResponse = HttpResponse(Http.Status.CONFLICT,
        responseString = Some("POST of 'http://localhost:8910/messages' returned 409. Response body: '{\"reason\":\"Duplicated message content or external reference ID\"}'"))

      when(mockMessageConnector.postMessage(any[Message])(any[HeaderCarrier]))
        .thenReturn(Future.successful(postMessageResponse))

      val messageResult = await(messageService.postCustomerReply(
        TwoWayMessageReply("Some content"), "some-reply-to-message-id"))
      messageResult.header.status shouldBe 502
      messageResult.body.asInstanceOf[Strict].data.utf8String shouldBe
        "{\"error\":409,\"message\":\"POST of 'http://localhost:8910/messages' returned 409. Response body: '{\\\"reason\\\":\\\"Duplicated message content or external reference ID\\\"}'\"}"
    }

    "return 502 (Bad Gateway) when posting a message to the message service and getMessageMetadata fails with a 404 Not Found (Unable to find ID)" in {

      when(mockMessageConnector.getMessageMetadata(any[String])(any[HeaderCarrier]))
        .thenReturn(
          Future.failed(
            new uk.gov.hmrc.http.NotFoundException(
              "GET of 'http://localhost:8910/messages/5c2dec526900006b000d53b1/metadata' returned 404 (Not Found). Response body: ''")
          )
        )

      val messageResult = await(messageService.postCustomerReply(
        TwoWayMessageReply("Some content"), "some-reply-to-message-id"))
      messageResult.header.status shouldBe 502
      messageResult.body.asInstanceOf[Strict].data.utf8String shouldBe
        "{\"error\":404,\"message\":\"GET of 'http://localhost:8910/messages/5c2dec526900006b000d53b1/metadata' returned 404 (Not Found). Response body: ''\"}"
    }

    SharedMetricRegistries.clear
  }

  "Generated JSON" should {

    "be correct for a two-way message posted by a customer" in {
      val expected =
        Message(
          ExternalRef("123412342314", "2WSM"),
          Recipient(TaxIdentifier("nino", "AB123456C"), "email@test.com"),
          MessageType.Customer,
          "QUESTION",
          "some base64-encoded-html",
          Details(FormId.Question)
        )

      val originalMessage = TwoWayMessage(
          "email@test.com",
        "QUESTION",
        Some("some base64-encoded-html"))

      val ninoId = "AB123456C"
      val actual = messageService.createJsonForMessage("123412342314",  MessageType.Customer, FormId.Question, originalMessage, ninoId)
      assert(actual.equals(expected))
    }

    "be correct for a two-way message replied to by an advisor" in {
      val expected = Message(
        ExternalRef("some-random-id", "2WSM"),
        Recipient(TaxIdentifier("nino", "AB123456C"), "email@test.com"),
        MessageType.Advisor,
        "RE: QUESTION",
        "some base64-encoded-html",
        Details(FormId.Reply, Some("reply-to-id"))
       )

      val metadata = MessageMetadata(
       "mongo-id",
       TaxEntity("regime", TaxIdWithName("nino", "AB123456C"), Some("email@test.com")),
       "QUESTION")

      val reply = TwoWayMessageReply("some base64-encoded-html")
      val actual = messageService.createJsonForReply("some-random-id", MessageType.Advisor, FormId.Reply, metadata, reply, "reply-to-id")
      assert(actual.equals(expected))
  }
 }
}
