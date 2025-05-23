/*
 * Copyright 2023 HM Revenue & Customs
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
import org.apache.pekko.stream.Materializer
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.http.HttpErrorHandler
import play.api.libs.json.JsValue
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, contentAsString, defaultAwaitTimeout, status}
import uk.gov.hmrc.individualsmatchingapi.controllers.DocumentationController
import unit.uk.gov.hmrc.individualsmatchingapi.support.SpecBase

import java.nio.file.{Files, Paths}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DocumentationControllerSpec extends SpecBase with Matchers with MockitoSugar {
  implicit lazy val materializer: Materializer = app.materializer

  trait Setup {
    val configuration: Configuration = mock[Configuration]
    val HttpErrorHandler: HttpErrorHandler = mock[HttpErrorHandler]

    val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    val controllerComponents: ControllerComponents =
      app.injector.instanceOf[ControllerComponents]
    val assets: Assets =
      app.injector.instanceOf[Assets]

    val underTest =
      new DocumentationController(controllerComponents, assets, configuration)

    when(configuration.getOptional[Seq[String]]("api.access.version-P1.0.whitelistedApplicationIds")).thenReturn(None)
    when(configuration.getOptional[Seq[String]]("api.access.version-1.0.whitelistedApplicationIds")).thenReturn(None)
    when(configuration.getOptional[String]("api.access.version-1.0.accessType")).thenReturn(None)
    when(configuration.getOptional[Seq[String]]("api.access.version-2.0.whitelistedApplicationIds")).thenReturn(None)
    when(configuration.getOptional[String]("api.access.version-2.0.status")).thenReturn(None)
    when(configuration.getOptional[Boolean]("api.access.version-2.0.endpointsEnabled")).thenReturn(None)
  }

  "/api/definition" should {
    "return 1.0 as PRIVATE when api.access.version-1.0.accessType is not set" in new Setup {
      when(configuration.getOptional[String]("api.access.version-1.0.accessType")).thenReturn(None)

      val result: Future[Result] = underTest.definition()(request)

      (apiVersion(result, "1.0") \ "access" \ "type")
        .as[String] shouldBe "PRIVATE"
    }

    "return 1.0 as PRIVATE when api.access.version-1.0.accessType is set to PRIVATE" in new Setup {
      when(configuration.getOptional[String]("api.access.version-1.0.accessType")).thenReturn(Some("PRIVATE"))

      val result: Future[Result] = underTest.definition()(request)

      (apiVersion(result, "1.0") \ "access" \ "type")
        .as[String] shouldBe "PRIVATE"
    }

    "return 1.0 as PUBLIC when api.access.version-1.0.accessType is set to PUBLIC" in new Setup {
      when(configuration.getOptional[String]("api.access.version-1.0.accessType")).thenReturn(Some("PUBLIC"))

      val result: Future[Result] = underTest.definition()(request)

      (apiVersion(result, "1.0") \ "access" \ "type")
        .as[String] shouldBe "PUBLIC"
    }

    "return 2.0 as BETA when api.access.version-2.0.status is not set" in new Setup {

      val result: Future[Result] = underTest.definition()(request)

      (apiVersion(result, "2.0") \ "status")
        .as[String] shouldBe "BETA"
    }

    "return 2.0 as ALPHA when api.access.version-2.0.status is set" in new Setup {
      when(configuration.getOptional[String]("api.access.version-2.0.status")).thenReturn(Some("ALPHA"))

      val result: Future[Result] = underTest.definition()(request)

      (apiVersion(result, "2.0") \ "status")
        .as[String] shouldBe "ALPHA"
    }

    "return endpoints enabled true when api.access.version-2.0.endpointsEnabled is not set" in new Setup {

      val result: Future[Result] = underTest.definition()(request)

      (apiVersion(result, "2.0") \ "endpointsEnabled")
        .as[Boolean] shouldBe true
    }

    "return endpoints enabled false when api.access.version-2.0.endpointsEnabled is set" in new Setup {

      when(configuration.getOptional[Boolean]("api.access.version-2.0.endpointsEnabled")).thenReturn(Some(false))

      val result: Future[Result] = underTest.definition()(request)

      (apiVersion(result, "2.0") \ "endpointsEnabled")
        .as[Boolean] shouldBe false
    }
  }

  "/api/documentation/version?/individuals/matching" should {
    "should return 200 and return file contents" in new Setup {
      private val versions = List("1.0", "2.0", "P1.0")
      for (version <- versions) {
        val docString = Files.readString(Paths.get(s"resources/public/api/conf/$version/application.yaml"))
        val result = underTest.specification(version, "application.yaml")(request)
        status(result) shouldBe 200
        contentAsString(result) shouldBe docString
      }
    }
  }

  private def apiVersion(result: Future[Result], version: String) =
    (contentAsJson(result) \ "api" \ "versions")
      .as[Seq[JsValue]]
      .find { v =>
        (v \ "version").as[String] == version
      }
      .getOrElse(fail(s"api version $version is not in the definition"))
}
