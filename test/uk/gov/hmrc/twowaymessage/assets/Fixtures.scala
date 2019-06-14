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

import org.joda.time.LocalDate
import uk.gov.hmrc.twowaymessage.model.{Message, _}

trait Fixtures {

  val testMessage = Message(
    ExternalRef("some-random-id", "2WSM"),
    Recipient(TaxIdentifier("nino", "AB123456C"), "email@test.com"),
    MessageType.Adviser,
    "QUESTION",
    "some base64-encoded-html",
    Details(
      FormId.Reply,
      Some("reply-to-id"),
      Some("thread-id"),
      Some("P800"),
      Some(Adviser(pidId = "adviser-id")))
  )

  val testConversationItem = ConversationItem(
    id = "5d02201b5b0000360151779e",
    subject = "test subject",
    body = Some(
      ConversationItemDetails(
      `type` = MessageType.Adviser,
      form = FormId.Reply,
      issueDate = Some(LocalDate.now),
      replyTo = Some("reply-to-id"),
      enquiryType = Some("test-enquiry-type"),
      adviser = Some(Adviser(pidId = "adviser-id")))),
    validFrom = LocalDate.now,
    content = Some("test-content")
  )

  def conversationItem(id:String) =
     s"""
     | {
     |     "renderUrl": {
     |         "url": "relUrl",
     |         "service": "service"
     |     },
     |     "statutory": false,
     |     "hash": "24d5d7da-1b11-4d38-a730-b2f952969440",
     |     "lastUpdated": 1554296147553,
     |     "status": "todo",
     |     "readTime": 1554296147548,
     |     "alerts": {
     |         "success": true,
     |         "alertTime": 1554296147552,
     |         "emailAddress": "2afd5ea4-b12e-4851-82d2-774cef6b3c83@test.com"
     |     },
     |     "alertDetails": {
     |         "data": {
     |         },
     |         "templateId": "templateId"
     |     },
     |     "alertFrom": "2013-12-01",
     |     "validFrom": "2013-12-01",
     |     "body": {
     |         "type": "2wsm-advisor",
     |         "adviser": {
     |             "pidId": "adviser-id"
     |         },
     |         "enquiryType": "enquiry-type",
     |         "threadId": "530410d70000000000000000",
     |         "issueDate": "2019-04-03",
     |         "detailsId": "C0123456781234568",
     |         "suppressedAt": "2013-01-02",
     |         "form": "2WSM-question"
     |     },
     |     "subject": "Blah blah blah",
     |     "recipient": {
     |         "identifier": {
     |             "value": "8000045498",
     |             "name": "sautr"
     |         },
     |         "regime": "sa"
     |     },
     |     "id": "${id}"
     | }
     """.stripMargin

  def conversationItems(id1:String, id2: String) =
    s"""
           | [
           | ${conversationItem(id1)},
           | ${conversationItem(id2)}
           | ]
         """.stripMargin

}
