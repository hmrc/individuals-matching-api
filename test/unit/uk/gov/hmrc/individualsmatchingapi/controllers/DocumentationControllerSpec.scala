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

package unit.uk.gov.hmrc.individualsmatchingapi.controllers

import controllers.Assets
import org.mockito.BDDMockito.given
import org.mockito.Mockito.when
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar
import play.api.Configuration
import play.api.http.HttpErrorHandler
import play.api.libs.json.JsValue
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.individualsmatchingapi.controllers.DocumentationController
import unit.uk.gov.hmrc.individualsmatchingapi.support.SpecBase

class DocumentationControllerSpec
    extends SpecBase
    with Matchers
    with MockitoSugar {

  implicit lazy val materializer = fakeApplication.materializer

  trait Setup {
    val configuration = mock[Configuration]
    val HttpErrorHandler = mock[HttpErrorHandler]

    val request = FakeRequest()
    val controllerComponents =
      fakeApplication.injector.instanceOf[ControllerComponents]
    val assets =
      fakeApplication.injector.instanceOf[Assets]

    val underTest =
      new DocumentationController(controllerComponents,
                                  assets,
                                  HttpErrorHandler,
                                  configuration)

    when(
      configuration.getOptional[Seq[String]](
        "api.access.version-P1.0.whitelistedApplicationIds")).thenReturn(None)
    when(
      configuration.getOptional[Seq[String]](
        "api.access.version-1.0.whitelistedApplicationIds")).thenReturn(None)
    when(configuration.getOptional[String]("api.access.version-1.0.accessType"))
      .thenReturn(None)
  }

  "/api/definition" should {
    "return 1.0 as PRIVATE when api.access.version-1.0.accessType is not set" in new Setup {
      given(
        configuration.getOptional[String]("api.access.version-1.0.accessType"))
        .willReturn(None)

      val result = await(underTest.definition()(request))

      (apiVersion(result, "1.0") \ "access" \ "type")
        .as[String] shouldBe "PRIVATE"
    }

    "return 1.0 as PRIVATE when api.access.version-1.0.accessType is set to PRIVATE" in new Setup {
      given(
        configuration.getOptional[String]("api.access.version-1.0.accessType"))
        .willReturn(Some("PRIVATE"))

      val result = await(underTest.definition()(request))

      (apiVersion(result, "1.0") \ "access" \ "type")
        .as[String] shouldBe "PRIVATE"
    }

    "return 1.0 as PUBLIC when api.access.version-1.0.accessType is set to PUBLIC" in new Setup {
      given(
        configuration.getOptional[String]("api.access.version-1.0.accessType"))
        .willReturn(Some("PUBLIC"))

      val result = await(underTest.definition()(request))

      (apiVersion(result, "1.0") \ "access" \ "type")
        .as[String] shouldBe "PUBLIC"
    }

    "return whitelisted applications from the configuration" in new Setup {
      when(
        configuration.getOptional[Seq[String]](
          "api.access.version-P1.0.whitelistedApplicationIds"))
        .thenReturn(Some(Seq("appVP1")))
      when(
        configuration.getOptional[Seq[String]](
          "api.access.version-1.0.whitelistedApplicationIds"))
        .thenReturn(Some(Seq("appV1")))

      val result = await(underTest.definition()(request))

      (apiVersion(result, "1.0") \ "access" \ "whitelistedApplicationIds")
        .as[Seq[String]] shouldBe Seq("appV1")
      (apiVersion(result, "P1.0") \ "access" \ "whitelistedApplicationIds")
        .as[Seq[String]] shouldBe Seq("appVP1")
    }
  }

  private def apiVersion(result: Result, version: String) =
    (jsonBodyOf(result) \ "api" \ "versions")
      .as[Seq[JsValue]]
      .find { v =>
        (v \ "version").as[String] == version
      }
      .getOrElse(fail(s"api version $version is not in the definition"))
}
