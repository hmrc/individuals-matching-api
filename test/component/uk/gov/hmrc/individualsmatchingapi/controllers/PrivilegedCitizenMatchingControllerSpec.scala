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

package component.uk.gov.hmrc.individualsmatchingapi.controllers

import java.util.UUID

import component.uk.gov.hmrc.individualsmatchingapi.stubs.{AuthStub, BaseSpec, CitizenDetailsStub, MatchingStub}
import org.joda.time.LocalDate
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.{BAD_REQUEST, LOCATION, NOT_FOUND}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.individualsmatchingapi.domain.{CitizenDetails, CitizenMatchingRequest, ErrorMatchingFailed}
import uk.gov.hmrc.individualsmatchingapi.domain.SandboxData.sandboxMatchId
import uk.gov.hmrc.individualsmatchingapi.domain.JsonFormatters._

import scala.concurrent.Await.result
import scalaj.http.{Http, HttpResponse}

class PrivilegedCitizenMatchingControllerSpec extends BaseSpec {

  val nino = "CS700100A"
  val firstName = "Amanda"
  val lastName = "Joseph"
  val dateOfBirthDesFormat = "13101972"
  val dateOfBirthSensibleformat = "1972-10-13"
  val matchingRequest = CitizenMatchingRequest(firstName, lastName, nino, dateOfBirthSensibleformat)

  feature("citizen matching is open and accessible") {

    scenario("valid request to the sandbox implementation") {

      When("I request individual income for the sandbox matchId")
      val response = Http(s"$serviceUrl/sandbox/")
        .postData("""{"firstName":"Amanda","lastName":"Joseph","nino":"NA000799C","dateOfBirth":"1960-01-15"}""")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 303 (See Other)")
      response.code shouldBe SEE_OTHER

      And("The 'Location' header contains the correct URL")
      response.headers.get(LOCATION) shouldBe Some(s"/individuals/matching/$sandboxMatchId")
    }

    scenario("Valid request to the live implementation. Individual's details match existing citizen records") {

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken)

      And("Citizen exists for the given NINO")
      CitizenDetailsStub.getByNinoReturnsCitizenDetails(nino, firstName, lastName, dateOfBirthDesFormat)

      And("Citizen details match the individual's details provided")
      MatchingStub.performMatchReturnsNoErrorCodes(
        citizenMatchingRequest(firstName, lastName, nino, dateOfBirthSensibleformat),
        citizenDetails(firstName, lastName, nino, dateOfBirthSensibleformat))

      When("I request a citizen's details match")
      val response = requestMatch(matchingRequest)

      Then("The response status should be 303 (See Other)")
      response.code shouldBe SEE_OTHER

      And("The 'Location' header contains the generated matchId")
      val locationHeader = response.headers.get(LOCATION)
      locationHeader shouldNot be(None)
      val matchId = locationHeader.get.stripPrefix("/individuals/matching/")

      And("The matchId is stored in mongo with its corresponding NINO")
      val ninoMatch = result(mongoRepository.read(UUID.fromString(matchId)), timeout).get
      ninoMatch.nino shouldBe Nino(nino)
    }
  }

  feature("Citizen matching error handling") {
    scenario("No match. Individual's details do not match existing citizen records") {

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken)

      And("Citizen exists for the given NINO")
      CitizenDetailsStub.getByNinoReturnsCitizenDetails("CS700100A", firstName, lastName, "13091972")

      And("Citizen details match the individual's details provided")
      MatchingStub.performMatchReturnsErrorCodes(
        citizenMatchingRequest(firstName, lastName, nino, dateOfBirthSensibleformat),
        citizenDetails(firstName, lastName, "CS700100A", "1972-09-13"))

      When("I request a citizen's details match")
      val response = requestMatch(matchingRequest)

      Then("The response status should be 403 (Forbidden)")
      response.code shouldBe FORBIDDEN

      And("The correct error message is returned")
      Json.parse(response.body) shouldBe Json.toJson(ErrorMatchingFailed)
    }

    scenario("Citizen does not exist for the given NINO") {

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken)

      And("Citizen for the given NINO cannot be found")
      CitizenDetailsStub.getByNinoReturnsError(nino, NOT_FOUND)

      When("I request a citizen's details match")
      val response = requestMatch(matchingRequest)

      Then("The response status should be 403 (Forbidden)")
      response.code shouldBe FORBIDDEN

      And("The correct error message is returned")
      Json.parse(response.body) shouldBe Json.toJson(ErrorMatchingFailed)
    }

    scenario("Invalid NINO provided") {

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken)

      And("The given NINO is invalid")
      CitizenDetailsStub.getByNinoReturnsError(nino, BAD_REQUEST, s"invalid nino: $nino")

      When("I request a citizen's details match")
      val response = requestMatch(matchingRequest)

      Then("The response status should be 403 (Forbidden)")
      response.code shouldBe FORBIDDEN

      And("The correct error message is returned")
      Json.parse(response.body) shouldBe Json.toJson(ErrorMatchingFailed)
    }
  }

  def requestMatch(matchingRequest: CitizenMatchingRequest): HttpResponse[String] = {
    Http(s"$serviceUrl/")
      .postData(Json.toJson(matchingRequest).toString()).headers(requestHeaders(acceptHeaderP1)).asString
  }

  def citizenMatchingRequest(firstName: String,
                             lastName: String,
                             nino: String,
                             dateOfBirth: String) = {
    CitizenMatchingRequest(firstName, lastName, nino, dateOfBirth)
  }

  def citizenDetails(firstName: String, lastName: String, nino: String, dateOfBirth: String) = {
    CitizenDetails(Some(firstName), Some(lastName), Some(nino), Some(LocalDate.parse(dateOfBirth)))
  }
}
