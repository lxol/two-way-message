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

package uk.gov.hmrc.gform.gformbackend

import javax.inject.{ Inject, Singleton }
import uk.gov.hmrc.gform.dms.DmsHtmlSubmission
import uk.gov.hmrc.gform.sharedmodel.form.EnvelopeId
import uk.gov.hmrc.gform.wshttp.GformWSHttp
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ ExecutionContext, Future }
import uk.gov.hmrc.http._
@Singleton
class GformConnector @Inject()(ws: GformWSHttp, servicesConfig: ServicesConfig) {
  lazy val baseUrl = servicesConfig.baseUrl("gform") + servicesConfig.getConfString("gform.path-prefix", "")

  implicit val uuidHttpReads: HttpReads[java.util.UUID] = new HttpReads[java.util.UUID] {
    def read(method: String, url: String, response: HttpResponse): java.util.UUID =
      java.util.UUID.fromString(response.body)
  }
  def submitToDmsViaGform(
    submission: DmsHtmlSubmission)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[java.util.UUID] =
    ws.POST[DmsHtmlSubmission, java.util.UUID](s"$baseUrl/dms/submit", submission)
}
