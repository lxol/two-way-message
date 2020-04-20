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

import com.codahale.metrics.SharedMetricRegistries
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder

class EnquiryTypeConfigurationSpec extends WordSpec
  with Matchers with GuiceOneAppPerSuite {

  val injector: Injector = new GuiceApplicationBuilder().injector()

  val enquiries: Enquiry = injector.instanceOf[Enquiry]


  "Enquiry configuration for p800" should {

    "should have correct enquiry details" in {
      enquiries("p800").get shouldBe EnquiryType(
        name = "p800",
        dmsFormId = "p800-under",
        classificationType = "PSA-DFS Secure Messaging SA",
        businessArea = "PT Operations",
        responseTime = "5 days",
        displayName = "P800 underpayment",
        pdfPageTitle = "Received from: P800 secure message",
        pdfTaxIdTitle = "National insurance number"
      )
    }
  }

  "Enquiry configuration for p800-overpayment" should {

    "should have correct enquiry details" in {
      enquiries("p800-overpayment").get shouldBe EnquiryType(
        name = "p800-overpayment",
        dmsFormId = "p800-over",
        classificationType = "PSA-DFS Secure Messaging SA",
        businessArea = "PT Operations",
        responseTime = "5 days",
        displayName = "P800 overpayment enquiry",
        pdfPageTitle = "Received from: P800 secure message",
        pdfTaxIdTitle = "National insurance number"
      )
    }
  }

  "Enquiry configuration for p800-paid" should {

    "should have correct enquiry details" in {
      enquiries("p800-paid").get shouldBe EnquiryType(
        name = "p800-paid",
        dmsFormId = "p800-paid",
        classificationType = "PSA-DFS Secure Messaging SA",
        businessArea = "PT Operations",
        responseTime = "5 days",
        displayName = "P800 overpayment paid enquiry",
        pdfPageTitle = "Received from: P800 secure message",
        pdfTaxIdTitle = "National insurance number"
      )
    }
  }

  "Enquiry configuration for p800-processing" should {

    "should have correct enquiry details" in {
      enquiries("p800-processing").get shouldBe EnquiryType(
        name = "p800-processing",
        dmsFormId = "p800-process",
        classificationType = "PSA-DFS Secure Messaging SA",
        businessArea = "PT Operations",
        responseTime = "5 days",
        displayName = "P800 overpayment processing enquiry",
        pdfPageTitle = "Received from: P800 secure message",
        pdfTaxIdTitle = "National insurance number"
      )
    }
  }

  "Enquiry configuration for p800-sent" should {

    "should have correct enquiry details" in {
      enquiries("p800-sent").get shouldBe EnquiryType(
        name = "p800-sent",
        dmsFormId = "p800-cheque",
        classificationType = "PSA-DFS Secure Messaging SA",
        businessArea = "PT Operations",
        responseTime = "5 days",
        displayName = "P800 overpayment sent enquiry",
        pdfPageTitle = "Received from: P800 secure message",
        pdfTaxIdTitle = "National insurance number"
      )
    }
  }

  "Enquiry configuration for p800-not-available" should {

    "should have correct enquiry details" in {
      enquiries("p800-not-available").get shouldBe EnquiryType(
        name = "p800-not-available",
        dmsFormId = "p800-unavail",
        classificationType = "PSA-DFS Secure Messaging SA",
        businessArea = "PT Operations",
        responseTime = "5 days",
        displayName = "P800 overpayment not available enquiry",
        pdfPageTitle = "Received from: P800 secure message",
        pdfTaxIdTitle = "National insurance number"
      )
    }
  }

  "Enquiry configuration for p800-underpayment" should {

    "should have correct enquiry details" in {
      enquiries("p800-underpayment").get shouldBe EnquiryType(
        name = "p800-underpayment",
        dmsFormId = "p800-under",
        classificationType = "PSA-DFS Secure Messaging SA",
        businessArea = "PT Operations",
        responseTime = "5 days",
        displayName = "P800 underpayment",
        pdfPageTitle = "Received from: P800 secure message",
        pdfTaxIdTitle = "National insurance number"
      )
    }
  }

  "Enquiry configuration for sa-general" should {

    "have correct enquiry details" in {
      enquiries("sa-general").get shouldBe EnquiryType(
        name = "sa-general",
        dmsFormId = "sa-general",
        classificationType = "DMB-SA-Secure Messaging",
        businessArea = "DMB",
        responseTime = "5 days",
        displayName = "Self Assessment",
        pdfPageTitle = "Received from: SA secure question",
        pdfTaxIdTitle = "UTR"
      )
    }
  }

  "Enquiry configuration for ct-general" should {

    "have correct enquiry details" in {
      enquiries("ct-general").get shouldBe EnquiryType(
        name = "ct-general",
        dmsFormId = "ct-general",
        classificationType = "DMB-CT-Secure Messaging",
        businessArea = "DMB",
        responseTime = "5 days",
        displayName = "Corporation Tax",
        pdfPageTitle = "Received from: CT secure question",
        pdfTaxIdTitle = "UTR"
      )
    }
  }

  "Enquiry configuration for vat-general" should {

    "have correct enquiry details" in {
      enquiries("vat-general").get shouldBe EnquiryType(
        name = "vat-general",
        dmsFormId = "vat-general",
        classificationType = "DMB-VAT-Secure Messaging",
        businessArea = "DMB",
        responseTime = "5 days",
        displayName = "VAT",
        pdfPageTitle = "Received from: VAT secure question",
        pdfTaxIdTitle = "Vat number"

      )
    }
  }

  "Enquiry configuration for epaye-general" should {

    "have correct enquiry details" in {
      enquiries("epaye-general").get shouldBe EnquiryType(
        name = "epaye-general",
        dmsFormId = "epaye-general",
        classificationType = "DMB-PAYE-Secure Messaging",
        businessArea = "DMB",
        responseTime = "5 days",
        displayName = "PAYE for employers",
        pdfPageTitle = "Received from: EPAYE secure question",
        pdfTaxIdTitle = "EmpRef number"
        )
    }
  }

  "Enquiry configuration for epaye-jrs" should {

    "have correct enquiry details" in {
      enquiries("epaye-jrs").get shouldBe EnquiryType(
        name = "epaye-jrs",
        dmsFormId = "epaye-jrs",
        classificationType = "Job Retention-TWSM",
        businessArea = "Universal",
        responseTime = "5 days",
        displayName = "PAYE for employers Job Retention Scheme",
        pdfPageTitle = "Received from: EPAYE Job Retention Scheme",
        pdfTaxIdTitle = "Employer PAYE reference"
      )
    }
  }

  SharedMetricRegistries.clear()

}
