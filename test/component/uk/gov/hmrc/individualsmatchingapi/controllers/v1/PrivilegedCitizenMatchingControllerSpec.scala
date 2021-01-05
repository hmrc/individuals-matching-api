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

package component.uk.gov.hmrc.individualsmatchingapi.controllers.v1

import component.uk.gov.hmrc.individualsmatchingapi.stubs.{AuthStub, BaseSpec, CitizenDetailsStub, MatchingStub}
import org.joda.time.LocalDate
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

  feature("citizen matching is open and accessible") {

    scenario("valid request to the sandbox implementation. Individual's details match sandbox citizen") {

      When("I request individual income for the sandbox matchId")
      val response = Http(s"$serviceUrl/sandbox/")
        .postData("""{"firstName":"Amanda","lastName":"Joseph","nino":"NA000799C","dateOfBirth":"1960-01-15"}""")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 200 (Ok)")
      response.code shouldBe OK

      And("The response contains a valid payload")
      parse(response.body) shouldBe parse(
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

      Then("a single ninoMatch record is stored in mongo with its corresponding NINO and generated matchId")
      val ninoMatchRecords =
        Await.result(mongoRepository.find("nino" -> nino), 3 second)
      ninoMatchRecords.size shouldBe 1

      And("The response status should be 200 (Ok)")
      response.code shouldBe OK

      And("The response contains a valid payload")
      parse(response.body) shouldBe parse(
        s"""
           {
             "_links": {
               "individual": {
                 "href": "/individuals/matching/${ninoMatchRecords.head.id}",
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
      parse(response.body) shouldBe Json.toJson(ErrorMatchingFailed)
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
      parse(response.body) shouldBe Json.toJson(ErrorMatchingFailed)
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
      parse(response.body) shouldBe Json.toJson(ErrorMatchingFailed)
    }
  }

  scenario("NINO provided with wrong format") {

    Given("A valid privileged Auth bearer token")
    AuthStub.willAuthorizePrivilegedAuthToken(authToken)

    And("The given NINO is invalid")
    CitizenDetailsStub.getByNinoReturnsError(nino, BAD_REQUEST, s"invalid nino: $nino")

    When("I request a citizen's details match")
    val response = requestMatch(matchingRequest, s => s.replace(nino, "ABC"))

    Then("The response status should be 403 (Forbidden)")
    response.code shouldBe BAD_REQUEST

    And("The correct error message is returned")
    print(s" customized reposne ${parse(response.body)}")
    parse(response.body) shouldBe
      JsonFormatters.errorInvalidRequestFormat.writes(ErrorInvalidRequest("Malformed nino submitted"))
  }

  def requestMatch(
    matchingRequest: CitizenMatchingRequest,
    f: String => String = s => s.replace("", "")): HttpResponse[String] =
    Http(s"$serviceUrl/")
      .postData(f(Json.toJson(matchingRequest).toString()))
      .headers(requestHeaders(acceptHeaderP1))
      .asString

  def citizenMatchingRequest(firstName: String, lastName: String, nino: String, dateOfBirth: String) =
    CitizenMatchingRequest(firstName, lastName, nino, dateOfBirth)

  def citizenDetails(firstName: String, lastName: String, nino: String, dateOfBirth: String) =
    CitizenDetails(Some(firstName), Some(lastName), Some(nino), Some(LocalDate.parse(dateOfBirth)))
}
