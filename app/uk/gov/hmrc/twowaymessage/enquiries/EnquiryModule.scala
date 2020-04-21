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

import com.google.inject.{ AbstractModule, Provides }
import javax.inject.Singleton
import play.api.{ Configuration, Environment }

class EnquiryModule(val environment: Environment, val configuration: Configuration) extends AbstractModule {

  @Provides
  @Singleton
  def enquiry: Enquiry =
    new Enquiry(
      configuration
        .get[Seq[Configuration]]("enquiry-types")
        .map { c =>
          {
            val t = EnquiryType(
              name = c.get[String]("name"),
              dmsFormId = c.get[String]("dmsFormId"),
              classificationType = c.get[String]("classificationType"),
              businessArea = c.get[String]("businessArea"),
              responseTime = c.get[String]("responseTime"),
              displayName = c.get[String]("displayName"),
              pdfPageTitle = c.get[String]("pdfPageTitle"),
              pdfTaxIdTitle = c.get[String]("pdfTaxIdTitle")
            )
            t.name -> t
          }
        }
        .toMap
    )
}
