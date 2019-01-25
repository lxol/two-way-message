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

import uk.gov.hmrc.twowaymessage.enquiries.repos.P800

object Enquiry {

  def apply (enq: String):Option[EnquiryTemplate] = enquiries.get(enq)


  private val enquiries = Map[String, EnquiryTemplate](
    "p800" -> P800
  )


  abstract class EnquiryTemplate {
    val title : String
    val classificationType : String
    val businessArea : String
  }

}






