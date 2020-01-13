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

package uk.gov.hmrc.twowaymessage.utils

import org.scalatest.WordSpec
import uk.gov.hmrc.play.test.UnitSpec

import scala.xml.Text

class XmlConversionSpec extends WordSpec with UnitSpec {

  "stringToXmlNodes" should {
    "return an empty Text node when passed an empty string" in {
      val result = XmlConversion.stringToXmlNodes("")
      result.get.head shouldBe Text("")
    }
    "return an empty text node when passed a string containing only a carriage return" in {
      val result = XmlConversion.stringToXmlNodes("\n")
      result.get.head shouldBe Text("")
    }
    "return a sequence containing one XML node when passed a valid XML string an single node" in {
      val result = XmlConversion.stringToXmlNodes("<test>test</test>")
      result.get shouldBe Seq(<test>test</test>)
    }
    "return a sequence of XML nodes when passed a valid XML string containing multiple nodes" in {
      val result = XmlConversion.stringToXmlNodes("<test>test1</test><test>test2</test>")
      result.get shouldBe <test>test1</test><test>test2</test>
    }
  }

}
