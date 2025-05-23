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

package component.uk.gov.hmrc.individualsmatchingapi.controllers

import component.uk.gov.hmrc.individualsmatchingapi.stubs.BaseSpec
import play.api.libs.json.Json
import play.api.test.Helpers.*
import scalaj.http.Http
import uk.gov.hmrc.domain.Nino

class MatchedCitizenControllerSpec extends BaseSpec {

  val nino = "CS700100A"

  Feature("Live implementation of matched citizen record is open and accessible") {

    Scenario("request for a matched citizen with a valid matchId") {

      Given("A match record exists for a given NINO")
      val ninoMatch = await(mongoRepository.create(Nino(nino)))

      When("I request the matched citizen record using the corresponding valid matchId")
      val response =
        Http(s"$serviceUrl/match-record/${ninoMatch.id.toString}").asString

      Then("The response status should be 200 (Ok)")
      response.code shouldBe OK

      And("The response contains the matched citizen record")
      Json.parse(response.body) shouldBe Json.parse(s"""{"nino":"$nino","matchId":"${ninoMatch.id.toString}"}""")
    }

    Scenario("request for a matched citizen with an invalid matchId") {

      Given("A matchId without a corresponding matched citizen record")
      val matchId = "123"

      When("I request the matched citizen record using the invalid matchId")
      val response = Http(s"$serviceUrl/match-record/$matchId").asString

      Then("The response status should be 404 (Not Found)")
      response.code shouldBe NOT_FOUND
      Json.parse(response.body) shouldBe Json.parse(
        s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}"""
      )
    }
  }
}
