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

package uk.gov.hmrc.twowaymessage.controllers

import java.util.UUID

import com.codahale.metrics.SharedMetricRegistries
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{ Injector, bind }
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.Helpers._
import play.api.test.{ FakeHeaders, FakeRequest, Helpers }
import uk.gov.hmrc.auth.core.AuthProvider.{ GovernmentGateway, PrivilegedApplication }
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.{ EmptyPredicate, Predicate }
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{ Name, ~ }
import uk.gov.hmrc.domain.{ CtUtr, EmpRef, HmrcMtdVat, HmrcObtdsOrg, Nino, SaUtr, SimpleName, TaxIdentifier }
import uk.gov.hmrc.gform.dms.DmsMetadata
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.twowaymessage.assets.TestUtil
import uk.gov.hmrc.twowaymessage.connector.mocks.MockAuthConnector
import uk.gov.hmrc.twowaymessage.enquiries.EnquiryType
import uk.gov.hmrc.twowaymessage.model.{ TwoWayMessage, TwoWayMessageReply }
import uk.gov.hmrc.twowaymessage.services.TwoWayMessageService

import scala.concurrent.Future

class AuthTwoWayMessageControllerSpec extends TestUtil with MockAuthConnector {

  val mockMessageService: TwoWayMessageService = mock[TwoWayMessageService]

  override lazy val injector: Injector = new GuiceApplicationBuilder()
    .overrides(bind[TwoWayMessageService].to(mockMessageService))
    .overrides(bind[AuthConnector].to(mockAuthConnector))
    .injector()

  val testTwoWayMessageController: TwoWayMessageController = injector.instanceOf[TwoWayMessageController]

  val authPredicate: Predicate = EmptyPredicate

  val twoWayMessageGood: JsValue = Json.parse("""
                                                |    {
                                                |      "contactDetails": {
                                                |         "email":"someEmail@test.com"
                                                |      },
                                                |      "subject":"QUESTION",
                                                |      "content":"SGVsbG8gV29ybGQ="
                                                |    }""".stripMargin)

  val fakeRequest1 = FakeRequest(
    Helpers.POST,
    routes.TwoWayMessageController.createMessage("queueName").url,
    FakeHeaders(),
    twoWayMessageGood)

