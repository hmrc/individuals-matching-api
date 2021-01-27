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

package unit.uk.gov.hmrc.individualsmatchingapi.controllers.v2

import java.util.UUID
import org.mockito.BDDMockito.given
import org.mockito.Matchers.{any, refEq}
import org.mockito.Mockito.{verifyZeroInteractions, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, MustMatchers}
import play.api.libs.json.Json
import play.api.libs.json.Json.parse
import play.api.mvc.{ControllerComponents, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, _}
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments, InsufficientEnrolments}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.individualsmatchingapi.controllers.v2.{LivePrivilegedCitizenMatchingController, SandboxPrivilegedCitizenMatchingController}
import uk.gov.hmrc.individualsmatchingapi.domain.SandboxData.sandboxMatchId
import uk.gov.hmrc.individualsmatchingapi.domain._
import uk.gov.hmrc.individualsmatchingapi.services.{LiveCitizenMatchingService, SandboxCitizenMatchingService, ScopesService}
import unit.uk.gov.hmrc.individualsmatchingapi.support.SpecBase

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.Future.{failed, successful}
import scala.util.Random

class PrivilegedCitizenMatchingControllerSpec
    extends SpecBase with MustMatchers with MockitoSugar with Results with BeforeAndAfter {

  trait Setup extends ScopesConfigHelper {

    val sampleCorrelationId = "188e9400-b636-4a3b-80ba-230a8c72b92a"

    val fakeRequest = FakeRequest()

    val controllerComponents = fakeApplication.injector.instanceOf[ControllerComponents]

    val sandboxCitizenMatchingService = new SandboxCitizenMatchingService
    val mockLiveCitizenMatchingService = mock[LiveCitizenMatchingService]

    val mockAuthConnector = mock[AuthConnector]

    val mockScopesService = new ScopesService(mockScopesConfig)

    implicit val ec: ExecutionContext = fakeApplication.injector.instanceOf[ExecutionContext]

    val liveController = new LivePrivilegedCitizenMatchingController(
      mockLiveCitizenMatchingService,
      mockScopesService,
      mockAuthConnector,
      controllerComponents)

    val sandboxController = new SandboxPrivilegedCitizenMatchingController(
      sandboxCitizenMatchingService,
      mockScopesService,
      mockAuthConnector,
      controllerComponents)

    given(mockAuthConnector.authorise(any(), refEq(Retrievals.allEnrolments))(any(), any()))
      .willReturn(Future.successful(Enrolments(Set(Enrolment("test-scope")))))
  }

  "live matching citizen controller" should {

    val matchId = UUID.randomUUID()

    "return 200 (Ok) for a matched citizen" in new Setup {
      when(mockLiveCitizenMatchingService.matchCitizen(any[CitizenMatchingRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(matchId))

      val eventualResult = liveController.matchCitizen()(
        fakeRequest.withBody(parse(matchingRequest())).withHeaders(("CorrelationId", sampleCorrelationId)))

      status(eventualResult) mustBe OK
      contentAsJson(eventualResult) mustBe parse(
        s"""
             {
               "_links": {
                 "individual": {
                   "href": "/individuals/matching/$matchId",
                   "name": "GET",
                   "title": "Get a matched individual’s information"
                 },
                 "self": {
                   "href": "/individuals/matching/"
                 }
               }
             }"""
      )
    }

    "return 200 Ok when matching a user with a '.' in their name" in new Setup {
      when(mockLiveCitizenMatchingService.matchCitizen(any())(any()))
        .thenReturn(Future.successful(matchId))

      val payload = Json.obj(
        "firstName"   -> "Mr.",
        "lastName"    -> "St. John",
        "nino"        -> "AA112233B",
        "dateOfBirth" -> "1900-01-01"
      )

      val res =
        liveController.matchCitizen()(fakeRequest.withBody(payload).withHeaders(("CorrelationId", sampleCorrelationId)))
      status(res) mustBe OK
    }

    "return 403 (Forbidden) for a citizen not found" in new Setup {
      when(mockLiveCitizenMatchingService.matchCitizen(any[CitizenMatchingRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new CitizenNotFoundException))

      val eventualResult = liveController.matchCitizen()(
        fakeRequest.withBody(parse(matchingRequest())).withHeaders(("CorrelationId", sampleCorrelationId))
      )

      status(eventualResult) mustBe FORBIDDEN
      contentAsJson(eventualResult) mustBe Json.obj(
        "code"    -> "MATCHING_FAILED",
        "message" -> "There is no match for the information provided"
      )
    }

    "return 403 (Forbidden) when a matching exception is thrown" in new Setup {
      when(mockLiveCitizenMatchingService.matchCitizen(any[CitizenMatchingRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new MatchingException))

      val eventualResult = liveController.matchCitizen()(
        fakeRequest.withBody(parse(matchingRequest())).withHeaders(("CorrelationId", sampleCorrelationId)))

      status(eventualResult) mustBe FORBIDDEN
      contentAsJson(eventualResult) mustBe Json.obj(
        "code"    -> "MATCHING_FAILED",
        "message" -> "There is no match for the information provided"
      )
    }

    "return 403 (Forbidden) when an invalid nino exception is thrown" in new Setup {
      when(mockLiveCitizenMatchingService.matchCitizen(any[CitizenMatchingRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new InvalidNinoException()))

      val eventualResult = liveController.matchCitizen()(
        fakeRequest.withBody(parse(matchingRequest())).withHeaders(("CorrelationId", sampleCorrelationId)))

      status(eventualResult) mustBe FORBIDDEN
      contentAsJson(eventualResult) mustBe Json.obj(
        "code"    -> "MATCHING_FAILED",
        "message" -> "There is no match for the information provided"
      )
    }

    "return 400 (BadRequest) for an invalid dateOfBirth" in new Setup {
      var requestBody =
        parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"NA000799C","dateOfBirth":"2020-01-32"}""")
      var eventualResult =
        liveController.matchCitizen()(
          fakeRequest.withBody(requestBody).withHeaders(("CorrelationId", sampleCorrelationId)))

      status(eventualResult) mustBe BAD_REQUEST
      contentAsJson(eventualResult) mustBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "dateOfBirth: invalid date format"
      )

      requestBody = parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"NA000799C","dateOfBirth":"20200131"}""")
      eventualResult = liveController.matchCitizen()(
        fakeRequest.withBody(requestBody).withHeaders(("CorrelationId", sampleCorrelationId)))

      status(eventualResult) mustBe BAD_REQUEST
      contentAsJson(eventualResult) mustBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "dateOfBirth: invalid date format"
      )
    }

    "return 400 (BadRequest) for an invalid nino" in new Setup {
      val requestBody =
        parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"AB1234567","dateOfBirth":"2020-01-31"}""")
      val eventualResult =
        liveController.matchCitizen()(
          fakeRequest.withBody(requestBody).withHeaders(("CorrelationId", sampleCorrelationId)))

      status(eventualResult) mustBe BAD_REQUEST
      contentAsJson(eventualResult) mustBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "Malformed nino submitted"
      )
    }

    "return 400 Bad Request when the first name is empty" in new Setup {
      val emptyFirstName = Json.obj(
        "firstName"   -> "",
        "lastName"    -> "Person",
        "nino"        -> "AA112233B",
        "dateOfBirth" -> "1900-01-01"
      )

      val res =
        liveController.matchCitizen()(
          fakeRequest.withBody(emptyFirstName).withHeaders(("CorrelationId", sampleCorrelationId)))

      status(res) mustBe BAD_REQUEST
      contentAsJson(res) mustBe Json.obj("code" -> "INVALID_REQUEST", "message" -> "firstName is required")
    }

    "return 400 Bad Request when the last name is empty" in new Setup {
      val emptyLastName = Json.obj(
        "firstName"   -> "Mr",
        "lastName"    -> "",
        "nino"        -> "AA112233B",
        "dateOfBirth" -> "1900-01-01"
      )

      val res =
        liveController.matchCitizen()(
          fakeRequest.withBody(emptyLastName).withHeaders(("CorrelationId", sampleCorrelationId)))

      status(res) mustBe BAD_REQUEST
      contentAsJson(res) mustBe Json.obj("code" -> "INVALID_REQUEST", "message" -> "lastName is required")
    }

    "return 400 Bad Request when the first name is greater than 35 characters" in new Setup {
      val firstNameTooLong = Json.obj(
        "firstName"   -> Random.nextString(36),
        "lastName"    -> "Person",
        "nino"        -> "AA112233B",
        "dateOfBirth" -> "1900-01-01"
      )

      val res =
        liveController.matchCitizen()(
          fakeRequest.withBody(firstNameTooLong).withHeaders(("CorrelationId", sampleCorrelationId)))

      status(res) mustBe BAD_REQUEST
      contentAsJson(res) mustBe Json
        .obj("code" -> "INVALID_REQUEST", "message" -> "firstName must be no more than 35 characters")
    }

    "return 400 Bad Request when the last name is greater than 35 characters" in new Setup {
      val lastNameTooLong = Json.obj(
        "firstName"   -> "Mr",
        "lastName"    -> Random.nextString(36),
        "nino"        -> "AA112233B",
        "dateOfBirth" -> "1900-01-01"
      )

      val res =
        liveController.matchCitizen()(
          fakeRequest.withBody(lastNameTooLong).withHeaders(("CorrelationId", sampleCorrelationId)))

      status(res) mustBe BAD_REQUEST
      contentAsJson(res) mustBe Json
        .obj("code" -> "INVALID_REQUEST", "message" -> "lastName must be no more than 35 characters")
    }

    "return 400 Bad Request when the first name contains invalid characters" in new Setup {
      val invalidFirstName = Json.obj(
        "firstName"   -> """/\/\/\/\""",
        "lastName"    -> "Person",
        "nino"        -> "AA112233B",
        "dateOfBirth" -> "1900-01-01"
      )

      val res =
        liveController.matchCitizen()(
          fakeRequest.withBody(invalidFirstName).withHeaders(("CorrelationId", sampleCorrelationId)))

      status(res) mustBe BAD_REQUEST
      contentAsJson(res) mustBe Json
        .obj("code" -> "INVALID_REQUEST", "message" -> "firstName contains invalid characters")
    }

    "return 400 Bad Request when the last name contains invalid characters" in new Setup {
      val invalidFirstName = Json.obj(
        "firstName"   -> "Mr",
        "lastName"    -> """/\/\/\/\""",
        "nino"        -> "AA112233B",
        "dateOfBirth" -> "1900-01-01"
      )

      val res =
        liveController.matchCitizen()(
          fakeRequest.withBody(invalidFirstName).withHeaders(("CorrelationId", sampleCorrelationId)))

      status(res) mustBe BAD_REQUEST
      contentAsJson(res) mustBe Json
        .obj("code" -> "INVALID_REQUEST", "message" -> "lastName contains invalid characters")
    }

    "fail with UnauthorizedException when the bearer token does not have enrolment read:individuals-matching" in new Setup {
      var requestBody =
        parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"NA000799C","dateOfBirth":"2020-01-32"}""")

      given(mockAuthConnector.authorise(any(), refEq(Retrievals.allEnrolments))(any(), any()))
        .willReturn(failed(new InsufficientEnrolments()))

      intercept[InsufficientEnrolments] {
        await(
          liveController.matchCitizen()(
            fakeRequest.withBody(requestBody).withHeaders(("CorrelationId", sampleCorrelationId))))
      }

      verifyZeroInteractions(mockLiveCitizenMatchingService)
    }
  }

  "Sandbox match citizen function" should {

    val matchId = UUID.randomUUID()

    "return 200 (Ok) for the sandbox matchId" in new Setup {
      val eventualResult = sandboxController.matchCitizen()(
        fakeRequest.withBody(parse(matchingRequest())).withHeaders(("CorrelationId", sampleCorrelationId)))

      status(eventualResult) mustBe OK
      contentAsJson(eventualResult) mustBe parse(
        s"""
             {
               "_links": {
                 "individual": {
                   "href": "/individuals/matching/$sandboxMatchId",
                   "name": "GET",
                   "title": "Get a matched individual’s information"
                 },
                 "self": {
                   "href": "/individuals/matching/"
                 }
               }
             }"""
      )
    }

    "return 403 (Forbidden) for a citizen not found" in new Setup {
      val eventualResult =
        sandboxController.matchCitizen()(
          fakeRequest
            .withBody(parse(matchingRequest(firstName = "José")))
            .withHeaders(("CorrelationId", sampleCorrelationId)))

      status(eventualResult) mustBe FORBIDDEN
      contentAsJson(eventualResult) mustBe Json.obj(
        "code"    -> "MATCHING_FAILED",
        "message" -> "There is no match for the information provided"
      )
    }

    "return 403 (Forbidden) when nino does not match a sandbox individual" in new Setup {
      val eventualResult =
        sandboxController.matchCitizen()(
          fakeRequest
            .withBody(parse(matchingRequest(nino = "AA000799C")))
            .withHeaders(("CorrelationId", sampleCorrelationId)))

      status(eventualResult) mustBe FORBIDDEN
      contentAsJson(eventualResult) mustBe Json.obj(
        "code"    -> "MATCHING_FAILED",
        "message" -> "There is no match for the information provided"
      )
    }

    "return 403 (Forbidden) when an invalid nino exception is thrown" in new Setup {
      val eventualResult =
        sandboxController.matchCitizen()(
          fakeRequest
            .withBody(parse(matchingRequest(nino = "NA000799D")))
            .withHeaders(("CorrelationId", sampleCorrelationId)))

      status(eventualResult) mustBe FORBIDDEN
      contentAsJson(eventualResult) mustBe Json.obj(
        "code"    -> "MATCHING_FAILED",
        "message" -> "There is no match for the information provided"
      )
    }

    "return 400 (BadRequest) for an invalid dateOfBirth" in new Setup {
      var requestBody =
        parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"NA000799C","dateOfBirth":"2020-01-32"}""")
      var eventualResult =
        sandboxController.matchCitizen()(
          fakeRequest.withBody(requestBody).withHeaders(("CorrelationId", sampleCorrelationId)))

      status(eventualResult) mustBe BAD_REQUEST
      contentAsJson(eventualResult) mustBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "dateOfBirth: invalid date format"
      )

      requestBody = parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"NA000799C","dateOfBirth":"20200131"}""")
      eventualResult = sandboxController.matchCitizen()(
        fakeRequest.withBody(requestBody).withHeaders(("CorrelationId", sampleCorrelationId)))

      status(eventualResult) mustBe BAD_REQUEST
      contentAsJson(eventualResult) mustBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "dateOfBirth: invalid date format"
      )
    }

    "return 400 (BadRequest) for an invalid nino" in new Setup {
      val requestBody =
        parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"AB1234567","dateOfBirth":"2020-01-31"}""")
      val eventualResult =
        sandboxController.matchCitizen()(
          fakeRequest.withBody(requestBody).withHeaders(("CorrelationId", sampleCorrelationId)))

      status(eventualResult) mustBe BAD_REQUEST
      contentAsJson(eventualResult) mustBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "Malformed nino submitted"
      )
    }

    "not require bearer token authentication" in new Setup {
      val eventualResult = sandboxController.matchCitizen()(
        fakeRequest.withBody(parse(matchingRequest())).withHeaders(("CorrelationId", sampleCorrelationId)))

      status(eventualResult) mustBe OK
      verifyZeroInteractions(mockAuthConnector)
    }

    "return 400 (Bad Request) when CorrelationId is missing" in new Setup {
      val requestBody =
        parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"NA000799C","dateOfBirth":"2020-01-31"}""")
      val exception =
        intercept[BadRequestException](sandboxController.matchCitizen()(fakeRequest.withBody(requestBody)))

      exception.message mustBe "CorrelationId is required"
      exception.responseCode mustBe BAD_REQUEST
    }

    "return 400 (Bad Request) when CorrelationId is malformed" in new Setup {
      val requestBody =
        parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"NA000799C","dateOfBirth":"2020-01-31"}""")

      val exception = intercept[BadRequestException](
        sandboxController.matchCitizen()(
          fakeRequest.withBody(requestBody).withHeaders(("CorrelationId", "fakeCorrelationId"))))

      exception.message mustBe "Malformed CorrelationId"
      exception.responseCode mustBe BAD_REQUEST
    }
  }

  private def matchingRequest(
    firstName: String = "Amanda",
    dateOfBirth: String = "1960-01-15",
    nino: String = "NA000799C") =
    s"""{
            "firstName":"$firstName",
            "lastName":"Joseph",
            "nino":"$nino",
            "dateOfBirth":"$dateOfBirth"
          }
        """
}
