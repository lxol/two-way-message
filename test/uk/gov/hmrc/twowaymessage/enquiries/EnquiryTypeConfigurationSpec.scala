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

import com.codahale.metrics.SharedMetricRegistries
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder

class EnquiryTypeConfigurationSpec extends WordSpec
  with Matchers with GuiceOneAppPerSuite {

  val injector = new GuiceApplicationBuilder().injector()

  val enquiries = injector.instanceOf[Enquiry]


  "Enquiry configuration for p800" should {

    "should have correct enquiry details" in {
      enquiries("p800").get shouldBe EnquiryType(
        name = "p800",
        dmsFormId = "P800",
        classificationType = "PSA-DFS Secure Messaging SA",
        businessArea = "PT Operations",
        responseTime = "5 days",
        displayName = "P800 underpayment"
      )
    }
  }

  "Enquiry configuration for p800-over-payment" should {

    "should have correct enquiry details" in {
      enquiries("p800-over-payment").get shouldBe EnquiryType(
        name = "p800-over-payment",
        dmsFormId = "P800",
        classificationType = "PSA-DFS Secure Messaging SA",
        businessArea = "PT Operations",
        responseTime = "5 days",
        displayName = "P800 overpayment enquiry"
      )
    }
  }

  "Enquiry configuration for p800-paid" should {

    "should have correct enquiry details" in {
      enquiries("p800-paid").get shouldBe EnquiryType(
        name = "p800-paid",
        dmsFormId = "P800",
        classificationType = "PSA-DFS Secure Messaging SA",
        businessArea = "PT Operations",
        responseTime = "5 days",
        displayName = "P800 overpayment paid enquiry"
      )
    }
  }

  "Enquiry configuration for p800-processing" should {

    "should have correct enquiry details" in {
      enquiries("p800-processing").get shouldBe EnquiryType(
        name = "p800-processing",
        dmsFormId = "P800",
        classificationType = "PSA-DFS Secure Messaging SA",
        businessArea = "PT Operations",
        responseTime = "5 days",
        displayName = "P800 overpayment processing enquiry"
      )
    }
  }

  "Enquiry configuration for p800-sent" should {

    "should have correct enquiry details" in {
      enquiries("p800-sent").get shouldBe EnquiryType(
        name = "p800-sent",
        dmsFormId = "P800",
        classificationType = "PSA-DFS Secure Messaging SA",
        businessArea = "PT Operations",
        responseTime = "5 days",
        displayName = "P800 overpayment sent enquiry"
      )
    }
  }

  "Enquiry configuration for p800-not-available" should {

    "should have correct enquiry details" in {
      enquiries("p800-not-available").get shouldBe EnquiryType(
        name = "p800-not-available",
        dmsFormId = "P800",
        classificationType = "PSA-DFS Secure Messaging SA",
        businessArea = "PT Operations",
        responseTime = "5 days",
        displayName = "P800 overpayment not available enquiry"
      )
    }
  }

  "Enquiry configuration for p800-underpayment" should {

    "should have correct enquiry details" in {
      enquiries("p800-underpayment").get shouldBe EnquiryType(
        name = "p800-underpayment",
        dmsFormId = "P800",
        classificationType = "PSA-DFS Secure Messaging SA",
        businessArea = "PT Operations",
        responseTime = "5 days",
        displayName = "P800 underpayment"
      )
    }
  }

  SharedMetricRegistries.clear

}
