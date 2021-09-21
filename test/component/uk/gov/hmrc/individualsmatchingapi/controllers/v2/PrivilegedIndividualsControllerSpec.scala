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

package component.uk.gov.hmrc.individualsmatchingapi.controllers.v2

import component.uk.gov.hmrc.individualsmatchingapi.stubs.{AuthStub, BaseSpec, CitizenDetailsStub}
import play.api.http.Status
import play.api.libs.json.Json
import scalaj.http.Http
import uk.gov.hmrc.domain.Nino

import scala.concurrent.Await.result

class PrivilegedIndividualsControllerSpec extends BaseSpec {

  val nino = "NA000799C"

  // Scopes list MUST be in alphabetical order
  val scopes = List(
    "read:individuals-matching-hmcts-c2",
    "read:individuals-matching-hmcts-c3",
    "read:individuals-matching-hmcts-c4",
    "read:individuals-matching-ho-ecp",
    "read:individuals-matching-ho-rp2",
    "read:individuals-matching-laa-c1",
    "read:individuals-matching-laa-c2",
    "read:individuals-matching-laa-c3",
    "read:individuals-matching-laa-c4",
    "read:individuals-matching-lsani-c1",
    "read:individuals-matching-lsani-c3",
    "read:individuals-matching-nictsejo-c4"
  )

  val validScopes = List(
    "read:individuals-matching-laa-c3"
  )

  Feature("matched individual is open and accessible") {

    Scenario("valid request to the live implementation") {

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, scopes, validScopes)

      And("A valid nino match exist")
      val matchId = result(mongoRepository.create(Nino(nino)), timeout).id.toString

      And("Citizen exists for the given NINO")
      CitizenDetailsStub.getByNinoReturnsCitizenDetails(nino, "John", "Smith", "13101972")

      When("I request available resources for a matched individual")
      val response = Http(s"$serviceUrl/$matchId").headers(requestHeaders(acceptHeaderP2)).asString

      Then("The response status should be 200 (Ok)")
      response.code shouldBe Status.OK

      And("The response contains a valid payload")
      Json.parse(response.body) shouldBe Json.parse(s"""
         {
           "_links":{
               "benefits-and-credits": {
               "href": "/individuals/benefits-and-credits/?matchId=$matchId",
               "title": "Get the individual's benefits and credits data"
             },
             "details": {
               "href": "/individuals/details/?matchId=$matchId",
               "title": "Get the individual's details"
             },
             "employments": {
               "href": "/individuals/employments/?matchId=$matchId",
               "title": "Get the individual's employment data"
             },
             "income": {
               "href": "/individuals/income/?matchId=$matchId",
               "title": "Get the individual's income data"
             },
             "self":{
               "href":"/individuals/matching/$matchId"
             }
           },
           "individual": {
             "firstName":"John",
             "lastName":"Smith",
             "nino":"$nino",
             "dateOfBirth":"1972-10-13"
           }
         }
       """)
    }
  }
}
