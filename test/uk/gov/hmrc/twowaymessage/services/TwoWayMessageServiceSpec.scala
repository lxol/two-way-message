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

import akka.util.Timeout
import com.codahale.metrics.SharedMetricRegistries
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{ Matchers, WordSpec }
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.HttpEntity.Strict
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.mvc.Http
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.gform.dms.{ DmsHtmlSubmission, DmsMetadata }
import uk.gov.hmrc.gform.gformbackend.GformConnector
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.twowaymessage.assets.Fixtures
import uk.gov.hmrc.twowaymessage.connectors.MessageConnector
import uk.gov.hmrc.twowaymessage.model.MessageFormat._
import uk.gov.hmrc.twowaymessage.model.MessageMetadataFormat._
import uk.gov.hmrc.twowaymessage.model._
import uk.gov.hmrc.twowaymessage.services.RenderType.ReplyType

import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }

class TwoWayMessageServiceSpec extends WordSpec with Matchers with GuiceOneAppPerSuite with Fixtures with MockitoSugar {

  implicit val mockExecutionContext = mock[ExecutionContext]
  implicit val mockHeaderCarrier = mock[HeaderCarrier]
  val mockMessageConnector = mock[MessageConnector]
  val mockGformConnector = mock[GformConnector]
  val mockHtmlCreationService = mock[HtmlCreatorService]

  lazy val mockhttpClient = mock[HttpClient]
  lazy val mockServiceConfig = mock[ServicesConfig]

  val injector = new GuiceApplicationBuilder()
    .overrides(bind[MessageConnector].to(mockMessageConnector))
    .overrides(bind[GformConnector].to(mockGformConnector))
    .overrides(bind[HtmlCreatorService].to(mockHtmlCreationService))
    .injector()

  val messageService = injector.instanceOf[TwoWayMessageService]

  val twoWayMessageReplyExample = TwoWayMessage(
    ContactDetails("someEmail@test.com"),
    "Question",
    "SGVsbG8gV29ybGQ=",
    Option.apply("replyId")
  )

  val dmsMetadataExample = DmsMetadata("", "AB123456C", "", "")

