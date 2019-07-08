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

import java.util.UUID.randomUUID

import com.google.inject.Inject
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsError, JsPath, Json, JsonValidationError}
import play.api.mvc.Result
import play.api.mvc.Results.Created
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.gform.dms.{DmsHtmlSubmission, DmsMetadata}
import uk.gov.hmrc.gform.gformbackend.GformConnector
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.twowaymessage.connectors.MessageConnector
import uk.gov.hmrc.twowaymessage.enquiries.Enquiry
import uk.gov.hmrc.twowaymessage.enquiries.Enquiry.EnquiryTemplate
import uk.gov.hmrc.twowaymessage.model._
import uk.gov.hmrc.twowaymessage.model.FormId.FormId
import uk.gov.hmrc.twowaymessage.model.MessageMetadataFormat._
import uk.gov.hmrc.twowaymessage.model.MessageType.MessageType
import uk.gov.hmrc.twowaymessage.model.Message
import uk.gov.hmrc.twowaymessage.model.MessageFormat._

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Node

class TwoWayMessageServiceImpl @Inject()
(messageConnector: MessageConnector, gformConnector: GformConnector,
servicesConfig: ServicesConfig, htmlCreatorService: HtmlCreatorService)
(implicit ec: ExecutionContext) extends TwoWayMessageService {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit def stringToXml(string: String): Seq[Node] = {
    val xml = scala.xml.XML.loadString("<root>" + string + "</root>")
    xml.child
  }

  override def getMessageMetadata(messageId: String)
                                 (implicit hc: HeaderCarrier): Future[Option[MessageMetadata]] = {
    messageConnector.getMessageMetadata(messageId).flatMap(  response =>
      response.status match {
        case OK =>
          val metadata = Json.parse(response.body).validate[MessageMetadata]
          Future.successful(metadata.asOpt)
        case _ => Future.successful(None)
      })
  }

  override def post(queueId: String, nino: Nino, twoWayMessage: TwoWayMessage, dmsMetaData: DmsMetadata, name: Name)
                   (implicit hc: HeaderCarrier): Future[Result] = {
    val body = createJsonForMessage(randomUUID.toString, twoWayMessage, nino, queueId, name)
    messageConnector.postMessage(body) flatMap { response =>
      handleResponse(twoWayMessage.content, twoWayMessage.subject, response, dmsMetaData)
    } recover handleError
  }

  override def postAdviserReply(twoWayMessageReply: TwoWayMessageReply, replyTo: String)(
    implicit hc: HeaderCarrier): Future[Result] =
    postReply(twoWayMessageReply, replyTo, MessageType.Adviser, FormId.Reply)

  override def postCustomerReply(twoWayMessageReply: TwoWayMessageReply, replyTo: String)
                                (implicit hc: HeaderCarrier): Future[Result] =
    (for {
      metadata <- getMessageMetadata(replyTo)
      queueId <- metadata.get.details.enquiryType
        .fold[Future[String]](Future.failed(new Exception(s"Unable to get DMS queue id for $replyTo")))(Future.successful)
      enquiryId <- Enquiry(queueId)
        .fold[Future[EnquiryTemplate]](Future.failed(new Exception(s"Unknown $queueId")))(Future.successful)
      dmsMetaData = DmsMetadata(enquiryId.dmsFormId, metadata.get.recipient.identifier.value,
        enquiryId.classificationType, enquiryId.businessArea)
      body = createJsonForReply(randomUUID.toString, MessageType.Customer, FormId.Question, metadata.get,
        twoWayMessageReply, replyTo)
      postMessageResponse <- messageConnector.postMessage(body)
      dmsHandleResponse <- handleResponse(twoWayMessageReply.content, metadata.get.subject,
        postMessageResponse, dmsMetaData)
    } yield dmsHandleResponse) recover handleError

  override def createDmsSubmission(html: String, response: HttpResponse, dmsMetaData: DmsMetadata)
                                  (implicit hc: HeaderCarrier): Future[Result] = {
    val dmsSubmission = DmsHtmlSubmission(encodeToBase64String(html), dmsMetaData)
    Future.successful(Created(Json.parse(response.body))).andThen {
      case _ => gformConnector.submitToDmsViaGform(dmsSubmission)
    }
  }
  //TODO: In future fix the either to be evaluate the correct left and right.
  override def findMessagesBy(messageId: String)
                             (implicit hc: HeaderCarrier): Future[Either[List[ConversationItem], String]] = {
    messageConnector.getMessages(messageId).flatMap {
      response =>
        response.json.validate[List[ConversationItem]].fold(
          errors => Future.successful(Right(Json stringify JsError.toJson(errors))),
          msgList => Future.successful(Left(msgList))
        )
    }
 }

  override def getMessageContentBy(messageId: String)
                                  (implicit hc: HeaderCarrier): Future[Option[String]] =
    getMessageContent(messageId).flatMap {
      case Some(content) => Future.successful(Some(content))
      case None => Future.successful(None)
    }

  private def postReply(twoWayMessageReply: TwoWayMessageReply, replyTo: String,
                        messageType: MessageType, formId: FormId)(
    implicit hc: HeaderCarrier): Future[Result] =
    (for {
      metadata <- getMessageMetadata(replyTo)
      body = createJsonForReply(randomUUID.toString, messageType, formId, metadata.get, twoWayMessageReply, replyTo)
      resp <- messageConnector.postMessage(body)
    } yield resp) map {
      handleResponse
    } recover handleError

  private def handleResponse(content: String, subject: String, response: HttpResponse, dmsMetaData: DmsMetadata)
                            (implicit hc: HeaderCarrier): Future[Result] =
    response.status match {
      case CREATED =>
        response.json.validate[Identifier].asOpt match {
          case Some(identifier) =>
            getConversation(identifier.id, RenderType.Adviser).flatMap {
              case Left(error) => Future.successful(errorResponse(INTERNAL_SERVER_ERROR, error.toString))
              case Right(html) => createDmsSubmission(html.toString,response,dmsMetaData)
            }
          case None => Future.successful(errorResponse(INTERNAL_SERVER_ERROR, "Failed to create enquiry reference"))
        }
      case _ => Future.successful(errorResponse(response.status, response.body))
    }

  private def getMessageContent(messageId: String)
                               (implicit hc: HeaderCarrier): Future[Option[String]] = {
    messageConnector.getMessageContent(messageId).flatMap(response =>
      getContent(response) match {
        case Some(content) => Future successful Some(content)
        case None => Future.successful(None)
      })
  }
//TODO: In future fix the either to be evaluate the correct left and right.
  override def getConversation(messageId: String, replyType: RenderType.ReplyType)
                              (implicit hc: HeaderCarrier): Future[Either[String,Html]] = {
    findMessagesBy(messageId).flatMap {
      case Right(error) => Future.successful(Left(error))
      case Left(list)   => htmlCreatorService.createConversation(messageId,list,replyType)
    }
  }

  override def getLastestMessage(messageId: String)(implicit hc: HeaderCarrier): Future[Either[String,Html]] = {
    findMessagesBy(messageId).flatMap {
      case Right(error)   => Future.successful(Left(error))
      case Left(list)     => list.sortWith((_.id > _.id)).headOption.map(
        msg => htmlCreatorService.createSingleMessageHtml(msg))
        .getOrElse(Future.successful(Right(Html(""))))
    }
  }

  override def getPreviousMessages(messageId: String)(implicit hc: HeaderCarrier): Future[Either[String,Html]] = {
    findMessagesBy(messageId).flatMap {
      case Right(error)   => Future.successful(Left(error))
      case Left(list)     => htmlCreatorService.createConversation(messageId,list.sortWith(_.id > _.id).drop(1),RenderType.CustomerForm)
    }
  }

}
