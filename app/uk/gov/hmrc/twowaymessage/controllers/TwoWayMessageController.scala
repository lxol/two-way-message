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

package uk.gov.hmrc.twowaymessage.controllers

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.controller.WithJsonBody
import uk.gov.hmrc.twowaymessage.model.TwoWayMessageFormat._
import uk.gov.hmrc.twowaymessage.model.{TwoWayMessage, TwoWayMessageReply}
import uk.gov.hmrc.twowaymessage.services.TwoWayMessageService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TwoWayMessageController @Inject()(twms: TwoWayMessageService)
                                       (implicit ec: ExecutionContext) extends InjectedController with WithJsonBody {

  private val logger = Logger(this.getClass)

  // Customer creating a two-way message
  def createMessage(queueId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request => validateAndPostMessage(request.body)
  }

  // Validates the customer's response payload and then posts the message
  def validateAndPostMessage(requestBody: JsValue): Future[Result] =
    requestBody.validate[TwoWayMessage] match {
      case _: JsSuccess[_] => twms.post(requestBody.as[TwoWayMessage])
      case e: JsError => Future.successful(BadRequest(Json.obj("error" -> "OK", "message" -> JsError.toJson(e))))
  }

  // Advisor replying to a customer message
  def createAdvisorResponse(replyTo: String): Action[JsValue] = Action.async(parse.json) {
    implicit request => validateAndPostAdvisorResponse(request.body, replyTo)
  }

  // Validates the advisor response payload and then posts the reply
  def validateAndPostAdvisorResponse(requestBody: JsValue, replyTo: String): Future[Result] =
    requestBody.validate[TwoWayMessageReply] match {
      case _: JsSuccess[_] => twms.postReply(requestBody.as[TwoWayMessageReply], replyTo)
      case e: JsError => Future.successful(BadRequest(Json.obj("error" -> "OK", "message" -> JsError.toJson(e))))
  }
}
