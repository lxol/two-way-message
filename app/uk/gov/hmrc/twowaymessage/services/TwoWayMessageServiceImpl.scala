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

import java.util.UUID.randomUUID

import com.google.inject.Inject
import play.api.http.Status.{ CREATED, INTERNAL_SERVER_ERROR, OK }
import play.api.libs.json.{ JsError, Json }
import play.api.mvc.Result
import play.api.mvc.Results.Created
import play.twirl.api.Html
import scala.concurrent.{ ExecutionContext, Future }
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.domain.TaxIds.TaxIdWithName
import uk.gov.hmrc.domain._
import uk.gov.hmrc.gform.dms.{ DmsHtmlSubmission, DmsMetadata }
import uk.gov.hmrc.gform.gformbackend.GformConnector
import uk.gov.hmrc.gform.sharedmodel.form.EnvelopeId
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.twowaymessage.connectors.MessageConnector
import uk.gov.hmrc.twowaymessage.enquiries.{ Enquiry, EnquiryType }
import uk.gov.hmrc.twowaymessage.model.FormId.FormId
import uk.gov.hmrc.twowaymessage.model.MessageFormat._
import uk.gov.hmrc.twowaymessage.model.MessageMetadataFormat._
import uk.gov.hmrc.twowaymessage.model.MessageType.MessageType
import uk.gov.hmrc.twowaymessage.model._

