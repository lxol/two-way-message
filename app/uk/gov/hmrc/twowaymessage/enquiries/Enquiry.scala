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

object Enquiry {

  def apply (enq: String):Option[EnquiryType] = enquiries.get(enq.toLowerCase)

  private val enquiries = Map[String, EnquiryType](
    "p800" -> EnquiryTypes.P800,
    "p800-over-payment" -> EnquiryTypes.P800OverPayment,
    "p800-paid" -> EnquiryTypes.P800Paid,
    "p800-processing" -> EnquiryTypes.P800Processing,
    "p800-sent" -> EnquiryTypes.P800Sent,
    "p800-not-available" -> EnquiryTypes.P800NotAvailable,
    "p800-underpayment" -> EnquiryTypes.P800UnderPayment
  )
}
