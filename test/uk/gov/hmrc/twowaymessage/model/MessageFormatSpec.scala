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

package uk.gov.hmrc.twowaymessage.connectors

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
    "read v3 message as defined in message microservice " in {
      val extrefid = "123456"
      val json = Json.parse(v3Message(s"${extrefid}"))
      val messageResult = json.validate[Message]
      messageResult should (matchPattern { case _:JsSuccess[Message] =>})
      messageResult.get.externalRef.id should be(extrefid)
    }

    "read v3 messages as defined in message microservice " in {
      val extrefid1 = "123456"
      val extrefid2 = "654321"
      val json = Json.parse(v3Messages(extrefid1, extrefid2))
      val messageResult = json.validate[List[Message]]
      messageResult should (matchPattern { case _:JsSuccess[List[Message]] =>})
    }
  }
}
