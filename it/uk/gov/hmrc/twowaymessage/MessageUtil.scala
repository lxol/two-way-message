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

package uk.gov.hmrc.twowaymessage

import com.google.common.io.BaseEncoding
import play.api.libs.json.{ JsObject, JsString }

object MessageUtil {
  import play.api.libs.json.{ Json, Reads }

  import scala.util.Random

  implicit val deserialiser: Reads[MessageId] = Json.reads[MessageId]
  def generateContent: String = {
    val stringLength = 20
    BaseEncoding.base64().encode(s"Hello world! - ${Random.nextString(stringLength)}".getBytes())
  }

  def generateSimpleContent: String =
    "Hello world!"

  case class MessageId(id: String)

  def buildValidCustomerMessage: JsObject =
    JsObject(
      Seq(
        "contactDetails" ->
          JsObject(Seq("email" -> JsString("someEmail@test.com"))),
        "subject" -> JsString("subject"),
        "content" -> JsString(generateContent),
        "replyTo" -> JsString("replyTo")
      ))

  def buildInvalidCustomerMessage: JsObject =
    JsObject(
      Seq(
        "email"   -> JsString("test@test.com"),
        "content" -> JsString(generateContent),
        "replyTo" -> JsString("replyTo")
      ))

  def buildValidReplyMessage: JsObject =
    JsObject(
      Seq(
        "content" -> JsString(generateContent)
      ))

  def buildValidReplyMessageWithTopic: JsObject =
    JsObject(
      Seq(
        "content" -> JsString(generateContent),
        "topic"   -> JsString("some-topic")
      ))

  def buildInvalidReplyMessage(): JsObject =
    JsObject(
      Seq(
        "c" -> JsString(generateContent)
      ))

}
