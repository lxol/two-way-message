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

package uk.gov.hmrc.twowaymessage.enquiries

import play.api.Play
import play.api.libs.json.{Json, Writes}

object DMSClassificationTypes {
  val PSA_DFS_Secure_Messaging_SA = "PSA-DFS Secure Messaging SA"
}

object DMSBusinessArea {
  val PT_Operations = "PT Operations"
}

object DisplayNames {
  val P800 = ""
  val P800_OVER_PAYMENT = "P800 overpayment enquiry"
  val P800_PAID = "P800 overpayment paid enquiry"
  val P800_PROCESSING = "P800 overpayment processing enquiry"
  val P800_SENT = "P800 overpayment sent enquiry"
  val p800_NOT_AVAILABLE = "P800 overpayment not available enquiry"
  val p800_UNDER_PAYMENT = "P800 underpayment"
}

case class EnquiryType (
                         val name: String,
                         val dmsFormId: String,
                         val classificationType: String,
                         val businessArea: String,
                         val responseTime: String,
                         val displayName: String
)

object EnquiryType {
  implicit val enquiryTypeWrites: Writes[EnquiryType] = Json.writes[EnquiryType]
}

object EnquiryTypes  {

  val P800 = EnquiryType(
    name = "p800" ,
    dmsFormId = "P800",
    classificationType = DMSClassificationTypes.PSA_DFS_Secure_Messaging_SA,
    businessArea = DMSBusinessArea.PT_Operations,
    responseTime = Play.current.configuration.getString("forms.p800.responseTime").get,
    displayName = DisplayNames.p800_UNDER_PAYMENT
  )

  val P800OverPayment = EnquiryType(
    name = "p800-over-payment" ,
    dmsFormId = "P800",
    classificationType = DMSClassificationTypes.PSA_DFS_Secure_Messaging_SA,
    businessArea = DMSBusinessArea.PT_Operations,
    responseTime = Play.current.configuration.getString("forms.p800.responseTime").get,
    displayName = DisplayNames.P800_OVER_PAYMENT
  )

  val P800Paid = EnquiryType(
    name = "p800-paid" ,
    dmsFormId = "P800",
    classificationType = DMSClassificationTypes.PSA_DFS_Secure_Messaging_SA,
    businessArea = DMSBusinessArea.PT_Operations,
    responseTime = Play.current.configuration.getString("forms.p800.responseTime").get,
    displayName = DisplayNames.P800_PAID
  )

  val P800Processing = EnquiryType(
    name = "p800-processing" ,
    dmsFormId = "P800",
    classificationType = DMSClassificationTypes.PSA_DFS_Secure_Messaging_SA,
    businessArea = DMSBusinessArea.PT_Operations,
    responseTime = Play.current.configuration.getString("forms.p800.responseTime").get,
    displayName = DisplayNames.P800_PROCESSING
  )

  val P800Sent = EnquiryType(
    name = "p800-sent" ,
    dmsFormId = "P800",
    classificationType = DMSClassificationTypes.PSA_DFS_Secure_Messaging_SA,
    businessArea = DMSBusinessArea.PT_Operations,
    responseTime = Play.current.configuration.getString("forms.p800.responseTime").get,
    displayName = DisplayNames.P800_SENT
  )

  val P800NotAvailable = EnquiryType(
    name = "p800-not-available" ,
    dmsFormId = "P800",
    classificationType = DMSClassificationTypes.PSA_DFS_Secure_Messaging_SA,
    businessArea = DMSBusinessArea.PT_Operations,
    responseTime = Play.current.configuration.getString("forms.p800.responseTime").get,
    displayName = DisplayNames.p800_NOT_AVAILABLE
  )

  val P800UnderPayment = EnquiryType(
    name = "p800-underpayment" ,
    dmsFormId = "P800",
    classificationType = DMSClassificationTypes.PSA_DFS_Secure_Messaging_SA,
    businessArea = DMSBusinessArea.PT_Operations,
    responseTime = Play.current.configuration.getString("forms.p800.responseTime").get,
    displayName = DisplayNames.p800_UNDER_PAYMENT
  )

}
