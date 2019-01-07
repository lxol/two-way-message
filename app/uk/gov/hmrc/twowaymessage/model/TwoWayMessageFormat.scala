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

package uk.gov.hmrc.twowaymessage.model

import play.api.libs.json.{Json, Reads, Writes}

object TwoWayMessageFormat {

  import uk.gov.hmrc.twowaymessage.model.CommonFormats._

  implicit val twoWayMessage: Reads[TwoWayMessage] = Json.reads[TwoWayMessage]
  implicit val twoWayMessageWrites: Writes[TwoWayMessage] = Json.writes[TwoWayMessage]

  implicit val twoWayMessageReply: Reads[TwoWayMessageReply] = Json.reads[TwoWayMessageReply]
  implicit val twoWayMessagReplyeWrites: Writes[TwoWayMessageReply] = Json.writes[TwoWayMessageReply]
}

case class TwoWayMessage(email: String, subject: String, content: Option[String] = None, replyTo: Option[String] = None)
case class TwoWayMessageReply(content: String)

object FormId extends Enumeration {

  type FormId = Value

  val Question = Value("2WSM-question")
  val Reply = Value("2WSM-reply")
}

object MessageType extends Enumeration {

  type MessageType = Value

  val Advisor = Value("2wsm-advisor")
  val Customer = Value("2wsm-customer")
}

