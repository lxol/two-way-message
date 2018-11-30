/*
 * Copyright 2018 HM Revenue & Customs
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
import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.test.Helpers._
import uk.gov.hmrc.twowaymessage.model.{TwoWayMessage, TwoWayMessageReply}
import uk.gov.hmrc.twowaymessage.services.TwoWayMessageService

import scala.concurrent.Future

class TwoWayMessageControllerSpec extends WordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar {

  val mockMessageService = mock[TwoWayMessageService]

  val injector = new GuiceApplicationBuilder()
    .overrides(bind[TwoWayMessageService].to(mockMessageService))
    .injector()

  val controller = injector.instanceOf[TwoWayMessageController]

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

  val twoWayMessageBadIdentifier = Json.parse(
    """
      |       {
      |        "recipient":{
      |        "taxIdentifier":{
      |          "name":"HMRC_ID",
      |          "value":"abc"
      |        },
      |        "email":"someEmail@test.com"
      |      },
      |      "subject":"QUESTION",
      |      "content":"SGVsbG8gV29ybGQ="
      |    }""".stripMargin)

  val twoWayMessageBadContent = Json.parse(
    """
      |    {
      |     "subject":"QUESTION"
      |    }""".stripMargin)

  val twoWayMessageReplyGood = Json.parse(
    """
      |    {
      |     "subject":"answer",
      |     "content":"Some base64-encoded HTML"
      |    }""".stripMargin)

  "TwoWayMessageController" should {

    "return 201 (Created) when a message is successfully created in the message service " in {
      when(mockMessageService.post(any[TwoWayMessage])).thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))
      val result = await(controller.validateAndPostMessage(twoWayMessageGood))
      result.header.status shouldBe Status.CREATED
    }

    "return 400 (Bad Request) if the tax identifier is not supported " in {
      val result = await(controller.validateAndPostMessage(twoWayMessageBadIdentifier))
      result.header.status shouldBe Status.BAD_REQUEST
    }

    "return 201 (Created) when a reply is successfully created in the message service " in {
      when(mockMessageService.postReply(any[TwoWayMessageReply], any[String]))
        .thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))
      val result = await(controller.validateAndPostAdvisorResponse(twoWayMessageReplyGood, "replyToId"))
      result.header.status shouldBe Status.CREATED
    }

    "return 400 (Bad Request) if the body is not as per the definition " in {
      val result = await(controller.validateAndPostAdvisorResponse(twoWayMessageBadContent, "replyToId"))
      result.header.status shouldBe Status.BAD_REQUEST
    }

    SharedMetricRegistries.clear
  }
}
