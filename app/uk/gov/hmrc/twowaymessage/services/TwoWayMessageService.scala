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

import java.util.UUID
import java.util.UUID.randomUUID
import java.util.concurrent.TimeUnit

import com.google.inject.Inject
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.{Result, Results}
import play.api.mvc.Results._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.twowaymessage.connectors.MessageConnector
import uk.gov.hmrc.twowaymessage.model.CommonFormats._
import uk.gov.hmrc.twowaymessage.model.Error
import uk.gov.hmrc.twowaymessage.model._

import scala.concurrent.{ExecutionContext, Future}

class TwoWayMessageService @Inject()(messageConnector: MessageConnector)(implicit ec: ExecutionContext) {

  implicit val hc = HeaderCarrier()

  def post(twoWayMessage: TwoWayMessage): Future[Result] = {
    val body = createJsonForMessage(randomUUID.toString, twoWayMessage)
    messageConnector.postMessage(body) map handleResponse
  }

  def postReply(twoWayMessageReply: TwoWayMessageReply, replyTo: String): Future[Result] = {
    for {
      metadata <- messageConnector.getMessageMetadata(replyTo)
      body = createJsonForReply(randomUUID.toString, metadata, twoWayMessageReply, replyTo)
      response <- messageConnector.postMessage(body)
    } yield handleResponse(response)
  }

  def handleResponse(response: HttpResponse): Result = response.status match {
    case CREATED => Created(Json.parse(response.body))
    case _ => BadGateway(Json.toJson(Error(response.status, response.body)))
  }

  def createJsonForMessage(id: String, twoWayMessage: TwoWayMessage): Message =
    Message(
      ExternalRef(id, "2WSM"),
      twoWayMessage.recipient,
      "2wsm-customer",
      twoWayMessage.subject,
      twoWayMessage.content.getOrElse(""),
      Details("2WSM-question", None)
    )

  def createJsonForReply(id: String, metadata: MessageMetadata, reply: TwoWayMessageReply, replyTo: String): Message = {
    Message(
      ExternalRef(id, "2WSM"),
      Recipient(
        TaxIdentifier(metadata.recipient.identifier.name, metadata.recipient.identifier.value),
        metadata.recipient.email.getOrElse("")
      ),
      "2wsm-advisor",
      s"RE: ${metadata.subject}",
      reply.content,
      Details("2WSM-reply", Some(replyTo))
    )
  }
}
