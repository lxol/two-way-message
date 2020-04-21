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

package uk.gov.hmrc.twowaymessage.services

object PdfTestUtil {

  def generatePdfFromHtml(html: String, inputFileName: String): Unit = {

    import java.io._

    import io.github.cloudify.scala.spdf._

    val pdf: Pdf = Pdf(
      "/usr/local/bin/wkhtmltopdf",
      new PdfConfig {
        orientation := Portrait
        pageSize := "A4"
        marginTop := "1in"
        marginBottom := "1in"
        marginLeft := "1in"
        marginRight := "1in"
        disableExternalLinks := true
        disableInternalLinks := true
      }
    )

    val outputFile: File = new File(inputFileName)
    pdf.run(html, outputFile)

  }

}
