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

package uk.gov.hmrc.twowaymessage.assets

import uk.gov.hmrc.twowaymessage.model.{Message, _}

trait Fixtures {

  val testMessage = Message(
    ExternalRef("some-random-id", "2WSM"),
    Recipient(TaxIdentifier("nino", "AB123456C"), "email@test.com"),
    MessageType.Advisor,
    "QUESTION",
    "some base64-encoded-html",
    Details(
      FormId.Reply,
      Some("reply-to-id"),
      Some("thread-id"),
      Some("P800"),
      Some(Adviser(pidId = "adviser-id")))
  )

  def v3Message(externalRefId:String) =
    s"""
           |{
           |   "externalRef":{
           |      "id": "${externalRefId}",
           |      "source": "twsm"
           |   },
           |   "recipient":{
           |      "taxIdentifier": {
           |          "name":"nino",
           |          "value":"AB123456C"
           |      },
           |      "name": "John Smith",
           |      "email": "foo@bar.com"
           |   },
           |   "messageType":"2wsm-customer",
           |   "validFrom": "2018-09-27",
           |   "subject":"subject",
           |   "content":"content",
           |   "alertQueue": "alert queue",
           |   "details":{
           |      "formId":"2WSM-reply",
           |      "statutory": false,
           |      "paperSent": false,
           |      "batchId" : "1234"
           |   }
           |}
    """.stripMargin

  def v3Messages(externalRefId1:String, externalRefId2: String) =
    s"""
           | [
           | ${v3Message(externalRefId1)},
           | ${v3Message(externalRefId2)}
           | ]
         """.stripMargin

}
