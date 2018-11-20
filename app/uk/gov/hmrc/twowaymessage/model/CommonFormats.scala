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
import play.api.libs.functional.syntax._

object CommonFormats {

  implicit val taxIdentifierReads: Reads[TaxIdentifier] =
    ((__ \ "name").read[String] and
      (__ \ "value").read[String].filter(_.matches("[A-Z]{2}[0-9]{6}[A-Z]{1}"))
      ) {
      (name, value) =>
        TaxIdentifier(
          name,
          value
        )
    }
  implicit val taxIdentifierWrites: Writes[TaxIdentifier] = Json.writes[TaxIdentifier]

  implicit val recipientReads: Reads[Recipient] = Json.reads[Recipient]

  implicit val recipientWrites: Writes[Recipient] = Json.writes[Recipient]

  implicit val errorWrites: Writes[Error] = Json.writes[Error]

}
case class Recipient(taxIdentifier: TaxIdentifier, email: String)

case class TaxIdentifier(name: String, value: String)

case class Error(error: Int, message: String)






