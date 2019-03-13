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

import play.api.libs.json._
import MessageType._
import FormId._
import uk.gov.hmrc.domain.Nino

object MessageFormat {

  implicit val taxIdentifierWrites: Writes[TaxIdentifier] = Json.writes[TaxIdentifier]

  implicit val recipientWrites: Writes[Recipient] = Json.writes[Recipient]

  implicit val detailsWrites: OWrites[Details] = Json.writes[Details]

  implicit val externalRefWrites: OWrites[ExternalRef] = Json.writes[ExternalRef]

  implicit val messageWrites: OWrites[Message] = Json.writes[Message]

  implicit val taxIdWithNameReads: Reads[TaxIdWithName] = Json.reads[TaxIdWithName]

  implicit val taxEntityReads: Reads[TaxEntity] = Json.reads[TaxEntity]

  implicit val messageMetadataReads: Reads[MessageMetadata] = Json.reads[MessageMetadata]

  implicit val taxIdWithNameWrites: Writes[TaxIdWithName] = Json.writes[TaxIdWithName]

  implicit val taxEntityWrites: Writes[TaxEntity] = Json.writes[TaxEntity]

  implicit val messageMetadataWrites: Writes[MessageMetadata] = Json.writes[MessageMetadata]
}

case class Recipient(taxIdentifier: TaxIdentifier, email: String)

case class TaxIdentifier(name: String, value: String)

case class Message(
  externalRef: ExternalRef,
  recipient: Recipient,
  messageType: MessageType,
  subject: String,
  content: String,
  details: Details)

case class ExternalRef(id: String, source: String)

case class Adviser(pidId: String)
object Adviser {
  implicit val adviserFormat: Format[Adviser] = Json.format[Adviser]
}

case class Details(
  formId: FormId,
  replyTo: Option[String] = None,
  threadId: Option[String] = None,
  enquiryType: Option[String] = None,
  adviser: Option[Adviser] = None)

case class MetadataDetails(threadId: Option[String], enquiryType: Option[String], adviser: Option[Adviser])
object MetadataDetails {
  implicit val metadataDetailsFormat: Format[MetadataDetails] = Json.format[MetadataDetails]
  val a = Nino(???)
}

final case class TaxIdWithName(name: String, value: String)

final case class TaxEntity(regime: String, identifier: TaxIdWithName, email: Option[String] = None)

final case class MessageMetadata(id: String, recipient: TaxEntity, subject: String, details: MetadataDetails)
