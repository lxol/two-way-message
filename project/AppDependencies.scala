// Copyright (C) 2011-2012 the original author or authors.
// See the LICENCE.txt file distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile = Seq(

    "org.typelevel" %% "cats-core" % "1.4.0",
    "org.apache.pdfbox" % "pdfbox" % "2.0.12",

    "com.codecommit" %% "parseback-core" % "0.3",
    "com.codecommit" %% "parseback-cats" % "0.3",

    "com.github.pureconfig" %% "pureconfig" % "0.9.2",

    "org.julienrf" %% "play-json-derived-codecs" % "3.3",
    "uk.gov.hmrc" %% "bootstrap-play-26" % "0.36.0",
    "uk.gov.hmrc" %% "play-health" % "3.8.0-play-26",
    "com.google.inject" % "guice" % "4.2.0",
    "com.kenshoo" %% "metrics-play" % "2.6.6_0.6.2",
    "com.typesafe.play" %% "play" % "2.6.20",
    "com.typesafe.play" %% "play-functional" % "2.6.10",
    "com.typesafe.play" %% "play-json" % "2.6.10",
    "com.typesafe.play" %% "play-json-joda" % "2.6.7",
    "uk.gov.hmrc" %% "domain"  % "5.3.0",
    "javax.inject" % "javax.inject" % "1",
    "com.typesafe.play" %% "twirl-api" % "1.3.15",
    "commons-codec" % "commons-codec" % "1.10",
    "uk.gov.hmrc" %% "auth-client" % "2.11.0-play-26"
  )

  val test = Set(
    "uk.gov.hmrc" %% "hmrctest" % "3.2.0" % "test,it",
    "uk.gov.hmrc" %% "service-integration-test" % "0.4.0-play-26" % "test,it",
    "org.scalatest" %% "scalatest" % "3.0.5" % "test",
    "com.typesafe.play" %% "play-test" % current % "test",
    "org.mockito" % "mockito-core" % "2.23.4" % "test",
    "org.pegdown" % "pegdown" % "1.6.0" % "test,it",
    "com.github.tomakehurst" % "wiremock-standalone" % "2.20.0" % "test,it",
    "org.scalacheck" %% "scalacheck" % "1.14.0" % "test,it",
    "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % "test,it",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % "test,it",
    "io.github.cloudify" %% "spdf" % "1.3.1" % "test"
  )

  val overrides = Set()

}
