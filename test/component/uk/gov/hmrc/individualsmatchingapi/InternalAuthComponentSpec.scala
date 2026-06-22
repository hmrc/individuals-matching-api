/*
 * Copyright 2026 HM Revenue & Customs
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

/*
 * Copyright 2026 HM Revenue & Customs
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

package component.uk.gov.hmrc.individualsmatchingapi

import component.uk.gov.hmrc.individualsmatchingapi.stubs.{AuthStub, BaseSpec, CitizenDetailsStub, InternalAuthStub, MatchingStub}
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.test.Helpers.OK
import scalaj.http.{Http, HttpResponse}
import uk.gov.hmrc.individualsmatchingapi.domain.CitizenMatchingRequest
import unit.uk.gov.hmrc.individualsmatchingapi.util.Individuals

class InternalAuthComponentSpec extends BaseSpec with Individuals {
  val nino = "CS700100A"
  val firstName = "Amanda"
  val lastName = "Joseph"
  val dateOfBirthDesFormat = "13101972"
  val dateOfBirthSensibleFormat = "1972-10-13"
  val scopes: List[String] = List("read:individuals-matching")
  val validScopes: List[String] = List("read:individuals-matching")

  Feature("internal auth precedence and fallback") {
    Scenario("internal auth success bypasses legacy auth") {
      Given("internal auth authorises the token")
      InternalAuthStub.willAuthorizeToken(authToken)

      And("the matching dependencies return a successful match")
      stubSuccessfulMatch()

      When("the V1 matching endpoint is invoked")
      val response = requestMatch()

      Then("the request succeeds and does not call legacy auth")
      response.code mustBe OK
      InternalAuthStub.verifyAuthRequestCount(expectedCount = 1)
      AuthStub.verifyAuthoriseRequestCount(expectedCount = 0)
    }

    Scenario("internal auth denial falls back to the V1 legacy scope") {
      Given("internal auth denies the token")
      InternalAuthStub.willNotAuthorizeToken(authToken)

      And("legacy auth allows the V1 matching scope")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, scopes, validScopes)

      And("the matching dependencies return a successful match")
      stubSuccessfulMatch()

      When("the V1 matching endpoint is invoked")
      val response = requestMatch()

      Then("the request succeeds via fallback and both auth services were called")
      response.code mustBe OK
      InternalAuthStub.verifyAuthRequestCount(expectedCount = 1)
      AuthStub.verifyAuthoriseRequestCount(expectedCount = 1)
    }
  }

  Feature("internal auth feature flag") {
    Scenario("when internal auth is disabled, V1 matching is authorised via legacy scopes auth only") {
      Given("legacy auth allows the V1 matching scope")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, scopes, validScopes)

      And("the matching dependencies return a successful match")
      stubSuccessfulMatch()

      When("the V1 matching endpoint is invoked against an app with internal auth disabled")
      val response = withConfiguredServer("features.internal-auth.enabled" -> false) { disabledServiceUrl =>
        requestMatch(disabledServiceUrl)
      }

      Then("the request succeeds, using legacy auth without calling internal auth")
      response.code mustBe OK
      InternalAuthStub.verifyAuthRequestCount(expectedCount = 0)
      AuthStub.verifyAuthoriseRequestCount(expectedCount = 1)
    }
  }

  private def stubSuccessfulMatch(): Unit = {
    CitizenDetailsStub.getByNinoReturnsCitizenDetails(nino, firstName, lastName, dateOfBirthDesFormat)
    MatchingStub.performMatchReturnsNoErrorCodes(
      CitizenMatchingRequest(firstName, lastName, nino, dateOfBirthSensibleFormat),
      citizenDetails(firstName, lastName, nino, dateOfBirthSensibleFormat)
    )
  }

  private def requestMatch(baseUrl: String = serviceUrl): HttpResponse[String] =
    Http(s"$baseUrl/")
      .postData(
        s"""{"firstName":"$firstName","lastName":"$lastName","nino":"$nino","dateOfBirth":"$dateOfBirthSensibleFormat"}"""
      )
      .headers(requestHeaders(acceptHeaderP1))
      .asString
}
