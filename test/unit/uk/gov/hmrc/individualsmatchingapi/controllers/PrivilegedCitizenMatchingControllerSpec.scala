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

import java.util.UUID

import org.mockito.BDDMockito.given
import org.mockito.Matchers.{any, refEq}
import org.mockito.Mockito.{verifyZeroInteractions, when}
import org.scalatest.BeforeAndAfter
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.libs.json.Json.parse
import play.api.mvc.Results
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, _}
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.{Enrolment, InsufficientEnrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsmatchingapi.config.ServiceAuthConnector
import uk.gov.hmrc.individualsmatchingapi.controllers.{LivePrivilegedCitizenMatchingController, SandboxPrivilegedCitizenMatchingController}
import uk.gov.hmrc.individualsmatchingapi.domain.SandboxData.sandboxMatchId
import uk.gov.hmrc.individualsmatchingapi.domain._
import uk.gov.hmrc.individualsmatchingapi.services.{LiveCitizenMatchingService, SandboxCitizenMatchingService}

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}
import scala.util.Random

class PrivilegedCitizenMatchingControllerSpec extends PlaySpec with MockitoSugar with Results with BeforeAndAfter {

  trait Setup {
    val fakeRequest = FakeRequest()

    val sandboxCitizenMatchingService = new SandboxCitizenMatchingService
    val sandboxController = new SandboxPrivilegedCitizenMatchingController(sandboxCitizenMatchingService, mockAuthConnector)

    val mockLiveCitizenMatchingService = mock[LiveCitizenMatchingService]
    val mockAuthConnector = mock[ServiceAuthConnector]
    val liveController = new LivePrivilegedCitizenMatchingController(mockLiveCitizenMatchingService, mockAuthConnector)

    given(mockAuthConnector.authorise(any(), refEq(EmptyRetrieval))(any(), any())).willReturn(successful(()))

  }

