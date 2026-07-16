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

package unit.uk.gov.hmrc.individualsmatchingapi.controllers.v1

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verifyNoInteractions, when}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json.parse
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents, RequestHeader, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments, InsufficientEnrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsmatchingapi.audit.AuditHelper
import uk.gov.hmrc.individualsmatchingapi.config.AppConfig
import uk.gov.hmrc.individualsmatchingapi.controllers.InternalAuthHelper
import uk.gov.hmrc.individualsmatchingapi.controllers.v1.live.LivePrivilegedCitizenMatchingController
import uk.gov.hmrc.individualsmatchingapi.controllers.v1.sandbox.SandboxPrivilegedCitizenMatchingController
import uk.gov.hmrc.individualsmatchingapi.domain.*
import uk.gov.hmrc.individualsmatchingapi.domain.SandboxData.sandboxMatchId
import uk.gov.hmrc.individualsmatchingapi.services.{LiveCitizenMatchingService, SandboxCitizenMatchingService, ScopesService}
import uk.gov.hmrc.internalauth.client.BackendAuthComponents
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}
import unit.uk.gov.hmrc.individualsmatchingapi.controllers.v2.ScopesConfigHelper
import unit.uk.gov.hmrc.individualsmatchingapi.support.SpecBase

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class PrivilegedCitizenMatchingControllerSpec extends SpecBase with Matchers with MockitoSugar {

  // noinspection ForwardReference
  trait Setup extends ScopesConfigHelper {
    val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    val sampleCorrelationId = "188e9400-b636-4a3b-80ba-230a8c72b92a"
    val controllerComponents: ControllerComponents =
      app.injector.instanceOf[ControllerComponents]
    implicit val ec: ExecutionContext = ExecutionContext.global
    val appLocal: Application = new GuiceApplicationBuilder()
      .configure("localEnv" -> true)
      .build()
    lazy val appConfigLocal: AppConfig = appLocal.injector.instanceOf[AppConfig]
    lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
    val mockScopesService = new ScopesService(mockScopesConfig)

    val sandboxCitizenMatchingService = new SandboxCitizenMatchingService
    val mockLiveCitizenMatchingService: LiveCitizenMatchingService = mock[LiveCitizenMatchingService]
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockInternalAuthBehaviour: StubBehaviour = mock[StubBehaviour]
    given ControllerComponents = stubControllerComponents()
    val backendAuthComponents: BackendAuthComponents = BackendAuthComponentsStub(mockInternalAuthBehaviour)
    val internalAuthHelper = new InternalAuthHelper(
      backendAuthComponents,
      Configuration(InternalAuthHelper.InternalAuthFeatureFlag -> true)
    )
    val mockAuditHelper: AuditHelper = mock[AuditHelper]
    implicit val auditHelper: AuditHelper = mockAuditHelper

    val sandboxController = new SandboxPrivilegedCitizenMatchingController(
      sandboxCitizenMatchingService,
      mockAuthConnector,
      internalAuthHelper,
      controllerComponents,
      mockScopesService
    )(using ec, auditHelper, appConfigLocal)

    val liveController = new LivePrivilegedCitizenMatchingController(
      mockLiveCitizenMatchingService,
      mockAuthConnector,
      internalAuthHelper,
      controllerComponents,
      mockScopesService
    )(using ec, auditHelper, appConfig)

    when(
      mockAuthConnector
        .authorise(any(), eqTo(Retrievals.allEnrolments))(using any(), any())
    ).thenReturn(Future.successful(Enrolments(Set(Enrolment("test-scope")))))
  }

  "live matching citizen controller" should {

    val matchId = UUID.randomUUID()

    "return 200 (Ok) for a matched citizen" in new Setup {
      when(
        mockLiveCitizenMatchingService
          .matchCitizen(any[CitizenMatchingRequest])(using any[HeaderCarrier], any[RequestHeader])
      ).thenReturn(Future.successful(matchId))

      val eventualResult: Future[Result] = liveController.matchCitizen()(
        fakeRequest.withBody(parse(matchingRequest())).withHeaders(("CorrelationId", sampleCorrelationId))
      )

      status(eventualResult) mustBe OK
      contentAsJson(eventualResult) mustBe parse(
        s"""
           {
             "_links": {
               "individual": {
                 "href": "/individuals/matching/$matchId",
                 "name": "GET",
                 "title": "Individual Details"
               },
               "self": {
                 "href": "/individuals/matching/"
               }
             }
           }"""
      )
    }

    "return 200 Ok when matching a user with a '.' in their name" in new Setup {

      when(
        mockLiveCitizenMatchingService.matchCitizen(any())(using any(), any())
      ).thenReturn(Future.successful(matchId))

      val payload: JsObject = Json.obj(
        "firstName"   -> "Mr.",
        "lastName"    -> "St. John",
        "nino"        -> "AA112233B",
        "dateOfBirth" -> "1900-01-01"
      )

      val res: Future[Result] =
        liveController.matchCitizen()(fakeRequest.withHeaders("CorrelationId" -> sampleCorrelationId).withBody(payload))
      status(res) mustBe OK
    }

    "return JsError when mandatory field missing" in new Setup {
      when(
        mockLiveCitizenMatchingService.matchCitizen(any())(using any(), any())
      ).thenReturn(Future.successful(matchId))

      val payload: JsObject = Json.obj(
        "firstName"   -> 1,
        "lastName"    -> "St. John",
        "nino"        -> "AA112233B",
        "dateOfBirth" -> "1900-01-01"
      )

      val res: Future[Result] =
        liveController.matchCitizen()(fakeRequest.withHeaders("CorrelationId" -> sampleCorrelationId).withBody(payload))

      status(res) mustBe 400
    }

    "return 403 (Forbidden) for a citizen not found" in new Setup {
      when(
        mockLiveCitizenMatchingService
          .matchCitizen(any[CitizenMatchingRequest])(using any[HeaderCarrier], any[RequestHeader])
      ).thenReturn(Future.failed(new CitizenNotFoundException))

      val eventualResult: Future[Result] = liveController.matchCitizen()(
        fakeRequest.withBody(parse(matchingRequest())).withHeaders(("CorrelationId", sampleCorrelationId))
      )

      status(eventualResult) mustBe FORBIDDEN
      contentAsJson(eventualResult) mustBe Json.obj(
        "code"    -> "MATCHING_FAILED",
        "message" -> "There is no match for the information provided"
      )
    }

    "return 403 (Forbidden) when a matching exception is thrown" in new Setup {
      when(
        mockLiveCitizenMatchingService
          .matchCitizen(any[CitizenMatchingRequest])(using any[HeaderCarrier], any[RequestHeader])
      ).thenReturn(Future.failed(new MatchingException))

      val eventualResult: Future[Result] = liveController.matchCitizen()(
        fakeRequest.withBody(parse(matchingRequest())).withHeaders(("CorrelationId", sampleCorrelationId))
      )

      status(eventualResult) mustBe FORBIDDEN
      contentAsJson(eventualResult) mustBe Json.obj(
        "code"    -> "MATCHING_FAILED",
        "message" -> "There is no match for the information provided"
      )
    }

    "return 403 (Forbidden) when an invalid nino exception is thrown" in new Setup {
      when(
        mockLiveCitizenMatchingService
          .matchCitizen(any[CitizenMatchingRequest])(using any[HeaderCarrier], any[RequestHeader])
      ).thenReturn(Future.failed(new InvalidNinoException()))

      val eventualResult: Future[Result] = liveController.matchCitizen()(
        fakeRequest.withBody(parse(matchingRequest())).withHeaders(("CorrelationId", sampleCorrelationId))
      )

      status(eventualResult) mustBe FORBIDDEN
      contentAsJson(eventualResult) mustBe Json.obj(
        "code"    -> "MATCHING_FAILED",
        "message" -> "There is no match for the information provided"
      )
    }

    "return 400 (BadRequest) for an invalid dateOfBirth" in new Setup {
      val requestBody: JsValue =
        parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"NA000799C","dateOfBirth":"2020-01-32"}""")
      val eventualResult: Future[Result] =
        liveController.matchCitizen()(fakeRequest.withBody(requestBody))

      status(eventualResult) mustBe BAD_REQUEST
      contentAsJson(eventualResult) mustBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "dateOfBirth: invalid date format"
      )

      val requestBody1 =
        parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"NA000799C","dateOfBirth":"20200131"}""")
      val eventualResult1 = liveController.matchCitizen()(fakeRequest.withBody(requestBody1))

      status(eventualResult1) mustBe BAD_REQUEST
      contentAsJson(eventualResult1) mustBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "dateOfBirth: invalid date format"
      )
    }

    "return 400 (BadRequest) for an invalid nino" in new Setup {
      val requestBody: JsValue =
        parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"AB1234567","dateOfBirth":"2020-01-31"}""")
      val eventualResult: Future[Result] =
        liveController.matchCitizen()(fakeRequest.withBody(requestBody))

      status(eventualResult) mustBe BAD_REQUEST
      contentAsJson(eventualResult) mustBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "Malformed nino submitted"
      )
    }

    "return 400 Bad Request when the first name is empty" in new Setup {
      val emptyFirstName: JsObject = Json.obj(
        "firstName"   -> "",
        "lastName"    -> "Person",
        "nino"        -> "AA112233B",
        "dateOfBirth" -> "1900-01-01"
      )

      val res: Future[Result] =
        liveController.matchCitizen()(fakeRequest.withBody(emptyFirstName))

      status(res) mustBe BAD_REQUEST
      contentAsJson(res) mustBe Json.obj("code" -> "INVALID_REQUEST", "message" -> "firstName is required")
    }

    "return 400 Bad Request when the last name is empty" in new Setup {
      val emptyLastName: JsObject = Json.obj(
        "firstName"   -> "Mr",
        "lastName"    -> "",
        "nino"        -> "AA112233B",
        "dateOfBirth" -> "1900-01-01"
      )

      val res: Future[Result] =
        liveController.matchCitizen()(fakeRequest.withBody(emptyLastName))

      status(res) mustBe BAD_REQUEST
      contentAsJson(res) mustBe Json.obj("code" -> "INVALID_REQUEST", "message" -> "lastName is required")
    }

    "return 400 Bad Request when the first name is greater than 35 characters" in new Setup {
      val firstNameTooLong: JsObject = Json.obj(
        "firstName"   -> Random.nextString(36),
        "lastName"    -> "Person",
        "nino"        -> "AA112233B",
        "dateOfBirth" -> "1900-01-01"
      )

      val res: Future[Result] =
        liveController.matchCitizen()(fakeRequest.withBody(firstNameTooLong))

      status(res) mustBe BAD_REQUEST
      contentAsJson(res) mustBe Json
        .obj("code" -> "INVALID_REQUEST", "message" -> "firstName must be no more than 35 characters")
    }

    "return 400 Bad Request when the last name is greater than 35 characters" in new Setup {
      val lastNameTooLong: JsObject = Json.obj(
        "firstName"   -> "Mr",
        "lastName"    -> Random.nextString(36),
        "nino"        -> "AA112233B",
        "dateOfBirth" -> "1900-01-01"
      )

      val res: Future[Result] =
        liveController.matchCitizen()(fakeRequest.withBody(lastNameTooLong))

      status(res) mustBe BAD_REQUEST
      contentAsJson(res) mustBe Json
        .obj("code" -> "INVALID_REQUEST", "message" -> "lastName must be no more than 35 characters")
    }

    "return 400 Bad Request when the first name contains invalid characters" in new Setup {
      val invalidFirstName: JsObject = Json.obj(
        "firstName"   -> """/\/\/\/\""",
        "lastName"    -> "Person",
        "nino"        -> "AA112233B",
        "dateOfBirth" -> "1900-01-01"
      )

      val res: Future[Result] =
        liveController.matchCitizen()(fakeRequest.withBody(invalidFirstName))

      status(res) mustBe BAD_REQUEST
      contentAsJson(res) mustBe Json
        .obj("code" -> "INVALID_REQUEST", "message" -> "firstName contains invalid characters")
    }

    "return 400 Bad Request when the last name contains invalid characters" in new Setup {
      val invalidFirstName: JsObject = Json.obj(
        "firstName"   -> "Mr",
        "lastName"    -> """/\/\/\/\""",
        "nino"        -> "AA112233B",
        "dateOfBirth" -> "1900-01-01"
      )

      val res: Future[Result] =
        liveController.matchCitizen()(fakeRequest.withBody(invalidFirstName))

      status(res) mustBe BAD_REQUEST
      contentAsJson(res) mustBe Json
        .obj("code" -> "INVALID_REQUEST", "message" -> "lastName contains invalid characters")
    }

    "fail with UnauthorizedException when the bearer token does not have enrolment read:individuals-matching" in new Setup {
      val requestBody: JsValue =
        parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"NA000799C","dateOfBirth":"2020-01-31"}""")

      when(
        mockAuthConnector.authorise(any(), any())(using any(), any())
      ).thenReturn(Future.failed(InsufficientEnrolments()))

      intercept[InsufficientEnrolments] {
        await(
          liveController.matchCitizen()(
            fakeRequest
              .withHeaders("CorrelationId" -> sampleCorrelationId)
              .withBody(requestBody)
          )
        )
      }

      verifyNoInteractions(mockLiveCitizenMatchingService)
    }
  }

  "Sandbox match citizen function" should {

    "return 200 (Ok) for the sandbox matchId" in new Setup {
      val eventualResult: Future[Result] =
        sandboxController.matchCitizen()(
          fakeRequest.withBody(parse(matchingRequest())).withHeaders(("CorrelationId", sampleCorrelationId))
        )

      status(eventualResult) mustBe OK
      contentAsJson(eventualResult) mustBe parse(
        s"""
           {
             "_links": {
               "individual": {
                 "href": "/individuals/matching/$sandboxMatchId",
                 "name": "GET",
                 "title": "Individual Details"
               },
               "self": {
                 "href": "/individuals/matching/"
               }
             }
           }"""
      )
    }

    "return 403 (Forbidden) for a citizen not found" in new Setup {
      val eventualResult: Future[Result] =
        sandboxController.matchCitizen()(
          fakeRequest
            .withHeaders(("CorrelationId", sampleCorrelationId))
            .withBody(parse(matchingRequest(firstName = "José")))
        )

      status(eventualResult) mustBe FORBIDDEN
      contentAsJson(eventualResult) mustBe Json.obj(
        "code"    -> "MATCHING_FAILED",
        "message" -> "There is no match for the information provided"
      )
    }

    "return 403 (Forbidden) when nino does not match a sandbox individual" in new Setup {
      val eventualResult: Future[Result] =
        sandboxController.matchCitizen()(
          fakeRequest
            .withHeaders(("CorrelationId", sampleCorrelationId))
            .withBody(parse(matchingRequest(nino = "AA000799C")))
        )

      status(eventualResult) mustBe FORBIDDEN
      contentAsJson(eventualResult) mustBe Json.obj(
        "code"    -> "MATCHING_FAILED",
        "message" -> "There is no match for the information provided"
      )
    }

    "return 403 (Forbidden) when an invalid nino exception is thrown" in new Setup {
      val eventualResult: Future[Result] =
        sandboxController.matchCitizen()(
          fakeRequest
            .withHeaders(("CorrelationId", sampleCorrelationId))
            .withBody(parse(matchingRequest(nino = "NA000799D")))
        )

      status(eventualResult) mustBe FORBIDDEN
      contentAsJson(eventualResult) mustBe Json.obj(
        "code"    -> "MATCHING_FAILED",
        "message" -> "There is no match for the information provided"
      )
    }

    "return 400 (BadRequest) for an invalid dateOfBirth" in new Setup {
      val requestBody: JsValue =
        parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"NA000799C","dateOfBirth":"2020-01-32"}""")
      val eventualResult: Future[Result] =
        sandboxController.matchCitizen()(
          fakeRequest.withHeaders(("CorrelationId", sampleCorrelationId)).withBody(requestBody)
        )

      status(eventualResult) mustBe BAD_REQUEST
      contentAsJson(eventualResult) mustBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "dateOfBirth: invalid date format"
      )

      val requestBody1: JsValue =
        parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"NA000799C","dateOfBirth":"20200131"}""")
      val eventualResult1: Future[Result] = sandboxController.matchCitizen()(
        fakeRequest.withHeaders(("CorrelationId", sampleCorrelationId)).withBody(requestBody1)
      )

      status(eventualResult1) mustBe BAD_REQUEST
      contentAsJson(eventualResult1) mustBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "dateOfBirth: invalid date format"
      )
    }

    "return 400 (BadRequest) for an invalid nino" in new Setup {
      val requestBody: JsValue =
        parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"AB1234567","dateOfBirth":"2020-01-31"}""")
      val eventualResult: Future[Result] =
        sandboxController.matchCitizen()(
          fakeRequest.withHeaders(("CorrelationId", sampleCorrelationId)).withBody(requestBody)
        )

      status(eventualResult) mustBe BAD_REQUEST
      contentAsJson(eventualResult) mustBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "Malformed nino submitted"
      )
    }

    "not require bearer token authentication" in new Setup {
      val eventualResult: Future[Result] =
        sandboxController.matchCitizen()(
          fakeRequest.withHeaders(("CorrelationId", sampleCorrelationId)).withBody(parse(matchingRequest()))
        )

      status(eventualResult) mustBe OK
      verifyNoInteractions(mockAuthConnector)
    }
  }

  private def matchingRequest(
    firstName: String = "Amanda",
    dateOfBirth: String = "1960-01-15",
    nino: String = "NA000799C"
  ) =
    s"""{
          "firstName":"$firstName",
          "lastName":"Joseph",
          "nino":"$nino",
          "dateOfBirth":"$dateOfBirth"
        }
      """
}
