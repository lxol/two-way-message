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

package uk.gov.hmrc.gform.dms

import java.time.Clock

import com.google.inject.{ AbstractModule, Provides }
import javax.inject.{ Named, Singleton }
import org.apache.pdfbox.pdmodel.PDDocument
import play.api.{ Configuration, Environment }
import uk.gov.hmrc.gform.fileupload.FUConfig
import uk.gov.hmrc.gform.sharedmodel.config.ContentType
import uk.gov.hmrc.gform.typeclasses.Rnd
import uk.gov.hmrc.gform.typeclasses.Rnd.RandomRnd
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.util.Random

class DmsModule(val environment: Environment, val configuration: Configuration) extends AbstractModule {

  @Provides
  @Singleton
  def pdfloader: Array[Byte] => PDDocument = PDDocument.load

  @Provides
  @Singleton
  @Named("dmsRandom")
  def rnd: Rnd[Random] = RandomRnd

  @Provides
  @Singleton
  @Named("dmsClock")
  def dmsClock: Clock = Clock.systemDefaultZone()

  @Provides
  @Singleton
  def fuConfig(servicesConfig: ServicesConfig): FUConfig = {
    val baseUrl = servicesConfig.baseUrl("file-upload")
    val pathPrefix = servicesConfig.getConfString("file-upload.path-prefix", "")
    val fileUploadBaseUrl = baseUrl + pathPrefix
    val formExpiryDays = configuration.get[Int]("formExpiryDays")
    formExpiryDays.verifyThat(_ > 0, s"'formExpiryDays' must be positive, was $formExpiryDays")
    val formMaxAttachments = configuration.get[Int]("formMaxAttachments")
    formMaxAttachments.verifyThat(_ > 0, s"'formMaxAttachments' must be positive, was $formMaxAttachments")
    val formMaxAttachmentSizeMB = configuration.get[Int]("formMaxAttachmentSizeMB")
    formMaxAttachmentSizeMB
      .verifyThat(_ > 0, s"'formMaxAttachmentSizeMB' must be positive, was $formMaxAttachmentSizeMB")
    val formMaxAttachmentTotalSizeMB = configuration.get[Int]("formMaxAttachmentTotalSizeMB")
    formMaxAttachmentTotalSizeMB
      .verifyThat(_ > 0, s"'formMaxAttachmentTotalSizeMB' must be positive, was $formMaxAttachmentTotalSizeMB")
    val contentTypesSeparatedByPipe = configuration.get[String]("contentTypesSeparatedByPipe")

    def contentTypes: List[ContentType] = contentTypesSeparatedByPipe.split('|').toList.map(ContentType.apply)

    contentTypes.length.verifyThat(_ > 0, s"'contentTypesSeparatedByPipe' is not set")

    FUConfig(
      fileUploadBaseUrl,
      servicesConfig.baseUrl("file-upload-frontend"),
      formExpiryDays,
      s"${formMaxAttachmentTotalSizeMB}MB", //heuristic to compute max size
      s"${formMaxAttachmentSizeMB}MB",
      formMaxAttachments,
      contentTypes
    )
  }

  private implicit class VerifyThat[T](t: T) {
    def verifyThat(assertion: T => Boolean, message: String = "") =
      if (!assertion(t)) throw new AppConfigException(message)
  }

  class AppConfigException(message: String) extends IllegalArgumentException(message)

}
