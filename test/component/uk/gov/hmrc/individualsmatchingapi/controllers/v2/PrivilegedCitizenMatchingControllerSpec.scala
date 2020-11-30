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

package component.uk.gov.hmrc.individualsmatchingapi.controllers.v2

import component.uk.gov.hmrc.individualsmatchingapi.stubs.{AuthStub, BaseSpec, CitizenDetailsStub, MatchingStub}
import org.joda.time.LocalDate
import play.api.http.Status
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.json.Json.parse
import play.api.test.Helpers.{BAD_REQUEST, NOT_FOUND}
import scalaj.http.{Http, HttpResponse}
import uk.gov.hmrc.individualsmatchingapi.domain.JsonFormatters._
import uk.gov.hmrc.individualsmatchingapi.domain.SandboxData.sandboxMatchId
import uk.gov.hmrc.individualsmatchingapi.domain._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationDouble

class PrivilegedCitizenMatchingControllerSpec extends BaseSpec {

  val nino = "CS700100A"
  val firstName = "Amanda"
  val lastName = "Joseph"
  val dateOfBirthDesFormat = "13101972"
  val dateOfBirthSensibleformat = "1972-10-13"
  val matchingRequest =
    CitizenMatchingRequest(firstName, lastName, nino, dateOfBirthSensibleformat)

  val scopes = List("read:individuals-matching")

  feature("citizen matching is open and accessible") {

    scenario("valid request to the sandbox implementation. Individual's details match sandbox citizen") {

      When("I request individual income for the sandbox matchId")
      val response = Http(s"$serviceUrl/sandbox/")
        .postData("""{"firstName":"Amanda","lastName":"Joseph","nino":"NA000799C","dateOfBirth":"1960-01-15"}""")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 500")
      response.code shouldBe Status.INTERNAL_SERVER_ERROR

      And("The response contains a valid payload")
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("Valid request to the live implementation. Individual's details match existing citizen records") {

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, scopes)

      And("Citizen exists for the given NINO")
      CitizenDetailsStub.getByNinoReturnsCitizenDetails(nino, firstName, lastName, dateOfBirthDesFormat)

      And("Citizen details match the individual's details provided")
      MatchingStub.performMatchReturnsNoErrorCodes(
        citizenMatchingRequest(firstName, lastName, nino, dateOfBirthSensibleformat),
        citizenDetails(firstName, lastName, nino, dateOfBirthSensibleformat))

      When("I request a citizen's details match")
      val response = requestMatch(matchingRequest)

      Then("a single ninoMatch record is stored in mongo with its corresponding NINO and generated matchId")
      val ninoMatchRecords =
        Await.result(mongoRepository.find("nino" -> nino), 3 second)
      ninoMatchRecords.size shouldBe 0

      Then("The response status should be 500")
      response.code shouldBe Status.INTERNAL_SERVER_ERROR

      And("The response contains a valid payload")
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }
  }

  feature("Citizen matching error handling") {
    scenario("No match. Individual's details do not match existing citizen records") {

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, scopes)

      And("Citizen exists for the given NINO")
      CitizenDetailsStub.getByNinoReturnsCitizenDetails("CS700100A", firstName, lastName, "13091972")

      And("Citizen details match the individual's details provided")
      MatchingStub.performMatchReturnsErrorCodes(
        citizenMatchingRequest(firstName, lastName, nino, dateOfBirthSensibleformat),
        citizenDetails(firstName, lastName, "CS700100A", "1972-09-13"))

      When("I request a citizen's details match")
      val response = requestMatch(matchingRequest)

      Then("The response status should be 500")
      response.code shouldBe Status.INTERNAL_SERVER_ERROR

      And("The correct error message is returned")
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("Citizen does not exist for the given NINO") {

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, scopes)

      And("Citizen for the given NINO cannot be found")
      CitizenDetailsStub.getByNinoReturnsError(nino, NOT_FOUND)

      When("I request a citizen's details match")
      val response = requestMatch(matchingRequest)

      Then("The response status should be 500")
      response.code shouldBe Status.INTERNAL_SERVER_ERROR

      And("The correct error message is returned")
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("Invalid NINO provided") {

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, scopes)

      And("The given NINO is invalid")
      CitizenDetailsStub.getByNinoReturnsError(nino, BAD_REQUEST, s"invalid nino: $nino")

      When("I request a citizen's details match")
      val response = requestMatch(matchingRequest)

      Then("The response status should be 500")
      response.code shouldBe Status.INTERNAL_SERVER_ERROR

      And("The correct error message is returned")
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }
  }

  scenario("NINO provided with wrong format") {

    Given("A valid privileged Auth bearer token")
    AuthStub.willAuthorizePrivilegedAuthToken(authToken, scopes)

    And("The given NINO is invalid")
    CitizenDetailsStub.getByNinoReturnsError(nino, BAD_REQUEST, s"invalid nino: $nino")

    When("I request a citizen's details match")
    val response = requestMatch(matchingRequest, s => s.replace(nino, "ABC"))

    Then("The response status should be 500")
    response.code shouldBe Status.INTERNAL_SERVER_ERROR

    And("The correct error message is returned")
    response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
  }

  def requestMatch(
    matchingRequest: CitizenMatchingRequest,
    f: String => String = s => s.replace("", "")): HttpResponse[String] =
    Http(s"$serviceUrl/")
      .postData(f(Json.toJson(matchingRequest).toString()))
      .headers(requestHeaders(acceptHeaderP2))
      .asString

  def citizenMatchingRequest(firstName: String, lastName: String, nino: String, dateOfBirth: String) =
    CitizenMatchingRequest(firstName, lastName, nino, dateOfBirth)

  def citizenDetails(firstName: String, lastName: String, nino: String, dateOfBirth: String) =
    CitizenDetails(Some(firstName), Some(lastName), Some(nino), Some(LocalDate.parse(dateOfBirth)))
}
