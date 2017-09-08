/*
 * Copyright 2017 HM Revenue & Customs
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

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.http.HeaderNames.LOCATION
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.individualsmatchingapi.controllers.CitizenMatchingController
import uk.gov.hmrc.individualsmatchingapi.domain.{CitizenMatchingRequest, CitizenNotFoundException, InvalidNinoException, MatchingException}
import uk.gov.hmrc.individualsmatchingapi.services.CitizenMatchingService
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class CitizenMatchingControllerSpec extends UnitSpec with MockitoSugar with ScalaFutures with WithFakeApplication {
  implicit lazy val materializer = fakeApplication.materializer

  trait Setup {
    val fakeRequest = FakeRequest()
    val mockCitizenMatchingService = mock[CitizenMatchingService]
    val citizenMatchingController = new CitizenMatchingController(mockCitizenMatchingService) {}
  }

  "match citizen function" should {

    val matchId = UUID.randomUUID()

    "return 303 for a matched citizen" in new Setup {
      when(mockCitizenMatchingService.matchCitizen(any[CitizenMatchingRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(matchId))

      val result = await(citizenMatchingController.matchCitizen()(fakeRequest.withBody(Json.parse(matchingRequest()))))

      status(result) shouldBe SEE_OTHER
      bodyOf(result) shouldBe "{}"
      result.header.headers.get(LOCATION) shouldBe Some(s"/individuals/matching/$matchId")
    }

    "return 403 (Forbidden) for a citizen not found" in new Setup {
      when(mockCitizenMatchingService.matchCitizen(any[CitizenMatchingRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new CitizenNotFoundException))

      val result = await(citizenMatchingController.matchCitizen()(fakeRequest.withBody(Json.parse(matchingRequest()))))

      status(result) shouldBe FORBIDDEN
      jsonBodyOf(result) shouldBe Json.obj(
        "code" -> "MATCHING_FAILED", "message" -> "There is no match for the information provided"
      )
    }

    "return 403 (Forbidden) when a matching exception is thrown" in new Setup {
      when(mockCitizenMatchingService.matchCitizen(any[CitizenMatchingRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new MatchingException))

      val result = await(citizenMatchingController.matchCitizen()(fakeRequest.withBody(Json.parse(matchingRequest()))))

      status(result) shouldBe FORBIDDEN
      jsonBodyOf(result) shouldBe Json.obj(
        "code" -> "MATCHING_FAILED", "message" -> "There is no match for the information provided"
      )
    }

    "return 403 (Forbidden) when an invalid nino exception is thrown" in new Setup {
      when(mockCitizenMatchingService.matchCitizen(any[CitizenMatchingRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new InvalidNinoException()))

      val result = await(citizenMatchingController.matchCitizen()(fakeRequest.withBody(Json.parse(matchingRequest()))))

      status(result) shouldBe FORBIDDEN
      jsonBodyOf(result) shouldBe Json.obj(
        "code" -> "MATCHING_FAILED", "message" -> "There is no match for the information provided"
      )
    }

    "return 400 (BadRequest) for an invalid dateOfBirth" in new Setup {
      var requestBody = Json.parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"NA000799C","dateOfBirth":"2020-01-32"}""")
      var result = await(citizenMatchingController.matchCitizen()(fakeRequest.withBody(requestBody)))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.obj(
        "code" -> "INVALID_REQUEST", "message" -> "dateOfBirth: invalid date format"
      )

      requestBody = Json.parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"NA000799C","dateOfBirth":"20200131"}""")
      result = await(citizenMatchingController.matchCitizen()(fakeRequest.withBody(requestBody)))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.obj(
        "code" -> "INVALID_REQUEST", "message" -> "dateOfBirth: invalid date format"
      )
    }

    "return 400 (BadRequest) for an invalid nino" in new Setup {
      val requestBody = Json.parse("""{"firstName":"Amanda","lastName":"Joseph","nino":"AB1234567","dateOfBirth":"2020-01-31"}""")
      val result = await(citizenMatchingController.matchCitizen()(fakeRequest.withBody(requestBody)))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.obj(
        "code" -> "INVALID_REQUEST", "message" -> "Malformed nino submitted"
      )
    }
  }

  private def matchingRequest(firstName: String = "Amanda", dateOfBirth: String = "1960-01-15") = {
    s"""{
          "firstName":"$firstName",
          "lastName":"Joseph",
          "nino":"NA000799C",
          "dateOfBirth":"$dateOfBirth"
        }
      """
  }

  private def invalidMatchingRequest() = {
    s"""{
          "lastName":"Joseph",
          "nino":"NA000799C",
          "dateOfBirth":"1960-01-15"
        }
      """
  }
}
