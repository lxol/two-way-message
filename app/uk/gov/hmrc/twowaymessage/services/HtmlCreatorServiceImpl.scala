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

package uk.gov.hmrc.twowaymessage.services

import javax.inject.Inject
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.twowaymessage.model.{ConversationItem, ItemMetadata, MessageType}
import uk.gov.hmrc.twowaymessage.utils.HtmlUtil._
import uk.gov.hmrc.twowaymessage.utils.XmlConversion

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.xml._

class HtmlCreatorServiceImpl @Inject()(servicesConfig: ServicesConfig)(implicit ec: ExecutionContext) extends HtmlCreatorService {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def createConversation(
    latestMessageId: String,
    messages: List[ConversationItem],
    replyType: RenderType.ReplyType): Future[Either[String, Html]] = {

    val conversation = createConversationList(messages.sortWith(_.id > _.id), replyType)
    val fullConversation = conversation.mkString(Xhtml.toXhtml(<hr/>))

    Future.successful(Right(Html.apply(fullConversation)))
  }

  override def createSingleMessageHtml(conversationItem: ConversationItem): Future[Either[String, Html]] =
    Future.successful(Right(Html.apply(format2wsMessageForCustomer(conversationItem, ItemMetadata(isLatestMessage = true, hasLink = false)))))

  override def createHtmlForPdf(latestMessageId: String,
                                customerId: String, messages:
                                List[ConversationItem],
                                subject: String): Future[Either[String,String]] = {
    val frontendUrl: String = servicesConfig.getString("pdf-admin-prefix")
    val url = s"$frontendUrl/message/$latestMessageId/reply"
    createConversation(latestMessageId, messages, RenderType.Adviser) map {
      case Left(error) => Left(error)
      case Right(html) =>
        XmlConversion.stringToXmlNodes(uk.gov.hmrc.twowaymessage.views.html.two_way_message(url, customerId, Html(escapeForXhtml(subject)), html).body) match {
          case Success(xml) => Right("<!DOCTYPE html>" + Xhtml.toXhtml(Utility.trim(xml.head)))
          case Failure(e) => Left("Unable to generate HTML for PDF due to: " + e.getMessage)
        }
    }
  }

  private def createConversationList(messages: List[ConversationItem], replyType: RenderType.ReplyType): List[String] =
    replyType match {
      case RenderType.CustomerLink =>
        messages
          .sortWith(_.id > _.id)
          .headOption
          .map { hm =>
            format2wsMessageForCustomer(hm, ItemMetadata(isLatestMessage = true)) :: messages.tail.map(m =>
              format2wsMessageForCustomer(m, ItemMetadata(isLatestMessage = false)))
          }
          .getOrElse(List.empty)
      case RenderType.CustomerForm =>
        messages
          .sortWith(_.id > _.id)
          .headOption
          .map { hm =>
            format2wsMessageForCustomer(hm, ItemMetadata(isLatestMessage = true, hasSmallSubject = true)) :: messages.tail.map(m =>
              format2wsMessageForCustomer(m, ItemMetadata(isLatestMessage = false)))
          }
          .getOrElse(List.empty)
      case RenderType.Adviser => messages.sortWith(_.id > _.id).map(msg => format2wsMessageForAdviser(msg))
    }

  private def format2wsMessageForCustomer(item: ConversationItem, metadata: ItemMetadata): String = {
    Xhtml.toXhtml(getHeader(metadata,item.subject) ++ <p class="faded-text--small">{getCustomerDateText(item)}</p>
      ++ getContentDiv(item.content) ++ getReplyLink(metadata, item).getOrElse(NodeSeq.Empty))
  }

  private def format2wsMessageForAdviser(item: ConversationItem): String = {
    Xhtml.toXhtml(
      <p class="faded-text--small">{getAdviserDatesText(item)}</p> ++ getContentDiv(item.content))
  }

