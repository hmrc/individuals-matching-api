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

import component.uk.gov.hmrc.individualsmatchingapi.stubs.BaseSpec
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.individualsmatchingapi.domain.SandboxData.sandboxMatchId

import scalaj.http.Http

class PrivilegedIndividualsControllerSpec extends BaseSpec {

  feature("matched individual is open and accessible") {

    scenario("valid request to the sandbox implementation") {

      When("I request available resources for a matched individual")
      val response = Http(s"$serviceUrl/sandbox/$sandboxMatchId").headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 200 (Ok)")
      response.code shouldBe Status.OK

      And("The response contains a valid payload")
      Json.parse(response.body) shouldBe Json.parse(
        s"""
           {
             "_links":{
               "income":{
                 "href":"/individuals/income?matchId=$sandboxMatchId",
                 "name":"GET",
                 "title":"View individual's income"
               },
               "employments":{
                 "href":"/individuals/employments?matchId=$sandboxMatchId",
                 "name":"GET",
                 "title":"View individual's employments"
               },
               "self":{
                 "href":"/individuals/matching/$sandboxMatchId"
               }
             },
             "firstName":"Amanda",
             "lastName":"Joseph",
             "nino":"NA000799C",
             "dateOfBirth":"1960-01-15"
           }
         """)
    }
  }
}
