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

package uk.gov.hmrc.twowaymessage.model

import org.joda.time.LocalDate
import org.scalatest._
import play.api.libs.json.{ Json, _ }
import uk.gov.hmrc.twowaymessage.assets.Fixtures
import uk.gov.hmrc.twowaymessage.model.MessageFormat._

class MessageFormatSpec extends WordSpec with Fixtures with Matchers {

  "Message json writer" should {

    "creates json from externalRef" in {
      val externalRefJson = Json.toJson(testMessage.externalRef)
      (externalRefJson \ "source").get should be(JsString("2WSM"))
    }

    "creates json from recipient" in {
      val recipientJson = Json.toJson(testMessage.recipient)
      (recipientJson \ "taxIdentifier" \ "name").get should be(JsString("nino"))
    }

    "creates json from messageType" in {
      val messageTypeJson = Json.toJson(testMessage.messageType)
      (messageTypeJson) should be(JsString("2wsm-advisor"))
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
      val json = jsonConversationItem
      val messageResult = json.validate[ConversationItem]
      messageResult should matchPattern { case _: JsSuccess[ConversationItem] => }
      messageResult.get.validFrom.toString should be("2019-06-13")
      messageResult.get.body.get.`type` should be(MessageType.Adviser)
    }

    "read conversation items as defined in message microservice " in {
      val id1 = "5d02201b5b0000360151779e"
      val id2 = "5d021fbe5b0000200151779c"
      val json = Json.parse(conversationItems(id1, id2))
      val messageResult = json.validate[List[ConversationItem]]
      messageResult should (matchPattern { case _: JsSuccess[List[ConversationItem]] => })
    }
  }

  val jsonConversationItem = Json.parse(
    """
  {
    "recipient": {
      "regime": "paye",
      "identifier": {
      "name": "nino",
      "value": "AB124567C"
    },
      "email": "matthew.groom@ntlworld.com"
    },
    "subject": "Matt Test 1",
    "body": {
      "form": "2WSM-reply",
      "type": "2wsm-advisor",
      "paperSent": false,
      "issueDate": "2019-06-13",
      "replyTo": "5d021fbe5b0000200151779c",
      "threadId": "5d021fbe5b0000200151779d",
      "enquiryType": "p800",
      "adviser": {
      "pidId": "123"
    }
    },
    "validFrom": "2019-06-13",
    "alertFrom": "2019-06-13",
    "alertDetails": {
      "templateId": "newMessageAlert_2WSM-reply",
      "recipientName": {
      "forename": "TestUser",
      "line1": "TestUser"
    },
      "data": {
      "email": "matthew.groom@ntlworld.com",
      "date": "2019-06-13",
      "subject": "Matt Test 1"
    }
    },
    "alerts": {
      "emailAddress": "matthew.groom@ntlworld.com",
      "alertTime": {
      "$date": 1560420498677
    },
      "success": true
    },
    "status": "succeeded",
    "lastUpdated": {
      "$date": 1560420498619
    },
    "hash": "n81BiQFbRwSFQ0b9rcthzPihVHpQ/wew1G8flshXeRM=",
    "statutory": false,
    "renderUrl": {
      "service": "message",
      "url": "/messages/5d02201b5b0000360151779e/content"
    },
    "externalRef": {
      "id": "5a3e51c6-f657-48dc-a132-2e72151a8e6c",
      "source": "2WSM"
    },
    "content": "RGVhciBUZXN0VXNlciBUaGFuayB5b3UgZm9yIHlvdXIgbWVzc2FnZSBvZiAxMyBKdW5lIDIwMTkuPGJyPlRvIHJlY2FwIHlvdXIgcXVlc3Rpb24sIEkgdGhpbmsgeW91J3JlIGFza2luZyBmb3IgaGVscCB3aXRoPGJyPkkgYmVsaWV2ZSB0aGlzIGFuc3dlcnMgeW91ciBxdWVzdGlvbiBhbmQgaG9wZSB5b3UgYXJlIHNhdGlzZmllZCB3aXRoIHRoZSByZXNwb25zZS48YnI+SWYgeW91IHRoaW5rIHRoZXJlIGlzIHNvbWV0aGluZyBpbXBvcnRhbnQgbWlzc2luZywgdXNlIHRoZSBsaW5rIGF0IHRoZSBlbmQgb2YgdGhpcyBtZXNzYWdlIHRvIGZpbmQgb3V0IGhvdyB0byBjb250YWN0IEhNUkMuPGJyPlJlZ2FyZHM8YnI+TWF0dGhldyBHcm9vbTxicj5uSE1SQyBkaWdpdGFsIHRlYW0u",
    "id":"5d02201b5b0000360151779e"
  }""")

  "ConversationItem" should {
    "content should be successfully decoded" in {
      val conversationItem = jsonConversationItem.validate[ConversationItem]
      conversationItem shouldBe JsSuccess(
        ConversationItem(
          "5d02201b5b0000360151779e",
          "Matt Test 1",
          Some(
            ConversationItemDetails(
              MessageType.Adviser,
              FormId.Reply,
              Some(LocalDate.parse("2019-06-13")),
              Some("5d021fbe5b0000200151779c"),
              Some("p800"),
              Some(Adviser("123")))),
          LocalDate.parse("2019-06-13"),
          Some("Dear TestUser Thank you for your message of 13 June 2019.<br>To recap your question, I think you're asking for help with<br>I believe this answers your question and hope you are satisfied with the response.<br>If you think there is something important missing, use the link at the end of this message to find out how to contact HMRC.<br>Regards<br>Matthew Groom<br>nHMRC digital team.")
        ))
    }
  }
}
