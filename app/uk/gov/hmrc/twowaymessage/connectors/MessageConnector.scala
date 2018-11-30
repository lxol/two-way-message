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

package uk.gov.hmrc.twowaymessage.connectors

import com.google.inject.Inject
import play.api.Logger
import play.api.http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.twowaymessage.model.{Message, MessageMetadata}
import uk.gov.hmrc.twowaymessage.model.CommonFormats._

import scala.concurrent.{ExecutionContext, Future}

class MessageConnector @Inject()(httpClient: HttpClient, servicesConfig: ServicesConfig)(implicit ec: ExecutionContext) extends Status {

  private val logger = Logger(this.getClass)
  val messageBaseUrl: String = servicesConfig.baseUrl("message")

  import uk.gov.hmrc.twowaymessage.model.MessageFormat._

  def postMessage(body: Message)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    httpClient.POST(s"$messageBaseUrl/messages", body)
  }

  def getMessageMetadata(replyTo: String)(implicit hc: HeaderCarrier): Future[MessageMetadata] = {
    import MessageMetadata._
    httpClient.GET[MessageMetadata](s"$messageBaseUrl/messages/${replyTo}/original")
  }
}
