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

package uk.gov.hmrc.gform.wshttp

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.config.Config
import javax.inject.{ Inject, Singleton }
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData.FilePart
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.{ HttpHook, HttpHooks }
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.http.ws._

import scala.concurrent.Future
trait HttpClient extends HttpGet with HttpPut with HttpPost with HttpDelete with HttpPatch

trait Hooks extends HttpHooks {
  override val hooks = Seq()
}
@Singleton
class GformWSHttp @Inject()(
  config: Configuration,
  val httpAuditing: HttpAuditing,
  val wsClient: WSClient,
  override protected val actorSystem: ActorSystem)
    extends HttpClient with WSHttp {

  override lazy val configuration: Option[Config] = Option(config.underlying)

  override val hooks: Seq[HttpHook] = Seq(httpAuditing.AuditingHook)

  def POSTFile[O](
    url: String,
    fileName: String,
    body: ByteString,
    headers: Seq[(String, String)],
    contentType: String //TODO: change type to ContentType
  )(
    implicit
    hc: HeaderCarrier,
    rds: HttpReads[O]): Future[HttpResponse] = {

    val source: Source[FilePart[Source[ByteString, NotUsed]], NotUsed] = Source(
      FilePart(fileName, fileName, Some(contentType), Source.single(body)) :: Nil)
    buildRequest(url).addHttpHeaders(headers: _*).post(source).map(new WSHttpResponse(_))

  }
}