  "live matching citizen controller" should {

    val matchId = UUID.randomUUID()

    "return 200 (Ok) for a matched citizen" in new Setup {
      when(mockLiveCitizenMatchingService.matchCitizen(any[CitizenMatchingRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(matchId))

      val eventualResult = liveController.matchCitizen()(fakeRequest.withBody(parse(matchingRequest())))

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
      when(mockLiveCitizenMatchingService.matchCitizen(any())(any())).thenReturn(Future.successful(matchId))

      val payload = Json.obj(
        "firstName" -> "Mr.",
        "lastName" -> "St. John",
        "nino" -> "AA112233B",
        "dateOfBirth" -> "1900-01-01"
      )

      val res = liveController.matchCitizen()(fakeRequest.withBody(payload))
      status(res) mustBe OK
    }

    "return 403 (Forbidden) for a citizen not found" in new Setup {
      when(mockLiveCitizenMatchingService.matchCitizen(any[CitizenMatchingRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new CitizenNotFoundException))

      val eventualResult = liveController.matchCitizen()(fakeRequest.withBody(parse(matchingRequest())))

      status(eventualResult) mustBe FORBIDDEN
      contentAsJson(eventualResult) mustBe Json.obj(
        "code" -> "MATCHING_FAILED", "message" -> "There is no match for the information provided"
      )
    }

    "return 403 (Forbidden) when a matching exception is thrown" in new Setup {
      when(mockLiveCitizenMatchingService.matchCitizen(any[CitizenMatchingRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new MatchingException))

      val eventualResult = liveController.matchCitizen()(fakeRequest.withBody(parse(matchingRequest())))

      status(eventualResult) mustBe FORBIDDEN
      contentAsJson(eventualResult) mustBe Json.obj(
        "code" -> "MATCHING_FAILED", "message" -> "There is no match for the information provided"
      )
    }

    "return 403 (Forbidden) when an invalid nino exception is thrown" in new Setup {
      when(mockLiveCitizenMatchingService.matchCitizen(any[CitizenMatchingRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new InvalidNinoException()))

      val eventualResult = liveController.matchCitizen()(fakeRequest.withBody(parse(matchingRequest())))

      status(eventualResult) mustBe FORBIDDEN
      contentAsJson(eventualResult) mustBe Json.obj(
        "code" -> "MATCHING_FAILED", "message" -> "There is no match for the information provided"
      )
    }

    "return 400 (BadRequest) for an invalid dateOfBirth" in new Setup {
      var requestBody = parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"NA000799C","dateOfBirth":"2020-01-32"}""")
      var eventualResult = liveController.matchCitizen()(fakeRequest.withBody(requestBody))

      status(eventualResult) mustBe BAD_REQUEST
      contentAsJson(eventualResult) mustBe Json.obj(
        "code" -> "INVALID_REQUEST", "message" -> "dateOfBirth: invalid date format"
      )

      requestBody = parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"NA000799C","dateOfBirth":"20200131"}""")
      eventualResult = liveController.matchCitizen()(fakeRequest.withBody(requestBody))

      status(eventualResult) mustBe BAD_REQUEST
      contentAsJson(eventualResult) mustBe Json.obj(
        "code" -> "INVALID_REQUEST", "message" -> "dateOfBirth: invalid date format"
      )
    }

    "return 400 (BadRequest) for an invalid nino" in new Setup {
      val requestBody = parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"AB1234567","dateOfBirth":"2020-01-31"}""")
      val eventualResult = liveController.matchCitizen()(fakeRequest.withBody(requestBody))

      status(eventualResult) mustBe BAD_REQUEST
      contentAsJson(eventualResult) mustBe Json.obj(
        "code" -> "INVALID_REQUEST", "message" -> "Malformed nino submitted"
      )
    }

    "return 400 Bad Request when the first name is empty" in new Setup {
      val emptyFirstName = Json.obj(
        "firstName" -> "",
        "lastName" -> "Person",
        "nino" -> "AA112233B",
        "dateOfBirth" -> "1900-01-01"
      )

      val res = liveController.matchCitizen()(fakeRequest.withBody(emptyFirstName))

      status(res) mustBe BAD_REQUEST
      contentAsJson(res) mustBe Json.obj("code" -> "INVALID_REQUEST", "message" -> "firstName is required")
    }

    "return 400 Bad Request when the last name is empty" in new Setup {
      val emptyLastName = Json.obj(
        "firstName" -> "Mr",
        "lastName" -> "",
        "nino" -> "AA112233B",
        "dateOfBirth" -> "1900-01-01"
      )

      val res = liveController.matchCitizen()(fakeRequest.withBody(emptyLastName))

      status(res) mustBe BAD_REQUEST
      contentAsJson(res) mustBe Json.obj("code" -> "INVALID_REQUEST", "message" -> "lastName is required")
    }

    "return 400 Bad Request when the first name is greater than 35 characters" in new Setup {
      val firstNameTooLong = Json.obj(
        "firstName" -> Random.nextString(36),
        "lastName" -> "Person",
        "nino" -> "AA112233B",
        "dateOfBirth" -> "1900-01-01"
      )

      val res = liveController.matchCitizen()(fakeRequest.withBody(firstNameTooLong))

      status(res) mustBe BAD_REQUEST
      contentAsJson(res) mustBe Json.obj("code" -> "INVALID_REQUEST", "message" -> "firstName must be no more than 35 characters")
    }

    "return 400 Bad Request when the last name is greater than 35 characters" in new Setup {
      val lastNameTooLong = Json.obj(
        "firstName" -> "Mr",
        "lastName" -> Random.nextString(36),
        "nino" -> "AA112233B",
        "dateOfBirth" -> "1900-01-01"
      )

      val res = liveController.matchCitizen()(fakeRequest.withBody(lastNameTooLong))

      status(res) mustBe BAD_REQUEST
      contentAsJson(res) mustBe Json.obj("code" -> "INVALID_REQUEST", "message" -> "lastName must be no more than 35 characters")
    }

    "return 400 Bad Request when the first name contains invalid characters" in new Setup {
      val invalidFirstName = Json.obj(
        "firstName" -> """/\/\/\/\""",
        "lastName" -> "Person",
        "nino" -> "AA112233B",
        "dateOfBirth" -> "1900-01-01"
      )

      val res = liveController.matchCitizen()(fakeRequest.withBody(invalidFirstName))

      status(res) mustBe BAD_REQUEST
      contentAsJson(res) mustBe Json.obj("code" -> "INVALID_REQUEST", "message" -> "firstName contains invalid characters")
    }

    "return 400 Bad Request when the last name contains invalid characters" in new Setup {
      val invalidFirstName = Json.obj(
        "firstName" -> "Mr",
        "lastName" -> """/\/\/\/\""",
        "nino" -> "AA112233B",
        "dateOfBirth" -> "1900-01-01"
      )

      val res = liveController.matchCitizen()(fakeRequest.withBody(invalidFirstName))

      status(res) mustBe BAD_REQUEST
      contentAsJson(res) mustBe Json.obj("code" -> "INVALID_REQUEST", "message" -> "lastName contains invalid characters")
    }

    "fail with UnauthorizedException when the bearer token does not have enrolment read:individuals-matching" in new Setup {
      var requestBody = parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"NA000799C","dateOfBirth":"2020-01-32"}""")

      given(mockAuthConnector.authorise(refEq(Enrolment("read:individuals-matching")), refEq(EmptyRetrieval))(any(), any())).willReturn(failed(new InsufficientEnrolments()))

      intercept[InsufficientEnrolments]{await(liveController.matchCitizen()(fakeRequest.withBody(requestBody)))}

      verifyZeroInteractions(mockLiveCitizenMatchingService)
    }
  }

  "Sandbox match citizen function" should {

    val matchId = UUID.randomUUID()

    "return 200 (Ok) for the sandbox matchId" in new Setup {
      val eventualResult = sandboxController.matchCitizen()(fakeRequest.withBody(parse(matchingRequest())))

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
      val eventualResult = sandboxController.matchCitizen()(fakeRequest.withBody(parse(matchingRequest(firstName = "José"))))

      status(eventualResult) mustBe FORBIDDEN
      contentAsJson(eventualResult) mustBe Json.obj(
        "code" -> "MATCHING_FAILED", "message" -> "There is no match for the information provided"
      )
    }

    "return 403 (Forbidden) when nino does not match a sandbox individual" in new Setup {
      val eventualResult = sandboxController.matchCitizen()(fakeRequest.withBody(parse(matchingRequest(nino="AA000799C"))))

      status(eventualResult) mustBe FORBIDDEN
      contentAsJson(eventualResult) mustBe Json.obj(
        "code" -> "MATCHING_FAILED", "message" -> "There is no match for the information provided"
      )
    }

    "return 403 (Forbidden) when an invalid nino exception is thrown" in new Setup {
      val eventualResult = sandboxController.matchCitizen()(fakeRequest.withBody(parse(matchingRequest(nino="NA000799D"))))

      status(eventualResult) mustBe FORBIDDEN
      contentAsJson(eventualResult) mustBe Json.obj(
        "code" -> "MATCHING_FAILED", "message" -> "There is no match for the information provided"
      )
    }

    "return 400 (BadRequest) for an invalid dateOfBirth" in new Setup {
      var requestBody = parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"NA000799C","dateOfBirth":"2020-01-32"}""")
      var eventualResult = sandboxController.matchCitizen()(fakeRequest.withBody(requestBody))

      status(eventualResult) mustBe BAD_REQUEST
      contentAsJson(eventualResult) mustBe Json.obj(
        "code" -> "INVALID_REQUEST", "message" -> "dateOfBirth: invalid date format"
      )

      requestBody = parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"NA000799C","dateOfBirth":"20200131"}""")
      eventualResult = sandboxController.matchCitizen()(fakeRequest.withBody(requestBody))

      status(eventualResult) mustBe BAD_REQUEST
      contentAsJson(eventualResult) mustBe Json.obj(
        "code" -> "INVALID_REQUEST", "message" -> "dateOfBirth: invalid date format"
      )
    }

    "return 400 (BadRequest) for an invalid nino" in new Setup {
      val requestBody = parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"AB1234567","dateOfBirth":"2020-01-31"}""")
      val eventualResult = sandboxController.matchCitizen()(fakeRequest.withBody(requestBody))

      status(eventualResult) mustBe BAD_REQUEST
      contentAsJson(eventualResult) mustBe Json.obj(
        "code" -> "INVALID_REQUEST", "message" -> "Malformed nino submitted"
      )
    }

    "not require bearer token authentication" in new Setup {
      val eventualResult = sandboxController.matchCitizen()(fakeRequest.withBody(parse(matchingRequest())))

      status(eventualResult) mustBe OK
      verifyZeroInteractions(mockAuthConnector)
    }
  }

  private def matchingRequest(firstName: String = "Amanda", dateOfBirth: String = "1960-01-15", nino: String = "NA000799C") = {
    s"""{
          "firstName":"$firstName",
          "lastName":"Joseph",
          "nino":"$nino",
          "dateOfBirth":"$dateOfBirth"
        }
      """
  }
}