  private def getHeader(metadata: ItemMetadata, subject: String): Elem = {
    val headingClass = "govuk-heading-xl margin-top-small margin-bottom-small"
    if (metadata.isLatestMessage && !metadata.hasSmallSubject) {
      <h1 class={headingClass}>{Unparsed(escapeForXhtml(subject))}</h1>
    } else {
      <h2 class={headingClass}>{Unparsed(escapeForXhtml(subject))}</h2>
    }
  }

  private def getContentDiv(maybeContent: Option[String]): Node = {
    maybeContent match {
      case Some(content) =>
        XmlConversion.stringToXmlNodes(content.replaceAllLiterally("<br>", "<br/>")) match {
          case Success(nodes) => Utility.trim(<div>{nodes}</div>)
          case Failure(_) => <div>There was a problem reading the message content</div>
        }
      case None => <div/>
    }
  }

  private def getReplyLink(metadata: ItemMetadata, conversationItem: ConversationItem):Option[Elem] = {
    if (metadata.isLatestMessage && metadata.hasLink) {
      val enquiryType = conversationItem.body
        .flatMap {
          _.enquiryType
        }
        .getOrElse("")
      val formActionUrl = s"/two-way-message-frontend/message/customer/$enquiryType/" + conversationItem.id + "/reply#reply-input-label"
      conversationItem.body.flatMap(_.`type` match {
        case MessageType.Adviser =>
          val link = <a href={formActionUrl}>Send another message about this</a>
          Some(<p>{getReplyIcon(formActionUrl) ++ link}</p>)
        case _ => None
      })
    } else {
      None
    }
  }

  private def getReplyIcon(formActionUrl: String): Node = {
    Utility.trim(<span>
      <a style="text-decoration:none;" href={formActionUrl}>
        <svg style="vertical-align:text-top;padding-right:5px;" width="21px" height="20px" viewBox="0 0 33 31" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
          <title>Reply</title>
          <g id="Page-1" stroke="none" stroke-width="1" fill="none" fill-rule="evenodd">
            <g id="icon-reply" fill="#000000" fill-rule="nonzero">
              <path d="M20.0052977,9.00577935 C27.0039418,9.21272548 32.6139021,14.9512245 32.6139021,22 C32.6139021,25.5463753 31.1938581,28.7610816 28.8913669,31.1065217 C29.2442668,30.1082895 29.4380446,29.1123203 29.4380446,28.1436033 C29.4380446,21.8962314 25.9572992,21.1011463 20.323108,21 L15,21 L15,30 L-1.42108547e-14,15 L15,2.25597319e-13 L15,9 L20,9 L20.0052977,9.00577935 Z" id="Combined-Shape"></path>
            </g>
          </g>
        </svg>
      </a>
    </span>)
  }

  private def getCustomerDateText(message: ConversationItem): String = {
    val messageDate = extractMessageDate(message)
    message.body match {
      case Some(conversationItemDetails) =>
        conversationItemDetails.`type` match {
          case MessageType.Customer => s"You sent this message on $messageDate"
          case MessageType.Adviser  => s"This message was sent to you on $messageDate"
          case _                    => defaultDateText(messageDate)
        }
      case _ => defaultDateText(messageDate)
    }
  }

  def getAdviserDatesText(message: ConversationItem): String = {
    val messageDate = extractMessageDate(message)
    message.body match {
      case Some(conversationItemDetails) =>
        conversationItemDetails.`type` match {
          case MessageType.Adviser  => s"$messageDate by HMRC:"
          case MessageType.Customer => s"$messageDate by the customer:"
          case _                    => defaultDateText(messageDate)
        }
      case _ => defaultDateText(messageDate)
    }
  }

  private def defaultDateText(dateStr: String) = s"This message was sent on $dateStr"

  private def extractMessageDate(message: ConversationItem): String =
    message.body.flatMap(_.issueDate) match {
      case Some(issueDate) => formatter(issueDate)
      case None            => formatter(message.validFrom)
    }

  private val dateFormatter = DateTimeFormat.forPattern("dd MMMM yyyy")

  private def formatter(date: LocalDate): String = date.toString(dateFormatter)

}
