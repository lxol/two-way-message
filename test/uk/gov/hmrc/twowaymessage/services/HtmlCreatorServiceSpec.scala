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
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.twowaymessage.assets.Fixtures
import uk.gov.hmrc.twowaymessage.model._

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{Utility, Xhtml}

class HtmlCreatorServiceSpec
    extends WordSpec with Matchers with GuiceOneAppPerSuite with Fixtures with MockitoSugar with UnitSpec {

  implicit val mockExecutionContext = mock[ExecutionContext]
  implicit val mockHeaderCarrier = mock[HeaderCarrier]

  lazy val mockhttpClient = mock[HttpClient]
  lazy val mockServiceConfig = mock[ServicesConfig]

  val mockTwoWayMessageService = mock[TwoWayMessageService]

  val injector = new GuiceApplicationBuilder()
    .overrides(bind[TwoWayMessageService].to(mockTwoWayMessageService))
    .injector()

  implicit val htmlCreatorService = injector.instanceOf[HtmlCreatorServiceImpl]

  val latestMessageId = "5d02201b5b0000360151779e"

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
        "<p>Dear TestUser</p><p>Thank you for your message of 13 June 2019.</p><p>To recap your question, " +
          "I think you're asking for help with</p><p>I believe this answers your question and hope you are satisfied with the response. " +
          "There's no need to send a reply. " +
          "But if you think there's something important missing, just ask another question about this below.</p>" +
          "<p>Regards<br>Matthew Groom<br>HMRC digital team</p>")
    ),
    ConversationItem(
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
      Some("<p>Hello, my friend!</p>")
    )
  )

  "createConversation" should {
    "create HTML for a customer" in {
      when(
        mockTwoWayMessageService
          .findMessagesBy(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(listOfConversationItems)))
      val result =
        await(htmlCreatorService.createConversation(latestMessageId, listOfConversationItems, RenderType.CustomerLink))
      result shouldBe
        Right(Html.apply(Xhtml.toXhtml(<h1 class="govuk-heading-xl margin-top-small margin-bottom-small">Matt Test 1</h1>
          <p class="faded-text--small">This message was sent to you on 13 June 2019</p> ++
          Utility.trim(
            <div>
              <p>Dear TestUser</p>
              <p>Thank you for your message of 13 June 2019.</p>
              <p>To recap your question, I think you're asking for help with</p>
              <p>I believe this answers your question and hope you are satisfied with the response. There's no need to send a reply. But if you think there's something important missing, just ask another question about this below.</p>
              <p>Regards<br/>Matthew Groom<br/>HMRC digital team</p>
            </div>) ++
          Utility.trim(
            <p>
              <span>
                <a style="text-decoration:none;" href="/two-way-message-frontend/message/customer/P800/5d02201b5b0000360151779e/reply#reply-input-label">
                  <svg style="vertical-align:text-top;padding-right:5px;" width="21px" height="20px" viewBox="0 0 33 31" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
                    <title>Reply</title>
                    <g id="Page-1" stroke="none" stroke-width="1" fill="none" fill-rule="evenodd">
                      <g id="icon-reply" fill="#000000" fill-rule="nonzero">
                        <path d="M20.0052977,9.00577935 C27.0039418,9.21272548 32.6139021,14.9512245 32.6139021,22 C32.6139021,25.5463753 31.1938581,28.7610816 28.8913669,31.1065217 C29.2442668,30.1082895 29.4380446,29.1123203 29.4380446,28.1436033 C29.4380446,21.8962314 25.9572992,21.1011463 20.323108,21 L15,21 L15,30 L-1.42108547e-14,15 L15,2.25597319e-13 L15,9 L20,9 L20.0052977,9.00577935 Z" id="Combined-Shape"></path>
                      </g>
                    </g>
                  </svg>
                </a>
              </span>
              <a href="/two-way-message-frontend/message/customer/P800/5d02201b5b0000360151779e/reply#reply-input-label">Send another message about this</a>
            </p>) ++
          <hr/>
          <h2 class="govuk-heading-xl margin-top-small margin-bottom-small">Matt Test 1</h2>
          <p class="faded-text--small">You sent this message on 13 June 2019</p>
          <div><p>Hello, my friend!</p></div>)))
    }

    "create HTML content for an advisor" in {
      when(
        mockTwoWayMessageService
          .findMessagesBy(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(listOfConversationItems)))
      val result =
        await(htmlCreatorService.createConversation(latestMessageId, listOfConversationItems, RenderType.Adviser))
      result shouldBe
        Right(Html(Xhtml.toXhtml(<p class="faded-text--small">13 June 2019 by HMRC:</p> ++
          Utility.trim(
            <div>
              <p>Dear TestUser</p>
              <p>Thank you for your message of 13 June 2019.</p>
              <p>To recap your question, I think you're asking for help with</p>
              <p>I believe this answers your question and hope you are satisfied with the response. There's no need to send a reply. But if you think there's something important missing, just ask another question about this below.</p>
              <p>Regards<br/>Matthew Groom<br/>HMRC digital team</p>
            </div>) ++
            <hr/>
          <p class="faded-text--small">13 June 2019 by the customer:</p>
          <div><p>Hello, my friend!</p></div>)))
    }
    SharedMetricRegistries.clear()
  }

  "createSingleMessageHtml" should {

    def conversationItem(subject:String):ConversationItem = ConversationItem(
      "5d021fbe5b0000200151779c",
      subject,
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

    "create one HTML message for the first message" in {
      val result = await(htmlCreatorService.createSingleMessageHtml(conversationItem("Matt Test 1")))
      result shouldBe
        Right(Html.apply(Xhtml.toXhtml(<h1 class="govuk-heading-xl margin-top-small margin-bottom-small">Matt Test 1</h1>
          <p class="faded-text--small">You sent this message on 13 June 2019</p>
          <div>Hello, my friend!</div>)))
    }

    "create one HTML message with escaped HTML subject text for the first message" in {
      val result = await(htmlCreatorService.createSingleMessageHtml(conversationItem("&lt;h1&gt;A &amp; B&lt;/h1&gt;")))
      result shouldBe
        Right(Html.apply(Xhtml.toXhtml(<h1 class="govuk-heading-xl margin-top-small margin-bottom-small">&lt;h1&gt;A &amp; B&lt;/h1&gt;</h1>
          <p class="faded-text--small">You sent this message on 13 June 2019</p>
          <div>Hello, my friend!</div>)))
    }

    "create one HTML message with non-escaped HTML subject for the first message" in {
      val result = await(htmlCreatorService.createSingleMessageHtml(conversationItem("<h1>A & B</h1>")))
      result shouldBe
        Right(Html(Xhtml.toXhtml(<h1 class="govuk-heading-xl margin-top-small margin-bottom-small">&lt;h1&gt;A &amp; B&lt;/h1&gt;</h1>
          <p class="faded-text--small">You sent this message on 13 June 2019</p>
          <div>Hello, my friend!</div>)))
    }

  }

  "createHtmlForPdf" should {

    val subjectWithEscapedChars = "&lt;b&gt;This is another test to see if this &gt; that &amp; that &lt; this&lt;/b&gt;"

    "create a complete HTML document" in {
      val subject = "Some subject"
      val result = await(htmlCreatorService.createHtmlForPdf(latestMessageId,"AB234567C",listOfConversationItems,"Some subject"))
      result shouldBe
      Right(expectedPdfHtml(subject))
    }

    "correctly render escaped HTML in the message subject" in {
      val result = await(htmlCreatorService.createHtmlForPdf(latestMessageId,"AB234567C",listOfConversationItems,subjectWithEscapedChars))
      result shouldBe
        Right(expectedPdfHtml(subjectWithEscapedChars))
    }

    "correctly escape unescaped HTML in the message subject" in {
      val subjectWithUnescapedChars = "<b>This is another test to see if this > that & that < this</b>"
      val result = await(htmlCreatorService.createHtmlForPdf(latestMessageId,"AB234567C",listOfConversationItems,subjectWithUnescapedChars))
      result shouldBe
        Right(expectedPdfHtml(subjectWithEscapedChars))
    }
  }


}
