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

package component.uk.gov.hmrc.individualsmatchingapi.controllers.v2

import component.uk.gov.hmrc.individualsmatchingapi.stubs.{AuthStub, BaseSpec, CitizenDetailsStub, MatchingStub}
import org.mongodb.scala.model.Filters
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.libs.json.Json.parse
import play.api.test.Helpers.{BAD_REQUEST, NOT_FOUND}
import scalaj.http.{Http, HttpResponse}
import uk.gov.hmrc.individualsmatchingapi.domain.*
import uk.gov.hmrc.individualsmatchingapi.domain.JsonFormatters.*
import uk.gov.hmrc.mongo.play.json.Codecs.toBson

import java.time.LocalDate
import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

//noinspection LanguageFeature
class PrivilegedCitizenMatchingControllerSpec extends BaseSpec {

  val nino = "CS700100A"
  val firstName = "Amanda"
  val lastName = "Joseph"
  val dateOfBirthDesFormat = "13101972"
  val dateOfBirthSensibleformat = "1972-10-13"
  val matchingRequest: CitizenMatchingRequest =
    CitizenMatchingRequest(firstName, lastName, nino, dateOfBirthSensibleformat)

  // Scopes list MUST be in alphabetical order
  val scopes: List[String] = List(
    "read:individuals-matching-hmcts-c2",
    "read:individuals-matching-hmcts-c3",
    "read:individuals-matching-hmcts-c4",
    "read:individuals-matching-ho-ecp",
    "read:individuals-matching-ho-nrc",
    "read:individuals-matching-ho-rp2",
    "read:individuals-matching-ho-v2",
    "read:individuals-matching-laa-c1",
    "read:individuals-matching-laa-c2",
    "read:individuals-matching-laa-c3",
    "read:individuals-matching-laa-c4",
    "read:individuals-matching-lad4",
    "read:individuals-matching-lsani-c1",
    "read:individuals-matching-lsani-c3",
    "read:individuals-matching-nictsejo-c4",
    "read:individuals-matching-scts"
  )

  val validScopes: List[String] = List("read:individuals-matching-laa-c3")

  Feature("citizen matching is open and accessible") {

    Scenario("Valid request to the live implementation. Individual's details match existing citizen records") {

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, scopes, validScopes)

      And("Citizen exists for the given NINO")
      CitizenDetailsStub.getByNinoReturnsCitizenDetails(nino, firstName, lastName, dateOfBirthDesFormat)

      And("Citizen details match the individual's details provided")
      MatchingStub.performMatchReturnsNoErrorCodes(
        citizenMatchingRequest(firstName, lastName, nino, dateOfBirthSensibleformat),
        citizenDetails(firstName, lastName, nino, dateOfBirthSensibleformat)
      )

      When("I request a citizen's details match")
      val response = requestMatch(matchingRequest)

      Then("a single ninoMatch record is stored in mongo with its corresponding NINO and generated matchId")
      val ninoMatchRecords =
        Await.result(mongoRepository.collection.find(Filters.equal("nino", toBson(nino))).headOption(), 3.second)
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
                 "title": "Get a matched individual’s information"
               },
               "self": {
                 "href": "/individuals/matching/"
               }
             }
           }"""
      )
    }
  }

  Feature("Citizen matching error handling") {
    Scenario("No match. Individual's details do not match existing citizen records") {

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, scopes, validScopes)

      And("Citizen exists for the given NINO")
      CitizenDetailsStub.getByNinoReturnsCitizenDetails("CS700100A", firstName, lastName, "13091972")

      And("Citizen details match the individual's details provided")
      MatchingStub.performMatchReturnsErrorCodes(
        citizenMatchingRequest(firstName, lastName, nino, dateOfBirthSensibleformat),
        citizenDetails(firstName, lastName, "CS700100A", "1972-09-13")
      )

      When("I request a citizen's details match")
      val response = requestMatch(matchingRequest)

      Then("The response status should be 404 (Not Found)")
      response.code shouldBe NOT_FOUND

      And("The correct error message is returned")
      Json.parse(response.body) shouldBe Json.parse(
        s"""{"code":"MATCHING_FAILED","message":"There is no match for the information provided"}"""
      )
    }

    Scenario("Citizen does not exist for the given NINO") {

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, scopes, validScopes)

      And("Citizen for the given NINO cannot be found")
      CitizenDetailsStub.getByNinoReturnsError(nino, NOT_FOUND)

      When("I request a citizen's details match")
      val response = requestMatch(matchingRequest)

      Then("The response status should be 404 (Not Found)")
      response.code shouldBe NOT_FOUND

      And("The correct error message is returned")
      Json.parse(response.body) shouldBe Json.parse(
        s"""{"code":"MATCHING_FAILED","message":"There is no match for the information provided"}"""
      )
    }

    Scenario("Invalid NINO provided") {

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, scopes, validScopes)

      And("The given NINO is invalid")
      CitizenDetailsStub.getByNinoReturnsError(nino, BAD_REQUEST, s"invalid nino: $nino")

      When("I request a citizen's details match")
      val response = requestMatch(matchingRequest)

      Then("The response status should be 404 (Not Found)")
      response.code shouldBe NOT_FOUND

      And("The correct error message is returned")
      Json.parse(response.body) shouldBe Json.parse(
        s"""{"code":"MATCHING_FAILED","message":"There is no match for the information provided"}"""
      )
    }
  }

  Scenario("NINO provided with wrong format") {

    Given("A valid privileged Auth bearer token")
    AuthStub.willAuthorizePrivilegedAuthToken(authToken, scopes, validScopes)

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
    f: String => String = s => s.replace("", "")
  ): HttpResponse[String] =
    Http(s"$serviceUrl/")
      .postData(f(Json.toJson(matchingRequest).toString()))
      .headers(requestHeaders(acceptHeaderP2))
      .asString

  def citizenMatchingRequest(
    firstName: String,
    lastName: String,
    nino: String,
    dateOfBirth: String
  ): CitizenMatchingRequest =
    CitizenMatchingRequest(firstName, lastName, nino, dateOfBirth)

  def citizenDetails(firstName: String, lastName: String, nino: String, dateOfBirth: String): CitizenDetails =
    CitizenDetails(Some(firstName), Some(lastName), Some(nino), Some(LocalDate.parse(dateOfBirth)))
}
