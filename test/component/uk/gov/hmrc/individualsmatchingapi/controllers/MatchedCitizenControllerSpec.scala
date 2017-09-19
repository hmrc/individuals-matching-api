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

import component.uk.gov.hmrc.individualsmatchingapi.stubs.BaseSpec
import play.api.libs.json.Json
import play.api.test.Helpers.{await, _}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.individualsmatchingapi.domain.SandboxData.sandboxMatchId

import scalaj.http.Http

class MatchedCitizenControllerSpec extends BaseSpec {

  val nino = "CS700100A"

  feature("Live implementation of matched citizen record is open and accessible") {

    scenario("request for a matched citizen with a valid matchId") {

      Given("A match record exists for a given NINO")
      val ninoMatch = await(mongoRepository.create(Nino(nino)))

      When("I request the matched citizen record using the corresponding valid matchId")
      val response = Http(s"$serviceUrl/match-record/${ninoMatch.id.toString}").asString

      Then("The response status should be 200 (Ok)")
      response.code shouldBe OK

      And("The response contains the matched citizen record")
      Json.parse(response.body) shouldBe Json.parse(s"""{"nino":"$nino","matchId":"${ninoMatch.id.toString}"}""")
    }

    scenario("request for a matched citizen with an invalid matchId") {

      Given("A matchId without a corresponding matched citizen record")
      val matchId = UUID.randomUUID().toString

      When("I request the matched citizen record using the invalid matchId")
      val response = Http(s"$serviceUrl/match-record/$matchId").asString

      Then("The response status should be 404 (Not Found)")
      response.code shouldBe NOT_FOUND
    }
  }

  feature("Sandbox implementation of matched citizen record is not accessible") {

    scenario("Request for a sandbox matched citizen record") {

      When("I attempt to request a sandbox matched citizen record")
      val response = Http(s"$serviceUrl/sandbox/match-record/$sandboxMatchId").asString

      Then("The response status should be 404 (Not Found)")
      response.code shouldBe NOT_FOUND
    }
  }
}
