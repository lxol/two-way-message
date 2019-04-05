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

import org.scalatest._
import play.api.libs.json.{Json, _}
import uk.gov.hmrc.twowaymessage.model.MessageFormat._
import uk.gov.hmrc.twowaymessage.model.{Message, _}
import uk.gov.hmrc.twowaymessage.assets.Fixtures

class MessageFormatSpec extends WordSpec with Fixtures with Matchers {

  "Message json writer" should {

    "creates json from externalRef" in {
      val externalRefJson = Json.toJson(testMessage.externalRef)
        (externalRefJson \ "source").get should be  (JsString("2WSM"))
    }

    "creates json from recipient" in {
      val recipientJson = Json.toJson(testMessage.recipient)
        (recipientJson \ "taxIdentifier" \ "name").get should be (JsString("nino"))
    }

    "creates json from messageType" in {
      val messageTypeJson = Json.toJson(testMessage.messageType)
      (messageTypeJson) should be (JsString("2wsm-advisor"))
    }

    "creates json from formId" in {
      val formIdJson = Json.toJson(testMessage.details.formId)
      (formIdJson) should be(JsString("2WSM-reply"))
    }

    "creates json from Adviser" in {
      val adviserJson = Json.toJson(testMessage.details.adviser)
        (adviserJson \ "pidId").get should be(JsString("adviser-id"))
    }

    "creates json from Details" in {
      val detailsJson = Json.toJson(testMessage.details)
        (detailsJson \ "enquiryType").get should be(JsString("P800"))
    }

    "creates json from Message" in {
      val messageJson = Json.toJson(testMessage)
        (messageJson \ "messageType").get should be(JsString("2wsm-advisor"))
    }

    "creates json from Messages" in {
      val messagesJson = Json.toJson(List(testMessage, testMessage))
        ((messagesJson).as[JsArray].value(1) \ "messageType").get should be(JsString("2wsm-advisor"))
    }
  }

  "Message json reader" should {
    "read conversation item as defined in message microservice " in {
      val id = "123456"
      val json = Json.parse(conversationItem(s"${id}"))
      val messageResult = json.validate[ConversationItem]
      messageResult should (matchPattern { case _:JsSuccess[ConversationItem] =>})
      messageResult.get.validFrom.toString should be("2013-12-01")
      messageResult.get.body.get.`type` should be(MessageType.Advisor)
    }

   "read conversation items as defined in message microservice " in {
      val id1 = "123456"
      val id2 = "654321"
      val json = Json.parse(conversationItems(id1, id2))
      val messageResult = json.validate[List[ConversationItem]]
      messageResult should (matchPattern { case _:JsSuccess[List[ConversationItem]] =>})
    }
  }
}
