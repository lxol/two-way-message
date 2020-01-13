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

package uk.gov.hmrc.twowaymessage.enquiries

class Enquiry(enquiries: Map[String, EnquiryType]) {
  def apply(enq: String): Option[EnquiryType] = enquiries.get(enq.toLowerCase)
}

case class EnquiryType(
                        val name: String,
                        val dmsFormId: String,
                        val classificationType: String,
                        val businessArea: String,
                        val responseTime: String,
                        val displayName: String
                      )
