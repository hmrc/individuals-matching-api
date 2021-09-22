/*
 * Copyright 2021 HM Revenue & Customs
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

package unit.uk.gov.hmrc.individualsmatchingapi.support

import com.typesafe.config.ConfigFactory
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Application, Configuration}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.PlayBodyParsers
import unit.uk.gov.hmrc.individualsmatchingapi.util.UnitSpec

trait SpecBase extends UnitSpec with GuiceOneAppPerSuite {

  lazy val additionalConfig: Configuration = Configuration()

  lazy val bodyParsers: PlayBodyParsers = fakeApplication.injector.instanceOf[PlayBodyParsers]

  def buildFakeApplication(extraConfig: Configuration): Application =
    new GuiceApplicationBuilder()
      .configure(
        Configuration(
          ConfigFactory.parseString("""
                                      | metrics.jvm = false
                                      | metrics.enabled = true
          """.stripMargin)
        ) withFallback extraConfig)
      .build()

  override lazy val fakeApplication: Application = buildFakeApplication(additionalConfig)

}
