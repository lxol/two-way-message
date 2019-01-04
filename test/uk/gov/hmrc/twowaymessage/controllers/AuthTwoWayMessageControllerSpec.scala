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
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.mvc.Results._
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, InsufficientEnrolments, MissingBearerToken}
import uk.gov.hmrc.twowaymessage.assets.TestUtil
import uk.gov.hmrc.twowaymessage.connector.mocks.MockAuthConnector
import uk.gov.hmrc.twowaymessage.model.TwoWayMessage
import uk.gov.hmrc.twowaymessage.services.TwoWayMessageService

import scala.concurrent.Future

class AuthTwoWayMessageControllerSpec extends TestUtil with MockAuthConnector {

  val mockMessageService = mock[TwoWayMessageService]

  override lazy val injector = new GuiceApplicationBuilder()
    .overrides(bind[TwoWayMessageService].to(mockMessageService))
    .overrides(bind[AuthConnector].to(mockAuthConnector))
    .injector()

  val testTwoWayMessageController = injector.instanceOf[TwoWayMessageController]

  val authPredicate: Predicate = EmptyPredicate

  val twoWayMessageGood = Json.parse(
    """
      |    {
      |      "recipient":{
      |        "taxIdentifier":{
      |          "name":"HMRC_ID",
      |          "value":"AB123456C"
      |        },
      |        "email":"someEmail@test.com"
      |      },
      |      "subject":"QUESTION",
      |      "content":"SGVsbG8gV29ybGQ="
      |    }""".stripMargin)

  val fakeRequest1 = FakeRequest(Helpers.POST, routes.TwoWayMessageController.createMessage("queueName").url, FakeHeaders(), twoWayMessageGood)

  "The TwoWayMessageController.createMessage method" when {

    "AuthConnector returns nino id " when  {

      "a message is successfully created in the message service, return 201 (Created)  " in {
        mockAuthorise(EmptyPredicate, Retrievals.nino)(Future.successful(Some("AB123456C")))
        when(mockMessageService.post(any[TwoWayMessage])).thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))
        val result = await(testTwoWayMessageController.createMessage("queueName")(fakeRequest1))
        status(result) shouldBe Status.CREATED
      }
    }

    "AuthConnector doesn't return nino id, returns 403(FORBIDDEN) " in  {
      mockAuthorise(EmptyPredicate, Retrievals.nino)(Future.successful(None))
      when(mockMessageService.post(any[TwoWayMessage])).thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))
      val result = await(testTwoWayMessageController.createMessage("queueName")(fakeRequest1))
      status(result) shouldBe Status.FORBIDDEN
    }

    "AuthConnector returns an exception that extends NoActiveSession, returns 401(UNAUTHORIZED) " in  {
      mockAuthorise(EmptyPredicate, Retrievals.nino)(Future.failed(MissingBearerToken()))
      when(mockMessageService.post(any[TwoWayMessage])).thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))
      val result = await(testTwoWayMessageController.createMessage("queueName")(fakeRequest1))
      status(result) shouldBe Status.UNAUTHORIZED
    }


    "AuthConnector returns an exception that doesn't extend NoActiveSession, returns 403(FORBIDDEN) " in  {
      mockAuthorise(EmptyPredicate, Retrievals.nino)(Future.failed(InsufficientEnrolments()))
      when(mockMessageService.post(any[TwoWayMessage])).thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))
      val result = await(testTwoWayMessageController.createMessage("queueName")(fakeRequest1))
      status(result) shouldBe Status.FORBIDDEN
    }

    SharedMetricRegistries.clear
  }
}
