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

import com.google.inject.ImplementedBy
import org.apache.commons.codec.binary.Base64
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.gform.dms.DmsMetadata
import uk.gov.hmrc.http._
import uk.gov.hmrc.twowaymessage.enquiries.Enquiry
import uk.gov.hmrc.twowaymessage.model._
import uk.gov.hmrc.twowaymessage.model.FormId.FormId
import uk.gov.hmrc.twowaymessage.model.MessageType.MessageType

import scala.concurrent.Future
import scala.language.implicitConversions

@ImplementedBy(classOf[TwoWayMessageServiceImpl])
trait TwoWayMessageService {

  type ErrorFunction = (Int, String) => Result

  val errorResponse: ErrorFunction = (status: Int, message: String) => BadGateway(Json.toJson(Error(status, message)))

  def getMessageMetadata(messageId: String)(implicit hc: HeaderCarrier): Future[Option[MessageMetadata]]

  def post(queueId: String, nino: Nino, twoWayMessage: TwoWayMessage, dmsMetaData: DmsMetadata, name: Name)(implicit hc: HeaderCarrier): Future[Result]

  def postAdviserReply(twoWayMessageReply: TwoWayMessageReply, replyTo: String)(implicit hc: HeaderCarrier): Future[Result]

  def postCustomerReply(twoWayMessageReply: TwoWayMessageReply, replyTo: String)(implicit hc: HeaderCarrier): Future[Result]

  def createDmsSubmission(html: String, response: HttpResponse, dmsMetaData: DmsMetadata)(implicit hc: HeaderCarrier): Future[Result]

  def getMessageContentBy(messageId: String)(implicit hc: HeaderCarrier): Future[Option[String]]

  def getPreviousMessages(messageId: String)(implicit hc: HeaderCarrier): Future[Either[String, Html]]

  def getLastestMessage(messageId: String)(implicit hc: HeaderCarrier): Future[Either[String, Html]]

  def createJsonForMessage(
    refId: String,
    twoWayMessage: TwoWayMessage,
    nino: Nino,
    queueId: String,
    name: Name): Message = {

    val responseTime = Enquiry(queueId).get.responseTime
    Message(
      ExternalRef(refId, "2WSM"),
      Recipient(
        TaxIdentifier(nino.name, nino.value),
        twoWayMessage.contactDetails.email,
        Option(
          TaxpayerName(forename = name.name, surname = name.lastName, line1 = deriveAddressedName(name))
        )
      ),
      MessageType.Customer,
      twoWayMessage.subject,
      twoWayMessage.content,
      Details(FormId.Question, None, None, enquiryType = Some(queueId), waitTime = Some(responseTime))
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
        TaxIdentifier(metadata.recipient.identifier.name, metadata.recipient.identifier.value),
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
        waitTime = queueId.map(qId => Enquiry(qId).get.responseTime)
      )
    )

  def encodeToBase64String(text: String): String =
    Base64.encodeBase64String(text.getBytes("UTF-8"))

  def findMessagesBy(messageId: String)(implicit hc: HeaderCarrier): Future[Either[String, List[ConversationItem]]]

  protected def getContent(response: HttpResponse): Option[String] =
    response.status match {
      case OK => Some(response.body)
      case _  => None
    }

  protected def handleResponse(response: HttpResponse): Result = response.status match {
    case CREATED => Created(Json.parse(response.body))
    case _       => errorResponse(response.status, response.body)
  }

  protected def handleError(): PartialFunction[Throwable, Result] = {
    case e: Upstream4xxResponse => errorResponse(e.upstreamResponseCode, e.message)
    case e: Upstream5xxResponse => errorResponse(e.upstreamResponseCode, e.message)
    case e: HttpException       => errorResponse(e.responseCode, e.message)
  }

  def deriveAddressedName(name: Name): Option[String] = (name.name, name.lastName) match {
    case (Some(n), Some(l)) => Some(s"$n $l")
    case (Some(n), None)    => Some(n)
    case _                  => None
  }

}
