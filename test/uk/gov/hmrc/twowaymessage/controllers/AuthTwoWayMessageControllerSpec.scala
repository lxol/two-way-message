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

package uk.gov.hmrc.twowaymessage.controllers

import java.util.UUID

import com.codahale.metrics.SharedMetricRegistries
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.{~, Name, Retrievals}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.gform.dms.DmsMetadata
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.twowaymessage.assets.TestUtil
import uk.gov.hmrc.twowaymessage.connector.mocks.MockAuthConnector
import uk.gov.hmrc.twowaymessage.model.{TwoWayMessage, TwoWayMessageReply}
import uk.gov.hmrc.twowaymessage.services.RenderType.ReplyType
import uk.gov.hmrc.twowaymessage.services.TwoWayMessageService

import scala.concurrent.Future
import scala.concurrent.duration.Duration

class AuthTwoWayMessageControllerSpec extends TestUtil with MockAuthConnector {

  val mockMessageService = mock[TwoWayMessageService]

  override lazy val injector = new GuiceApplicationBuilder()
    .overrides(bind[TwoWayMessageService].to(mockMessageService))
    .overrides(bind[AuthConnector].to(mockAuthConnector))
    .injector()

  val testTwoWayMessageController = injector.instanceOf[TwoWayMessageController]

  val authPredicate: Predicate = EmptyPredicate

  val twoWayMessageGood = Json.parse("""
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
      mockAuthorise(Enrolment("HMRC-NI"), Retrievals.nino and Retrievals.name)(
        Future.successful(new ~(Some(nino.value), name)))

      when(
        mockMessageService
          .post(anyString, org.mockito.ArgumentMatchers.eq(nino), any[TwoWayMessage], any[DmsMetadata], any[Name])(
            any[HeaderCarrier]))
        .thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))
      val result = await(testTwoWayMessageController.createMessage("p800")(fakeRequest1))
      status(result) shouldBe Status.CREATED
    }

    "return 403 (FORBIDDEN) when AuthConnector doesn't return a Nino" in {
      val name = Name(Option("unknown"), Option("user"))
      mockAuthorise(Enrolment("HMRC-NI"), Retrievals.nino and Retrievals.name)(Future.successful(new ~(None, name)))
      val result = await(testTwoWayMessageController.createMessage("p800")(fakeRequest1))
      status(result) shouldBe Status.FORBIDDEN
    }

    "return 403 (FORBIDDEN) when createMessage is presented with an invalid queue id" in {
      val name = Name(Option("unknown"), Option("user"))
      mockAuthorise(Enrolment("HMRC-NI"), Retrievals.nino and Retrievals.name)(Future.successful(new ~(None, name)))
      val result = await(testTwoWayMessageController.createMessage("other-queue-id")(fakeRequest1))
      status(result) shouldBe Status.FORBIDDEN
    }

    "return 401 (UNAUTHORIZED) when AuthConnector returns an exception that extends NoActiveSession" in {
      mockAuthorise(Enrolment("HMRC-NI"), Retrievals.nino and Retrievals.name)(Future.failed(MissingBearerToken()))
      val result = await(testTwoWayMessageController.createMessage("p800")(fakeRequest1))
      status(result) shouldBe Status.UNAUTHORIZED
    }

    "return 403 (FORBIDDEN) when AuthConnector returns an exception that doesn't extend NoActiveSession" in {
      mockAuthorise(Enrolment("HMRC-NI"), Retrievals.nino and Retrievals.name)(Future.failed(InsufficientEnrolments()))
      val result = await(testTwoWayMessageController.createMessage("p800")(fakeRequest1))
      status(result) shouldBe Status.FORBIDDEN
    }

    SharedMetricRegistries.clear
  }

  "The TwoWayMessageController.createCustomerResponse method" should {

    "return 201 (CREATED) when a message is successfully created by the message service with a valid Nino" in {
      val nino = Nino("AB123456C")
      mockAuthorise(Enrolment("HMRC-NI"))(Future.successful(Some(nino.value)))
      when(
        mockMessageService.postCustomerReply(any[TwoWayMessageReply], ArgumentMatchers.eq("replyTo"))(
          any[HeaderCarrier]))
        .thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))
      val result = await(testTwoWayMessageController.createCustomerResponse("p800", "replyTo")(fakeRequest1))
      status(result) shouldBe Status.CREATED
    }

    "return 401 (UNAUTHORIZED) when AuthConnector returns an exception that extends NoActiveSession" in {
      mockAuthorise(Enrolment("HMRC-NI"))(Future.failed(MissingBearerToken()))
      val result = await(testTwoWayMessageController.createCustomerResponse("p800", "replyTo")(fakeRequest1))
      status(result) shouldBe Status.UNAUTHORIZED
    }

    "return 403 (FORBIDDEN) when AuthConnector returns an exception that doesn't extend NoActiveSession" in {
      mockAuthorise(Enrolment("HMRC-NI"))(Future.failed(InsufficientEnrolments()))
      val result: Result = await(testTwoWayMessageController.createCustomerResponse("queueName", "replyTo")(fakeRequest1))
      status(result) shouldBe Status.FORBIDDEN
    }

    SharedMetricRegistries.clear
  }


  "The TwoWayMessageController.getContentBy method" should {
    "return 200 (OK) when the message type is valid" in {
      val nino = Nino("AB123456C")
      mockAuthorise(Enrolment("HMRC-NI") or AuthProviders(PrivilegedApplication))(Future.successful(Some(nino.value)))
      when(
        mockMessageService.findMessagesBy(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(List())))
      val result = await(testTwoWayMessageController.getContentBy("1", "Customer")(fakeRequest1).run())(Duration.Inf)
      status(result) shouldBe Status.OK
    }

    "return 400 (BAD_REQUEST) when the message type is invalid" in {
      val nino = Nino("AB123456C")
      mockAuthorise(Enrolment("HMRC-NI") or AuthProviders(PrivilegedApplication))(Future.successful(Some(nino.value)))
      val result = await(testTwoWayMessageController.getContentBy("1", "nfejwk")(fakeRequest1).run() )
      status(result) shouldBe Status.BAD_REQUEST
    }


    SharedMetricRegistries.clear
  }

}