class TwoWayMessageServiceImpl @Inject()(
  messageConnector: MessageConnector,
  gformConnector: GformConnector,
  servicesConfig: ServicesConfig,
  enquiries: Enquiry,
  htmlCreatorService: HtmlCreatorService)(implicit ec: ExecutionContext)
    extends TwoWayMessageService {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def getMessageMetadata(messageId: String)(implicit hc: HeaderCarrier): Future[Option[MessageMetadata]] =
    messageConnector
      .getMessageMetadata(messageId)
      .flatMap(response =>
        response.status match {
          case OK =>
            val metadata = Json.parse(response.body).validate[MessageMetadata]
            Future.successful(metadata.asOpt)
          case _ => Future.successful(None)
      })

  override def post(
    enquiryType: EnquiryType,
    taxIdentifier: TaxIdWithName,
    twoWayMessage: TwoWayMessage,
    dmsMetaData: DmsMetadata,
    name: Name)(implicit hc: HeaderCarrier): Future[Result] = {
    val body = createJsonForMessage(randomUUID.toString, twoWayMessage, taxIdentifier, enquiryType.name, name)
    messageConnector.postMessage(body) flatMap { response =>
      handleResponse(twoWayMessage.subject, response, dmsMetaData, enquiryType, Some(twoWayMessage.contactDetails))
    } recover handleError
  }

  override def postAdviserReply(twoWayMessageReply: TwoWayMessageReply, replyTo: String)(
    implicit hc: HeaderCarrier): Future[Result] =
    postAdviserReply(twoWayMessageReply, replyTo, MessageType.Adviser, FormId.Reply)

  override def postCustomerReply(twoWayMessageReply: TwoWayMessageReply, replyTo: String)(
    implicit hc: HeaderCarrier): Future[Result] =
    (for {
      metadata <- getMessageMetadata(replyTo)
      enquiryType <- metadata.get.details.enquiryType
                      .fold[Future[String]](Future.failed(new Exception(s"Unable to get DMS queue id for $replyTo")))(
                        Future.successful)
      enquiryId <- enquiries(enquiryType)
                    .fold[Future[EnquiryType]](Future.failed(new Exception(s"Unknown $enquiryType")))(Future.successful)
      dmsMetaData = DmsMetadata(
        enquiryId.dmsFormId,
        metadata.get.recipient.identifier.value,
        enquiryId.classificationType,
        enquiryId.businessArea)
      body = createJsonForReply(
        Some(enquiryType),
        randomUUID.toString,
        MessageType.Customer,
        FormId.Question,
        metadata.get,
        twoWayMessageReply,
        replyTo)
      postMessageResponse <- messageConnector.postMessage(body)
      dmsHandleResponse   <- handleResponse(metadata.get.subject, postMessageResponse, dmsMetaData, enquiryId, None)
    } yield dmsHandleResponse) recover handleError

  override def createDmsSubmission(html: String, response: HttpResponse, dmsMetaData: DmsMetadata, messageId: String)(
    implicit hc: HeaderCarrier): Future[Result] = {
    val dmsSubmission = DmsHtmlSubmission(encodeToBase64String(html), dmsMetaData)
    Future(Created(Json.parse(response.body))).andThen {
      case _ => submitToDms(messageId, dmsSubmission)
    }
  }

  override def findMessagesBy(messageId: String)(
    implicit hc: HeaderCarrier): Future[Either[String, List[ConversationItem]]] =
    messageConnector.getMessages(messageId).flatMap { response =>
      response.status match {
        case OK =>
          response.json
            .validate[List[ConversationItem]]
            .fold(
              errors => Future.successful(Left(Json stringify JsError.toJson(errors))),
              msgList => Future.successful(Right(msgList))
            )
        case _ => Future.successful(Left("Error retrieving messages"))
      }
    }

  override def getMessageContentBy(messageId: String)(implicit hc: HeaderCarrier): Future[Option[String]] =
    getMessageContent(messageId).flatMap {
      case Some(content) => Future.successful(Some(content))
      case None          => Future.successful(None)
    }

  private def submitToDms(messageId: String, dmsSubmission: DmsHtmlSubmission) =
    gformConnector.submitToDmsViaGform(dmsSubmission).flatMap { response =>
      response.status match {
        case OK =>
          response.json
            .validate[EnvelopeId]
            .fold(
              _ => Future.successful(Left("Error with submitToDmsViaGform")),
              envelopId => messageConnector.postDmsStatus(messageId, envelopId.value)
            )
      }
    }

  private def postAdviserReply(
    twoWayMessageReply: TwoWayMessageReply,
    replyTo: String,
    messageType: MessageType,
    formId: FormId)(implicit hc: HeaderCarrier): Future[Result] =
    (for {
      metadata <- getMessageMetadata(replyTo)
      body = createJsonForReply(
        None,
        randomUUID.toString,
        messageType,
        formId,
        metadata.get,
        twoWayMessageReply,
        replyTo)
      resp <- messageConnector.postMessage(body)
    } yield resp) map {
      handleResponse
    } recover handleError

  private def handleResponse(
    subject: String,
    response: HttpResponse,
    dmsMetaData: DmsMetadata,
    enquiryType: EnquiryType,
    contactDetails: Option[ContactDetails])(implicit hc: HeaderCarrier): Future[Result] =
    response.status match {
      case CREATED =>
        response.json.validate[Identifier].asOpt match {
          case Some(identifier) =>
            findMessagesBy(identifier.id).flatMap {
              case Left(error) => Future.successful(errorResponse(INTERNAL_SERVER_ERROR, error))
              case Right(list) =>
                htmlCreatorService
                  .createHtmlForPdf(identifier.id, dmsMetaData.customerId, list, subject, enquiryType, contactDetails)
                  .flatMap {
                    case Left(error) => Future.successful(errorResponse(INTERNAL_SERVER_ERROR, error))
                    case Right(html) => createDmsSubmission(html, response, dmsMetaData, identifier.id)
                  }
            }
          case None => Future.successful(errorResponse(INTERNAL_SERVER_ERROR, "Failed to create enquiry reference"))
        }
      case _ => Future.successful(errorResponse(response.status, response.body))
    }

  private def getMessageContent(messageId: String)(implicit hc: HeaderCarrier): Future[Option[String]] =
    messageConnector
      .getMessageContent(messageId)
      .flatMap(response =>
        getContent(response) match {
          case Some(content) => Future successful Some(content)
          case None          => Future.successful(None)
      })

  override def getLastestMessage(messageId: String)(implicit hc: HeaderCarrier): Future[Either[String, Html]] =
    findMessagesBy(messageId).flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(list) =>
        list
          .sortWith(_.id > _.id)
          .headOption
          .map(msg => htmlCreatorService.createSingleMessageHtml(msg))
          .getOrElse(Future.successful(Right(Html(""))))
    }

  override def getPreviousMessages(messageId: String)(implicit hc: HeaderCarrier): Future[Either[String, Html]] =
    findMessagesBy(messageId).flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(list) =>
        htmlCreatorService.createConversation(messageId, list.sortWith(_.id > _.id).drop(1), RenderType.CustomerForm)
    }

  def createJsonForMessage(
    refId: String,
    twoWayMessage: TwoWayMessage,
    taxIdentifier: TaxIdWithName,
    enquiryType: String,
    name: Name): Message = {

    val responseTime = enquiries(enquiryType).get.responseTime
    Message(
      ExternalRef(refId, "2WSM"),
      Recipient(
        taxIdentifier,
        twoWayMessage.contactDetails.email,
        Option(
          TaxpayerName(forename = name.name, surname = name.lastName, line1 = deriveAddressedName(name))
        )
      ),
      MessageType.Customer,
      twoWayMessage.subject,
      twoWayMessage.content,
      Details(FormId.Question, None, None, enquiryType = Some(enquiryType), waitTime = Some(responseTime))
    )

  }
  def createJsonForReply(
    queueId: Option[String],
    refId: String,
    messageType: MessageType,
    formId: FormId,
    metadata: MessageMetadata,
    reply: TwoWayMessageReply,
    replyTo: String): Message =
    Message(
      ExternalRef(refId, "2WSM"),
      Recipient(
        metadata.recipient.identifier,
        metadata.recipient.email.getOrElse(""),
        metadata.taxpayerName
      ),
      messageType,
      metadata.subject,
      reply.content,
      Details(
        formId,
        Some(replyTo),
        metadata.details.threadId,
        metadata.details.enquiryType,
        metadata.details.adviser,
        waitTime = queueId.map(qId => enquiries(qId).get.responseTime),
        topic = reply.topic
      )
    )

}
