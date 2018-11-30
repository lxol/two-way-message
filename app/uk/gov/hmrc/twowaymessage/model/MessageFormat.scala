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

package uk.gov.hmrc.twowaymessage.model

import play.api.libs.json._

object MessageFormat {

  import uk.gov.hmrc.twowaymessage.model.CommonFormats._

  implicit val externalRefWrites: OWrites[ExternalRef] = Json.writes[ExternalRef]

  implicit val detailsWrites: OWrites[Details] =  Json.writes[Details]

  implicit val externalRef: Reads[ExternalRef] = Json.reads[ExternalRef]

  implicit val details: Reads[Details] = Json.reads[Details]

  implicit val messageReads: Reads[Message] = Json.reads[Message]

  implicit val messageWrites: OWrites[Message] = Json.writes[Message]

  implicit val taxIdWithNameReads: Reads[TaxIdWithName] = Json.reads[TaxIdWithName]

  implicit val taxEntityReads: Reads[TaxEntity] = Json.reads[TaxEntity]

  implicit val messageMetadataReads: Reads[MessageMetadata] = Json.reads[MessageMetadata]
}

case class Message(externalRef: ExternalRef, recipient: Recipient, messageType: String, subject: String, content: String, details: Details)

case class ExternalRef(id: String, source: String)

case class Details(formId: String, replyTo: Option[String] = None)

final case class TaxIdWithName(name: String, value: String)

final case class TaxEntity(regime: String, identifier: TaxIdWithName, email: Option[String] = None)

final case class MessageMetadata(id: String, recipient: TaxEntity, subject: String)