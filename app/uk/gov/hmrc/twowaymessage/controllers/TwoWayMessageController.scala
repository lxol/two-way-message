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

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.gform.dms.DmsMetadata
import uk.gov.hmrc.gform.gformbackend.GformConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.controller.WithJsonBody
import uk.gov.hmrc.twowaymessage.enquiries.Enquiry
import uk.gov.hmrc.twowaymessage.model._
import uk.gov.hmrc.twowaymessage.model.MessageMetadataFormat._
import uk.gov.hmrc.twowaymessage.model.TwoWayMessageFormat._
import uk.gov.hmrc.twowaymessage.services.TwoWayMessageService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TwoWayMessageController @Inject()(
  twms: TwoWayMessageService,
  val authConnector: AuthConnector,
  val gformConnector: GformConnector)(implicit ec: ExecutionContext)
    extends InjectedController with WithJsonBody with AuthorisedFunctions {

  private val logger = Logger(this.getClass)

  // Customer creating a two-way message
  def createMessage(queueId: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
    authorised(Enrolment("HMRC-NI")).retrieve(Retrievals.nino) {
      case Some(ninoId) => validateAndPostMessage(queueId, Nino(ninoId), request.body)
      case _ =>
        Logger.debug("Can not retrieve user's nino, returning Forbidden - Not Authorised Error")
        Future.successful(Forbidden(Json.toJson("Not authorised")))
    } recover handleAuthorizationError
  }

  def getRecipientMetadata(messageId: String): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
    twms.getMessageMetadata(messageId).map {
      case Some(m) => Ok(Json.toJson(m))
      case None => NotFound
    }
  }

  def handleAuthorizationError(): PartialFunction[Throwable, Result] = {
    case _: NoActiveSession =>
      Logger.debug("Request did not have an Active Session, returning Unauthorised - Unauthenticated Error")
      Unauthorized(Json.toJson("Not authenticated"))

    case _ =>
      Logger.debug("Request has an active session but was not authorised, returning Forbidden - Not Authorised Error")
      Forbidden(Json.toJson("Not authorised"))
  }

  // Validates the customer's message payload and then posts the message
  def validateAndPostMessage(queueId: String, nino: Nino, requestBody: JsValue)(
    implicit hc: HeaderCarrier): Future[Result] =
    requestBody.validate[TwoWayMessage] match {
      case _: JsSuccess[_] =>
        Enquiry(queueId) match {
          case Some(enquiryId) => {
            val dmsMetaData = DmsMetadata(enquiryId.dmsFormId, nino.nino, enquiryId.classificationType, enquiryId.businessArea)
            twms.post(queueId, nino, requestBody.as[TwoWayMessage], dmsMetaData)
          }
          case None => Future.successful(BadRequest(Json.obj("error" -> 400, "message" -> s"Invalid EnquityId: $queueId")))
        }

      case e: JsError => Future.successful(BadRequest(Json.obj("error" -> 400, "message" -> JsError.toJson(e))))
    }

  // Advisor replying to a customer message
  def createAdvisorResponse(replyTo: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    {
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
      authorised(AuthProviders(PrivilegedApplication)) {
        validateAndPostAdvisorResponse(request.body, replyTo)
      }.recoverWith {
        case _ => Future.successful(Forbidden)
      }
    }
  }

  // Validates the advisor response payload and then posts the reply
  def validateAndPostAdvisorResponse(requestBody: JsValue, replyTo: String)(
    implicit hc: HeaderCarrier): Future[Result] =
    requestBody.validate[TwoWayMessageReply] match {
      case _: JsSuccess[_] => twms.postAdvisorReply(requestBody.as[TwoWayMessageReply], replyTo)
      case e: JsError      => Future.successful(BadRequest(Json.obj("error" -> 400, "message" -> JsError.toJson(e))))
    }

  // Customer replying to an advisor's message
  def createCustomerResponse(queueId: String, replyTo: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
      authorised(Enrolment("HMRC-NI")) {
        validateAndPostCustomerResponse(request.body, replyTo)
      } recover handleAuthorizationError
  }

  // Validates the customer's response payload and then posts the reply
  def validateAndPostCustomerResponse(requestBody: JsValue, replyTo: String)(
    implicit hc: HeaderCarrier): Future[Result] =
    requestBody.validate[TwoWayMessageReply] match {
      case _: JsSuccess[_] => twms.postCustomerReply(requestBody.as[TwoWayMessageReply], replyTo)
      case e: JsError      => Future.successful(BadRequest(Json.obj("error" -> 400, "message" -> JsError.toJson(e))))
    }
}
