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

import component.uk.gov.hmrc.individualsmatchingapi.stubs.{AuthStub, BaseSpec, CitizenDetailsStub}
import play.api.http.Status
import play.api.libs.json.Json
import scalaj.http.Http
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.individualsmatchingapi.domain.SandboxData.sandboxMatchId

import scala.concurrent.Await.result

class PrivilegedIndividualsControllerSpec extends BaseSpec {

  val nino = "NA000799C"

  feature("matched individual is open and accessible") {

    scenario("valid request to the live implementation") {

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken)

      And("A valid nino match exist")
      val matchId = result(mongoRepository.create(Nino(nino)), timeout).id.toString

      And("Citizen exists for the given NINO")
      CitizenDetailsStub.getByNinoReturnsCitizenDetails(nino, "John", "Smith", "13101972")

      When("I request available resources for a matched individual")
      val response = Http(s"$serviceUrl/$matchId").headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 200 (Ok)")
      response.code shouldBe Status.OK

      And("The response contains a valid payload")
      Json.parse(response.body) shouldBe Json.parse(s"""
           {
             "_links":{
               "income":{
                 "href":"/individuals/income/?matchId=$matchId",
                 "name":"GET",
                 "title":"View individual's income"
               },
               "employments":{
                 "href":"/individuals/employments/?matchId=$matchId",
                 "name":"GET",
                 "title":"View individual's employments"
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

    scenario("valid request to the sandbox implementation") {

      When("I request available resources for a matched individual")
      val response = Http(s"$serviceUrl/sandbox/$sandboxMatchId").headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 200 (Ok)")
      response.code shouldBe Status.OK

      And("The response contains a valid payload")
      Json.parse(response.body) shouldBe Json.parse(s"""
           {
             "_links":{
               "income":{
                 "href":"/individuals/income/?matchId=$sandboxMatchId",
                 "name":"GET",
                 "title":"View individual's income"
               },
               "employments":{
                 "href":"/individuals/employments/?matchId=$sandboxMatchId",
                 "name":"GET",
                 "title":"View individual's employments"
               },
               "self":{
                 "href":"/individuals/matching/$sandboxMatchId"
               }
             },
             "individual": {
               "firstName":"Amanda",
               "lastName":"Joseph",
               "nino":"NA000799C",
               "dateOfBirth":"1960-01-15"
             }
           }
         """)
    }
  }
}