  "The TwoWayMessageController.createMessage method" should {

    "return 201 (CREATED) when a message is successfully created by the message service with a valid Nino" in {
      val nino = Nino("AB123456C")
      val name = Name(Option("firstname"), Option("surename"))

      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(
        Future.successful(new ~(Enrolments(Set(enrol("HMRC-NI", "nino", "AB123456C"))), Some(name))))

      when(mockMessageService
        .post(any[EnquiryType], org.mockito.ArgumentMatchers.eq(nino), any[TwoWayMessage], any[DmsMetadata], any[Name])(
          any[HeaderCarrier]))
        .thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))
      val result = await(testTwoWayMessageController.createMessage("p800")(fakeRequest1))
      result.header.status mustBe Status.CREATED
    }

    "return 403 (FORBIDDEN) when AuthConnector doesn't return a Nino" in {
      val name = Name(Option("unknown"), Option("user"))
      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(
        Future.successful(new ~(Enrolments(Set()), Some(name))))
      val result = await(testTwoWayMessageController.createMessage("p800")(fakeRequest1))
      result.header.status mustBe Status.FORBIDDEN
    }

    "return 403 (FORBIDDEN) when AuthConnector has sautr but no Nino" in {
      val name = Name(Option("unknown"), Option("user"))
      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(
        Future.successful(new ~(Enrolments(Set(enrol("IR-SA", "sautr", "1234567890"))), Some(name))))
      val result = await(testTwoWayMessageController.createMessage("p800")(fakeRequest1))
      result.header.status mustBe Status.FORBIDDEN
    }

    "return 403 (FORBIDDEN) when createMessage is presented with an invalid queue id" in {
      val name = Name(Option("unknown"), Option("user"))
      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(
        Future.successful(new ~(Enrolments(Set(enrol("HMRC-NI", "nino", "AB123456C"))), Some(name))))
      val result = await(testTwoWayMessageController.createMessage("vat-general")(fakeRequest1))
      result.header.status mustBe Status.FORBIDDEN
    }

    "return 401 (UNAUTHORIZED) when AuthConnector returns an exception that extends NoActiveSession" in {
      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(Future.failed(MissingBearerToken()))
      val result = await(testTwoWayMessageController.createMessage("p800")(fakeRequest1))
      result.header.status mustBe Status.UNAUTHORIZED
    }

    "return 403 (FORBIDDEN) when AuthConnector returns an exception that doesn't extend NoActiveSession" in {
      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(Future.failed(InsufficientEnrolments()))
      val result = await(testTwoWayMessageController.createMessage("p800")(fakeRequest1))
      result.header.status mustBe Status.FORBIDDEN
    }

    SharedMetricRegistries.clear
  }

  "The TwoWayMessageController.createMessage method for sa-general" should {

    "return 201 (CREATED) when a message is successfully created by the message service with a valid sautr" in {
      val sautr = SaUtr("1234567890")
      val name = Name(Option("firstname"), Option("surename"))

      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(
        Future.successful(new ~(Enrolments(Set(enrol("IR-SA", "sautr", "1234567890"))), Some(name))))

      when(
        mockMessageService
          .post(
            any[EnquiryType],
            org.mockito.ArgumentMatchers.eq(sautr),
            any[TwoWayMessage],
            any[DmsMetadata],
            any[Name])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))
      val result = await(testTwoWayMessageController.createMessage("sa-general")(fakeRequest1))
      result.header.status mustBe Status.CREATED
    }

    "return 403 (FORBIDDEN) when AuthConnector doesn't return a sautr" in {
      val name = Name(Option("unknown"), Option("user"))
      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(
        Future.successful(new ~(Enrolments(Set()), Some(name))))
      val result = await(testTwoWayMessageController.createMessage("sa-general")(fakeRequest1))
      result.header.status mustBe Status.FORBIDDEN
    }

    "return 403 (FORBIDDEN) when createMessage is presented with an invalid queue id" in {
      val name = Name(Option("unknown"), Option("user"))
      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(
        Future.successful(new ~(Enrolments(Set(enrol("IR-SA", "sautr", "1234567890"))), Some(name))))
      val result = await(testTwoWayMessageController.createMessage("vat-general")(fakeRequest1))
      result.header.status mustBe Status.FORBIDDEN
    }

    SharedMetricRegistries.clear
  }

  "The TwoWayMessageController.createMessage method for ct-general" should {

    "return 201 (CREATED) when a message is successfully created by the message service with a valid ctutr" in {
      val ctutr = CtUtr("1234")
      val name = Name(Option("firstname"), Option("surename"))

      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(
        Future.successful(new ~(Enrolments(Set(enrol("IR-CT", "ctutr", "1234"))), Some(name))))

      when(
        mockMessageService
          .post(
            any[EnquiryType],
            org.mockito.ArgumentMatchers.eq(ctutr),
            any[TwoWayMessage],
            any[DmsMetadata],
            any[Name])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))
      val result = await(testTwoWayMessageController.createMessage("ct-general")(fakeRequest1))
      result.header.status mustBe Status.CREATED
    }

    "return 403 (FORBIDDEN) when AuthConnector doesn't return a ctutr" in {
      val name = Name(Option("unknown"), Option("user"))
      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(
        Future.successful(new ~(Enrolments(Set()), Some(name))))
      val result = await(testTwoWayMessageController.createMessage("ct-general")(fakeRequest1))
      result.header.status mustBe Status.FORBIDDEN
    }

    "return 403 (FORBIDDEN) when createMessage is presented with an invalid queue id" in {
      val name = Name(Option("unknown"), Option("user"))
      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(
        Future.successful(new ~(Enrolments(Set(enrol("IR-CT", "ctutr", "1234"))), Some(name))))
      val result = await(testTwoWayMessageController.createMessage("vat-general")(fakeRequest1))
      result.header.status mustBe Status.FORBIDDEN
    }

    SharedMetricRegistries.clear
  }

  "The TwoWayMessageController.createMessage method for vat-general" should {

    "return 201 (CREATED) when a message is successfully created by the message service with a valid HmrcMtdVat" in {
      val hmrcMtdVat = HmrcMtdVat("1234567890")
      val name = Name(Option("firstname"), Option("surename"))

      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(
        Future.successful(new ~(Enrolments(Set(enrol("HMRC-MTD-VAT", "HMRC-MTD-VAT", "1234567890"))), Some(name))))

      when(
        mockMessageService
          .post(
            any[EnquiryType],
            org.mockito.ArgumentMatchers.eq(hmrcMtdVat),
            any[TwoWayMessage],
            any[DmsMetadata],
            any[Name])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))
      val result = await(testTwoWayMessageController.createMessage("vat-general")(fakeRequest1))
      result.header.status mustBe Status.CREATED
    }

    "return 403 (FORBIDDEN) when AuthConnector doesn't return a HmrcMtdVat" in {
      val name = Name(Option("unknown"), Option("user"))
      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(
        Future.successful(new ~(Enrolments(Set()), Some(name))))
      val result = await(testTwoWayMessageController.createMessage("vat-general")(fakeRequest1))
      result.header.status mustBe Status.FORBIDDEN
    }

    "return 403 (FORBIDDEN) when createMessage is presented with an invalid queue id" in {
      val name = Name(Option("unknown"), Option("user"))
      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(
        Future.successful(new ~(Enrolments(Set(enrol("HMRC-MTD-VAT", "HMRC-MTD-VAT", "1234567890"))), Some(name))))
      val result = await(testTwoWayMessageController.createMessage("ct-general")(fakeRequest1))
      result.header.status mustBe Status.FORBIDDEN
    }

    SharedMetricRegistries.clear
  }

  "The TwoWayMessageController.createMessage method for epaye-general" should {

    "return 201 (CREATED) when a message is successfully created by the message service with a valid EmpRef" in {
      val empRef = new TaxIdentifier with SimpleName {
        override val name: String = "empRef"
        override def value: String = "12345/67890"
      }
      val name = Name(Option("firstname"), Option("surename"))

      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(
        Future.successful(new ~(Enrolments(Set(enrolEmpRef("IR-PAYE", "12345", "67890"))), Some(name))))

      when(
        mockMessageService
          .post(any[EnquiryType], any[TaxIdentifier with SimpleName], any[TwoWayMessage], any[DmsMetadata], any[Name])(
            any[HeaderCarrier]))
        .thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))
      val result = await(testTwoWayMessageController.createMessage("epaye-general")(fakeRequest1))
      result.header.status mustBe Status.CREATED
    }

    "return 403 (FORBIDDEN) when AuthConnector doesn't return a EPAYE" in {
      val name = Name(Option("unknown"), Option("user"))
      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(
        Future.successful(new ~(Enrolments(Set()), Some(name))))
      val result = await(testTwoWayMessageController.createMessage("epaye-general")(fakeRequest1))
      result.header.status mustBe Status.FORBIDDEN
    }

    "return 403 (FORBIDDEN) when createMessage is presented with an invalid queue id" in {
      val name = Name(Option("unknown"), Option("user"))
      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(
        Future.successful(new ~(Enrolments(Set(enrolEmpRef("IR-PAYE", "12345", "67890"))), Some(name))))
      val result = await(testTwoWayMessageController.createMessage("ct-general")(fakeRequest1))
      result.header.status mustBe Status.FORBIDDEN
    }

    SharedMetricRegistries.clear
  }

  "The TwoWayMessageController.createMessage method for epaye-jrs" should {

    "return 201 (CREATED) when a message is successfully created by the message service with a valid EmpRef" in {
      val empRef = new TaxIdentifier with SimpleName {
        override val name: String = "empRef"
        override def value: String = "12345/67890"
      }
      val name = Name(Option("firstname"), Option("surename"))

      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(
        Future.successful(new ~(Enrolments(Set(enrolEmpRef("IR-PAYE", "12345", "67890"))), Some(name))))

      when(
        mockMessageService
          .post(any[EnquiryType], any[TaxIdentifier with SimpleName], any[TwoWayMessage], any[DmsMetadata], any[Name])(
            any[HeaderCarrier]))
        .thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))
      val result = await(testTwoWayMessageController.createMessage("epaye-jrs")(fakeRequest1))
      result.header.status mustBe Status.CREATED
    }

    "return 403 (FORBIDDEN) when AuthConnector doesn't return a EPAYE" in {
      val name = Name(Option("unknown"), Option("user"))
      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(
        Future.successful(new ~(Enrolments(Set()), Some(name))))
      val result = await(testTwoWayMessageController.createMessage("epaye-jrs")(fakeRequest1))
      result.header.status mustBe Status.FORBIDDEN
    }

    "return 403 (FORBIDDEN) when createMessage is presented with an invalid queue id" in {
      val name = Name(Option("unknown"), Option("user"))
      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(
        Future.successful(new ~(Enrolments(Set(enrolEmpRef("IR-PAYE", "12345", "67890"))), Some(name))))
      val result = await(testTwoWayMessageController.createMessage("ct-general")(fakeRequest1))
      result.header.status mustBe Status.FORBIDDEN
    }

    SharedMetricRegistries.clear
  }

  "The TwoWayMessageController.createMessage method for vat-general/HMCE-VATDEC-ORG" should {

    "return 201 (CREATED) when a message is successfully created by the message service with a valid HMCE-VATDEC-ORG" in {
      val vatdec = new TaxIdentifier with SimpleName {
        override val name: String = "HMCE-VATDEC-ORG"
        override def value: String = "1234567890"
      }
      val name = Name(Option("firstname"), Option("surename"))

      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(
        Future.successful(new ~(Enrolments(Set(enrol("HMCE-VATDEC-ORG", "VATRegNo", "1234567890"))), Some(name))))

      when(
        mockMessageService
          .post(any[EnquiryType], any[TaxIdentifier with SimpleName], any[TwoWayMessage], any[DmsMetadata], any[Name])(
            any[HeaderCarrier]))
        .thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))
      val result = await(testTwoWayMessageController.createMessage("vat-general")(fakeRequest1))
      result.header.status mustBe Status.CREATED
    }

    "return 403 (FORBIDDEN) when AuthConnector doesn't return HMCE-VATDEC-ORG entitlements" in {
      val name = Name(Option("unknown"), Option("user"))
      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(
        Future.successful(new ~(Enrolments(Set()), Some(name))))
      val result = await(testTwoWayMessageController.createMessage("vat-general")(fakeRequest1))
      result.header.status mustBe Status.FORBIDDEN
    }

    "return 403 (FORBIDDEN) when createMessage is presented with an invalid queue id" in {
      val name = Name(Option("unknown"), Option("user"))
      mockAuthorise(retrievals = Retrievals.allEnrolments and Retrievals.name)(
        Future.successful(new ~(Enrolments(Set(enrol("HMCE-VATDEC-ORG", "VATRegNo", "1234567890"))), Some(name))))
      val result = await(testTwoWayMessageController.createMessage("ct-general")(fakeRequest1))
      result.header.status mustBe Status.FORBIDDEN
    }

    SharedMetricRegistries.clear
  }
  "The TwoWayMessageController.createCustomerResponse method" should {

    "return 201 (CREATED) when a message is successfully created by the message service with a valid Nino" in {
      val nino = Nino("AB123456C")
      mockAuthorise(retrievals = Retrievals.allEnrolments)(
        Future.successful(new Enrolments(Set(enrol("HMRC-NI", "nino", "AB123456C")))))
      when(
        mockMessageService.postCustomerReply(any[TwoWayMessageReply], ArgumentMatchers.eq("replyTo"))(
          any[HeaderCarrier]))
        .thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))
      val result = await(testTwoWayMessageController.createCustomerResponse("p800", "replyTo")(fakeRequest1))
      result.header.status mustBe Status.CREATED
    }

    "return 401 (UNAUTHORIZED) when AuthConnector returns an exception that extends NoActiveSession" in {
      mockAuthorise(retrievals = Retrievals.allEnrolments)(Future.failed(MissingBearerToken()))
      val result = await(testTwoWayMessageController.createCustomerResponse("p800", "replyTo")(fakeRequest1))
      result.header.status mustBe Status.UNAUTHORIZED
    }

    "return 403 (FORBIDDEN) when AuthConnector returns an exception that doesn't extend NoActiveSession" in {
      mockAuthorise(retrievals = Retrievals.allEnrolments)(Future.failed(InsufficientEnrolments()))
      val result: Result =
        await(testTwoWayMessageController.createCustomerResponse("queueName", "replyTo")(fakeRequest1))
      result.header.status mustBe Status.FORBIDDEN
    }

    SharedMetricRegistries.clear
  }

  "The TwoWayMessageController.getContentBy method" should {
    "return 200 (OK) when the message type is valid" in {
      val nino = Nino("AB123456C")
      mockAuthorise(AuthProviders(GovernmentGateway, PrivilegedApplication))(Future.successful())
      when(mockMessageService.findMessagesBy(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(List())))
      val result = await(testTwoWayMessageController.getContentBy("1", "Customer")(fakeRequest1).run())
      result.header.status mustBe Status.OK
    }

    "return 400 (BAD_REQUEST) when the message type is invalid" in {
      mockAuthorise(AuthProviders(GovernmentGateway, PrivilegedApplication))(Future.successful())
      val result = await(testTwoWayMessageController.getContentBy("1", "nfejwk")(fakeRequest1).run())
      result.header.status mustBe Status.BAD_REQUEST
    }

    SharedMetricRegistries.clear
  }

  "The TwoWayMessageController.getEnquiryTypeDetails method" should {

    case class EnquiryTypeDetailsScenario(
      enquiryType: String,
      enrolments: Set[Enrolment],
      expectedDisplayName: String,
      invalidEnquiryType: String,
      taxIdName: String,
      taxId: String)

    val hmrcNiEnrol = enrol("HMRC-NI", "nino", "AB123456C")
    val enquiryTypeDisplayNameMap = List(
      EnquiryTypeDetailsScenario("p800", Set(hmrcNiEnrol), "P800 underpayment", "epaye-general", "nino", "AB123456C"),
      EnquiryTypeDetailsScenario(
        "p800-overpayment",
        Set(hmrcNiEnrol),
        "P800 overpayment enquiry",
        "epaye-general",
        "nino",
        "AB123456C"),
      EnquiryTypeDetailsScenario(
        "p800-paid",
        Set(hmrcNiEnrol),
        "P800 overpayment paid enquiry",
        "epaye-general",
        "nino",
        "AB123456C"),
      EnquiryTypeDetailsScenario(
        "p800-processing",
        Set(hmrcNiEnrol),
        "P800 overpayment processing enquiry",
        "epaye-general",
        "nino",
        "AB123456C"),
      EnquiryTypeDetailsScenario(
        "p800-sent",
        Set(hmrcNiEnrol),
        "P800 overpayment sent enquiry",
        "epaye-general",
        "nino",
        "AB123456C"),
      EnquiryTypeDetailsScenario(
        "p800-not-available",
        Set(hmrcNiEnrol),
        "P800 overpayment not available enquiry",
        "epaye-general",
        "nino",
        "AB123456C"),
      EnquiryTypeDetailsScenario(
        "p800-underpayment",
        Set(hmrcNiEnrol),
        "P800 underpayment",
        "epaye-general",
        "nino",
        "AB123456C"),
      EnquiryTypeDetailsScenario(
        "sa-general",
        Set(enrol("IR-SA", "sautr", "1234567890")),
        "Self Assessment",
        "epaye-general",
        "sautr",
        "1234567890"),
      EnquiryTypeDetailsScenario(
        "ct-general",
        Set(enrol("IR-CT", "ctutr", "1234")),
        "Corporation Tax",
        "epaye-general",
        "ctutr",
        "1234"),
      EnquiryTypeDetailsScenario(
        "vat-general",
        Set(enrol("HMRC-MTD-VAT", "HMRC-MTD-VAT", "1234567890")),
        "VAT",
        "epaye-general",
        "HMRC-MTD-VAT",
        "1234567890"),
      EnquiryTypeDetailsScenario(
        "epaye-general",
        Set(enrolEmpRef("IR-PAYE", "12345", "67890")),
        "PAYE for employers",
        "p800",
        "empRef",
        "12345/67890"),
      EnquiryTypeDetailsScenario(
        "epaye-jrs",
        Set(enrolEmpRef("IR-PAYE", "12183", "23190")),
        "PAYE for employers Job Retention Scheme",
        "p800",
        "empRef",
        "12183/23190")
    )

    enquiryTypeDisplayNameMap.foreach(scenario => {
      s"return 200 when ${scenario.enquiryType} enquiry type is requested" in {
        val name = Name(Option("firstname"), Option("surename"))
        mockAuthorise(retrievals = Retrievals.allEnrolments)(Future.successful(Enrolments(scenario.enrolments)))
        val result = testTwoWayMessageController.getEnquiryTypeDetails(scenario.enquiryType)(FakeRequest())
        await(result).header.status mustBe Status.OK
        Json.parse(contentAsString(result)) mustBe
          Json.parse(s"""{
                        |"displayName":"${scenario.expectedDisplayName}",
                        |"responseTime":"5 days",
                        |"taxIdName":"${scenario.taxIdName}",
                        |"taxId":"${scenario.taxId}"
                        |}""".stripMargin)
      }

      s"return 401 (UNAUTHORIZED) when AuthConnector returns an exception that extends NoActiveSession for ${scenario.enquiryType} enquiry type" in {
        mockAuthorise(retrievals = Retrievals.allEnrolments)(Future.failed(MissingBearerToken()))
        val result = testTwoWayMessageController.getEnquiryTypeDetails(scenario.enquiryType)(FakeRequest())
        await(result).header.status mustBe Status.UNAUTHORIZED
      }

      s"return 403 (FORBIDDEN) when AuthConnector doesn't return the correct enrolment for the given enquiry type: ${scenario.enquiryType}" in {
        val name = Name(Option("unknown"), Option("user"))
        mockAuthorise(retrievals = Retrievals.allEnrolments)(Future.successful(Enrolments(Set())))
        val result = testTwoWayMessageController.getEnquiryTypeDetails(scenario.enquiryType)(FakeRequest())
        await(result).header.status mustBe Status.FORBIDDEN
      }

      s"return 403 (FORBIDDEN) when getEnquiryTypeDetails is called with the incorrect queue id instead of ${scenario.enquiryType} for the enrolment" in {
        val name = Name(Option("unknown"), Option("user"))
        mockAuthorise(retrievals = Retrievals.allEnrolments)(Future.successful(Enrolments(scenario.enrolments)))
        val result = testTwoWayMessageController.getEnquiryTypeDetails(scenario.invalidEnquiryType)(FakeRequest())
        await(result).header.status mustBe Status.FORBIDDEN
      }
    })

    "return 404 when incorrect enquiryType is requested" in {
      val name = Name(Option("firstname"), Option("surename"))
      mockAuthorise(retrievals = Retrievals.allEnrolments)(
        Future.successful(Enrolments(Set(enrol("HMRC-NI", "nino", "AB123456C")))))
      val result = testTwoWayMessageController.getEnquiryTypeDetails("i am not valid")(FakeRequest())
      await(result).header.status mustBe Status.NOT_FOUND
    }
  }

}