  "TwoWayMessageService.post" should {

    val nino = Nino("AB123456C")
    val twoWayMessageExample = TwoWayMessage(
      ContactDetails("someEmail@test.com"),
      "Question",
      "SGVsbG8gV29ybGQ=",
      Option.empty
    )

    val conversationItem = List(
      ConversationItem(
        "5d02201b5b0000360151779e",
        "Matt Test 1",
        Some(
          ConversationItemDetails(
            MessageType.Adviser,
            FormId.Reply,
            Some(LocalDate.parse("2019-06-13")),
            Some("5d021fbe5b0000200151779c"),
            Some("P800"))),
        LocalDate.parse("2019-06-13"),
        Some(
          "Dear TestUser Thank you for your message of 13 June 2019.</br>To recap your question, " +
            "I think you're asking for help with</br>I believe this answers your question and hope you are satisfied with the response. " +
            "There's no need to send a reply. " +
            "But if you think there's something important missing, just ask another question about this below." +
            "</br>Regards</br>Matthew Groom</br>HMRC digital team.")
      ))

    "return 201 (Created) when a message is successfully created by the message service" in {
      when(
        mockMessageConnector
          .postMessage(any[Message])(any[HeaderCarrier])
      ).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.CREATED, Some(Json.parse("{\"id\":\"5c18eb2e6f0000100204b161\"}")))
        )
      )
      when(
        mockMessageConnector
          .getMessages(any[String])(any[HeaderCarrier])
      ).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.OK, Some(Json.toJson(conversationItem)))
        )
      )
      when(
        mockGformConnector
          .submitToDmsViaGform(any[DmsHtmlSubmission])(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.CREATED)
        )
      )
      when(
        mockHtmlCreationService.createHtmlForPdf(any[String], any[String], any[List[ConversationItem]], any[String])
      ).thenReturn(
        Future.successful(Right(<html/>.mkString))
      )

      val name = Name(Option("firstname"), Option("surname"))
      val messageResult = await(messageService.post("p800", nino, twoWayMessageExample, dmsMetadataExample, name))
      messageResult.header.status shouldBe 201
    }

    "return 502 (Bad Gateway) when posting a message to the message service fails" in {
      when(mockMessageConnector.postMessage(any[Message])(any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(Http.Status.BAD_REQUEST)))
      val name = Name(Option("firstname"), Option("surname"))
      val messageResult = await(messageService.post("p800", nino, twoWayMessageExample, dmsMetadataExample, name))
      messageResult.header.status shouldBe 502
    }

    SharedMetricRegistries.clear
  }

  "TwoWayMessageService.postAdviserReply" should {

    val messageMetadata = MessageMetadata(
      "5c18eb166f0000110204b160",
      TaxEntity(
        "REGIME",
        TaxIdWithName("HMRC-NI", "AB123456C"),
        Some("someEmail@test.com")
      ),
      "SUBJECT",
      details = MetadataDetails(
        threadId = Some("5c18eb166f0000110204b160"),
        enquiryType = Some("P800"),
        adviser = Some(Adviser("adviser-id"))
      ),
      None,
      Some("08 May 2019")
    )

    "return 201 (Created) when a message is successfully created by the message service" in {

      when(mockMessageConnector.getMessageMetadata(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(Http.Status.OK, Some(Json.toJson(messageMetadata)))))

      when(
        mockMessageConnector
          .postMessage(any[Message])(any[HeaderCarrier]))
        .thenReturn(Future.successful(
          HttpResponse(Http.Status.CREATED, Some(Json.parse("{\"id\":\"5c18eb2e6f0000100204b161\"}")))))

      val messageResult =
        await(messageService.postAdviserReply(TwoWayMessageReply("Some content"), "some-reply-to-message-id"))
      messageResult.header.status shouldBe 201
    }

    "return 502 (Bad Gateway) when posting a message to the message service fails with a 409 Conflict" in {

      when(mockMessageConnector.getMessageMetadata(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(Http.Status.OK, Some(Json.toJson(messageMetadata)))))

      val postMessageResponse = HttpResponse(
        Http.Status.CONFLICT,
        responseString = Some(
          "POST of 'http://localhost:8910/messages' returned 409. Response body: '{\"reason\":\"Duplicated message content or external reference ID\"}'")
      )

      when(mockMessageConnector.postMessage(any[Message])(any[HeaderCarrier]))
        .thenReturn(Future.successful(postMessageResponse))

      val messageResult =
        await(messageService.postAdviserReply(TwoWayMessageReply("Some content"), "some-reply-to-message-id"))
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

      val messageResult =
        await(messageService.postCustomerReply(TwoWayMessageReply("Some content"), "some-reply-to-message-id"))
      messageResult.header.status shouldBe 502
      messageResult.body.asInstanceOf[Strict].data.utf8String shouldBe
        "{\"error\":400,\"message\":\"GET of 'http://localhost:8910/messages/5c2dec526900006b000d53b/metadata' returned 400 (Bad Request). Response body '{ \\\"status\\\": 400, \\\"message\\\": \\\"A client error occurred: ID 5c2dec526900006b000d53b was invalid\\\" } '\"}"
    }

    SharedMetricRegistries.clear
  }

  "TwoWayMessageService.postCustomerReply" should {

    val messageMetadata = MessageMetadata(
      id = "5c18eb166f0000110204b160",
      recipient = TaxEntity(
        "REGIME",
        TaxIdWithName("HMRC-NI", "AB123456C"),
        Some("someEmail@test.com")
      ),
      subject = "SUBJECT",
      details = MetadataDetails(
        threadId = Some("5c18eb166f0000110204b160"),
        enquiryType = Some("p800"),
        adviser = Some(Adviser("adviser-id"))
      ),
      None,
      Some("08 May 2019")
    )

    "return 201 (Created) when a message is successfully created by the message service" in {

      when(mockMessageConnector.getMessageMetadata(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(Http.Status.OK, Some(Json.toJson(messageMetadata)))))

      when(
        mockMessageConnector
          .postMessage(any[Message])(any[HeaderCarrier]))
        .thenReturn(Future.successful(
          HttpResponse(Http.Status.CREATED, Some(Json.parse("{\"id\":\"5c18eb2e6f0000100204b161\"}")))))

      val messageResult =
        await(messageService.postCustomerReply(TwoWayMessageReply("Some content"), "some-reply-to-message-id"))
      messageResult.header.status shouldBe 201
    }

    "return 502 (Bad Gateway) when posting a message to the message service fails with a 409 Conflict" in {

      when(mockMessageConnector.getMessageMetadata(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(Http.Status.OK, Some(Json.toJson(messageMetadata)))))

      val postMessageResponse = HttpResponse(
        Http.Status.CONFLICT,
        responseString = Some(
          "POST of 'http://localhost:8910/messages' returned 409. Response body: '{\"reason\":\"Duplicated message content or external reference ID\"}'")
      )

      when(mockMessageConnector.postMessage(any[Message])(any[HeaderCarrier]))
        .thenReturn(Future.successful(postMessageResponse))

      val messageResult =
        await(messageService.postCustomerReply(TwoWayMessageReply("Some content"), "some-reply-to-message-id"))
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

      val messageResult =
        await(messageService.postCustomerReply(TwoWayMessageReply("Some content"), "some-reply-to-message-id"))
      messageResult.header.status shouldBe 502
      messageResult.body.asInstanceOf[Strict].data.utf8String shouldBe
        "{\"error\":404,\"message\":\"GET of 'http://localhost:8910/messages/5c2dec526900006b000d53b1/metadata' returned 404 (Not Found). Response body: ''\"}"
    }

    SharedMetricRegistries.clear
  }

  "TwoWayMessageService.findMessagesListBy" should {

    val fixtureMessages = conversationItems("5c2dec526900006b000d53b1", "5c2dec526900006b000d53b1")
    "return a list of messages if successfully fetched from the message service" in {
      when(
        mockMessageConnector
          .getMessages(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(Http.Status.OK, Some(Json.parse(fixtureMessages)))))
      val messagesResult = await(messageService.findMessagesBy("1234567890"))
      messagesResult.right.get.head.validFrom.toString should be("2013-12-01")
    }

    val invalidFixtureMessages = "{}"
    "return error if invalid message list json" in {
      when(
        mockMessageConnector
          .getMessages(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(Http.Status.OK, Some(Json.parse(invalidFixtureMessages)))))
      val messagesResult = await(messageService.findMessagesBy("1234567890"))
      messagesResult.right should not be (None)
    }
    SharedMetricRegistries.clear
  }

  "Generated JSON" should {

    "be correct for a two-way message posted by a customer" in {
      val taxpayerName =
        TaxpayerName(forename = Option("firstname"), surname = Option("surname"), line1 = Option("firstname surname"))
      val expected =
        Message(
          ExternalRef("123412342314", "2WSM"),
          Recipient(TaxIdentifier("nino", "AB123456C"), "email@test.com", Option(taxpayerName)),
          MessageType.Customer,
          "QUESTION",
          "some base64-encoded-html",
          Details(FormId.Question, None, None, enquiryType = Some("p800"), waitTime = Some("7 days"))
        )

      val originalMessage = TwoWayMessage(ContactDetails("email@test.com"), "QUESTION", "some base64-encoded-html")
      val nino = Nino("AB123456C")
      val name = Name(Option("firstname"), Option("surname"))
      val actual = messageService.createJsonForMessage("123412342314", originalMessage, nino, "p800", name)
    }

    "be correct for a two-way message replied to by an adviser" in {
      val expected = Message(
        ExternalRef("some-random-id", "2WSM"),
        Recipient(TaxIdentifier("nino", "AB123456C"), "email@test.com"),
        MessageType.Adviser,
        "QUESTION",
        "some base64-encoded-html",
        Details(FormId.Reply, Some("reply-to-id"), Some("thread-id"), Some("P800"), Some(Adviser(pidId = "adviser-id")))
      )

      val metadata = MessageMetadata(
        "mongo-id",
        TaxEntity("regime", TaxIdWithName("nino", "AB123456C"), Some("email@test.com")),
        "QUESTION",
        MetadataDetails(
          threadId = Some("thread-id"),
          enquiryType = Some("P800"),
          adviser = Some(Adviser(pidId = "adviser-id"))
        ),
        None,
        Some("08 May 2019")
      )

      val reply = TwoWayMessageReply("some base64-encoded-html")
      val actual = messageService
        .createJsonForReply(None, "some-random-id", MessageType.Adviser, FormId.Reply, metadata, reply, "reply-to-id")
      assert(actual.equals(expected))
    }

    "when deriving users name when full name is there, then it should be expected" in {
      val name = Name(Option("Firstname"), Option("Surname"))
      val actual = messageService.deriveAddressedName(name)
      val expected = Option("Firstname Surname")
      assert(actual.equals(expected))
    }

    "when deriving users name when only first name is there, then it should be expected" in {
      val name = Name(Option("Firstname"), None)
      val actual = messageService.deriveAddressedName(name)
      val expected = Option("Firstname")
      assert(actual.equals(expected))
    }

    "when deriving users name when only no name is present, then None should be expected" in {
      val name = Name(None, None)
      val actual = messageService.deriveAddressedName(name)
      val expected = None
      assert(actual.equals(expected))
    }
  }

  val htmlMessageExample = TwoWayMessage(
    ContactDetails("someEmail@test.com"),
    "This looks wrong I need it changed",
    "SSB0aGluayB0aGF0IHRoZSBhc3Nlc3NtZW50IGZvciBsYXN0IHllYXIgaXMgaW5jb3JyZWN0LiBJdCBzaG93cyBteSBDb21wYW55IGNhciB3aXRoIEImUSBhbmQgaXQgYWxzbyBzaG93cyB0aGF0IEkgd2FzIHJlY2VpdmluZyBhIGZ1ZWwgYmVuZWZpdC4KCkkgZGlkIHN0aWxsIGhhdmUgbXkgRm9yZCBGb2N1cyBjb21wYW55IGNhciBsYXN0IHllYXIgYnV0IEImUSBjaGFuZ2VkIHRoZWlyIHBvbGljeSBvbiBmdWVsLiBXZSBub3cgc3VibWl0IGEgbW9udGhseSBzaGVldCBzaG93aW5nIGFsbCBvdXIgYnVzaW5lc3MgYW5kIHBlcnNvbmFsIG1pbGVhZ2UuIEImUSBwYXlyb2xsIHRoZW4gY2hhcmdlIHVzIGJhY2sgdGhlIHBlcmNlbnRhZ2Ugb2YgcGVyc29uYWwgbWlsZWFnZSBpbiBvdXIgbmV4dCBwYXkgc2xpcC4KCkImUSBjaGFuZ2VkIHRoZSBwb2xpY3kgaW4gQXByaWwgMjAxOC4KCkkgdGhpbmsgdGhpcyBtZWFucyB5b3Ugd2lsbCBvd2UgbWUgc29tZSBtb25leSBvbiB0YXggcmF0aGVyIHRoYW4gbXkgb3duaW5nIG1vbmV5IHRvIHlvdS4=",
    Option.empty
  )

  "TwoWayMessageService.getMessageContentBy" should {
    "return Html content of the message" in {

      val htmlString = <h1 class="govuk-heading-xl margin-top-small margin-bottom-small">Incorrect tax bill</h1>
        <p class="faded-text--small">You sent this message on 12 March, 2019</p>
        <p>What happens if I refuse to pay?</p>
          <hr/>
        <h2 class="govuk-heading-xl margin-top-small margin-bottom-small">Incorrect tax bill</h2>
        <p class="faded-text--small">This message was sent to you on 12 March, 2019</p>
        <p>I'm sorry but this tax bill is for you and you need to pay it.

        You can pay it online of at your bank.</p>
          <hr/>
        <h2 class="govuk-heading-xl margin-top-small margin-bottom-small">Incorrect tax bill</h2>
        <p class="faded-text--small">You sent this message on 12 March, 2019</p>
        <p>I have been sent a tax bill that I'm sure is for someone else as I don't earn any money. Please can you check.</p>.mkString

      when(mockMessageConnector.getMessageContent(any[String])(any[HeaderCarrier])).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.OK, None, Map.empty, Some(htmlString))
        )
      )
      val actualHtml = await(messageService.getMessageContentBy("123"))
      assert(actualHtml.get.contains(htmlString))

    }

    "return None if unable to get message content" in {
      when(mockMessageConnector.getMessageContent(any[String])(any[HeaderCarrier])).thenReturn(
        Future.successful(HttpResponse(Http.Status.BAD_GATEWAY))
      )
      val actualHtml = await(messageService.getMessageContentBy("123"))
      actualHtml shouldBe None
    }
  }

  "TwoWayMessageService.getLatestMessage" should {
    "return the latest message as Html" in {

      val conversationItem = ConversationItem(
        "5d021fbe5b0000200151779c",
        "Matt Test 1",
        Some(
          ConversationItemDetails(
            MessageType.Customer,
            FormId.Question,
            Some(LocalDate.parse("2019-06-13")),
            None,
            Some("p800"))),
        LocalDate.parse("2019-06-13"),
        Some("Hello, my friend!")
      )

      val conversationItemAsJson = Json.toJson(conversationItem.toString.stripMargin)

      val htmlConvervsationItem = Html.apply(<h1
        class="govuk-heading-xl margin-top-small margin-bottom-small">
          Matt Test 1
        </h1><p class="faded-text--small">
          You sent this message on 13 June 2019
        </p><p>
          Hello, my friend!
        </p>.mkString)
    }
  }

  val listOfConversationItems = List(
    ConversationItem(
      "5d02201b5b0000360151779e",
      "Matt Test 1",
      Some(
        ConversationItemDetails(
          MessageType.Adviser,
          FormId.Reply,
          Some(LocalDate.parse("2019-06-13")),
          Some("5d021fbe5b0000200151779c"),
          Some("P800"))),
      LocalDate.parse("2019-06-13"),
      Some(
        "Dear TestUser Thank you for your message of 13 June 2019.<br/>To recap your question, " +
          "I think you're asking for help with<br/>I believe this answers your question and hope you are satisfied with the response. " +
          "There's no need to send a reply. " +
          "But if you think there's something important missing, just ask another question about this below." +
          "<br/>Regards<br/>Matthew Groom<br/>HMRC digital team.")
    ),
    ConversationItem(
      "5d021fbe5b0000200151779c",
      "Matt Test 1",
      Some(
        ConversationItemDetails(
          MessageType.Customer,
          FormId.Question,
          Some(LocalDate.parse("2019-06-13")),
          Some("p800"))),
      LocalDate.parse("2019-06-13"),
      Some("Hello, my friend!")
    )
  )

  val htmlConversationItems = Html.apply(<h1 class="govuk-heading-xl margin-top-small margin-bottom-small">
    Matt Test 1
  </h1><p class="faded-text--small">
    This message was sent to you on 13 June 2019
  </p><p>
    Dear TestUser Thank you for your message of 13 June 2019.<br/>To recap your question, I think you're asking for help with<br/>I believe this answers your question and hope you are satisfied with the response. There's no need to send a reply. But if you think there's something important missing, just ask another question about this below.<br/>Regards<br/>Matthew Groom<br/>HMRC digital team.
  </p><a href="/two-way-message-frontend/message/customer/P800/5d02201b5b0000360151779e/reply#reply-input-label">Send another message about this</a><h2
  class="govuk-heading-xl margin-top-small margin-bottom-small">
    Matt Test 1
  </h2><p class="faded-text--small">
    You sent this message on 13 June 2019
  </p><p>
    Hello, my friend!
  </p>.mkString)

  val jsonConversationItems = Json.toJson(listOfConversationItems.toString.stripMargin)

  "TwoWayMessageService.getPreviousMessages" should {

    "return a list of messages as Html" in {
      when(mockMessageConnector.getMessages(any[String])(any[HeaderCarrier])).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.OK, Some(jsonConversationItems), Map.empty, None)
        )
      )

      when(
        mockHtmlCreationService
          .createConversation("5d02201b5b0000360151779e", listOfConversationItems.tail, RenderType.CustomerLink))
        .thenReturn(
          Future.successful(Right(htmlConversationItems))
        )

      val result = await(messageService.getPreviousMessages("5d02201b5b0000360151779e")(mockHeaderCarrier))
      result.isRight
    }
  }

  "TwoWayMessageService.getConversation" should {
    "return a list of messages as Html" in {
      when(mockMessageConnector.getMessages(any[String])(any[HeaderCarrier])).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.OK, Some(jsonConversationItems), Map.empty, None)
        )
      )

      when(
        mockHtmlCreationService
          .createConversation("5d02201b5b0000360151779e", listOfConversationItems, RenderType.CustomerLink)).thenReturn(
        Future.successful(Right(htmlConversationItems))
      )

      val result =
        await(messageService.findMessagesBy("5d02201b5b0000360151779e")(mockHeaderCarrier))
      result.isRight

    }

  }

}
