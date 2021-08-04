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

package component.uk.gov.hmrc.individualsmatchingapi

import component.uk.gov.hmrc.individualsmatchingapi.stubs.{AuthStub, BaseSpec}
import play.api.Application
import play.api.http.Status
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.{ACCEPT, AUTHORIZATION}
import uk.gov.hmrc.individualsmatchingapi.domain.SandboxData.sandboxMatchId
import scalaj.http.{Http, HttpResponse}

class VersioningSpec extends BaseSpec {

  implicit override lazy val app: Application = GuiceApplicationBuilder()
    .configure(
      "auditing.enabled"                -> false,
      "auditing.traceRequests"          -> false,
      "microservice.services.auth.port" -> AuthStub.port,
      "run.mode"                        -> "It"
    )
    .build()

  feature("Versioning") {

    scenario("Requests with an accept header version P1") {

      When("A request to the match citizen endpoint is made with version P1 accept header")
      val response = invokeWithHeaders(s"/sandbox/$sandboxMatchId", AUTHORIZATION -> authToken, acceptHeaderP1)

      Then("The response status should be 200 (Ok)")
      response.code shouldBe OK

      And("The response body should be for api version P1")
      Json.parse(response.body) shouldBe
        Json.parse(s"""
            {
              "_links": {
                "income": {
                  "href": "/individuals/income/?matchId=$sandboxMatchId",
                  "name": "GET",
                  "title": "View individual's income"
                },
                "employments": {
                  "href": "/individuals/employments/?matchId=$sandboxMatchId",
                  "name": "GET",
                  "title": "View individual's employments"
                },
                "self": {
                  "href": "/individuals/matching/$sandboxMatchId"
                }
              },
              "individual": {
                "firstName": "Amanda",
                "lastName": "Joseph",
                "nino": "NA000799C",
                "dateOfBirth": "1960-01-15"
              }
            }""")
    }

    scenario("Requests with an accept header version P2") {

      When("A request to the match citizen endpoint is made with version P2 accept header")
      val response = invokeWithHeaders(
        s"/sandbox/$sandboxMatchId",
        AUTHORIZATION -> authToken,
        acceptHeaderP2,
        testCorrelationHeader)

      Then("The response status should be 200 (Ok)")
      response.code shouldBe OK

      And("The response body should be for api version P1")
      Json.parse(response.body) shouldBe
        Json.parse(s"""
            {
              "_links": {
                "benefits-and-credits": {
                  "href": "/individuals/benefits-and-credits/?matchId=$sandboxMatchId",
                  "title": "Get the individual's benefits and credits data"
                },
                "details": {
                  "href": "/individuals/details/?matchId=$sandboxMatchId",
                  "title": "Get the individual's details"
                },
                "employments": {
                  "href": "/individuals/employments/?matchId=$sandboxMatchId",
                  "title": "Get the individual's employment data"
                },
                "income": {
                  "href": "/individuals/income/?matchId=$sandboxMatchId",
                  "title": "Get the individual's income data"
                },
                "self": {
                  "href"  : "/individuals/matching/$sandboxMatchId"
                }
              },
              "individual": {
                "firstName": "Amanda",
                "lastName": "Joseph",
                "nino": "NA000799C",
                "dateOfBirth": "1960-01-15"
              }
            }""")
    }

    scenario("Requests without an accept header default to version 1") {

      When("A request to the match citizen endpoint is made without an accept header")
      val response = invokeWithHeaders(s"/sandbox/$sandboxMatchId", AUTHORIZATION -> authToken)

      Then("The response status should be 404 (Not Found)")
      response.code shouldBe NOT_FOUND
    }

    scenario("Requests with an accept header version 1 are correctly forwarded") {

      When("A request to the match citizen endpoint is made with version 1 accept header")
      val response = invokeWithHeaders(s"/sandbox/$sandboxMatchId", AUTHORIZATION -> authToken, acceptHeaderV1)

      Then("The response status should be 404 (Not Found)")
      response.code shouldBe NOT_FOUND
    }

    scenario("Requests with an accept header with an invalid version") {

      When("A request to the match citizen endpoint is made with version 10.0 accept header")
      val response = invokeWithHeaders(
        s"/sandbox/$sandboxMatchId",
        AUTHORIZATION -> authToken,
        ACCEPT        -> "application/vnd.hmrc.10.0+json")

      Then("The response status should be 404 (Not Found)")
      response.code shouldBe NOT_FOUND
    }
  }

  private def invokeWithHeaders(urlPath: String, headers: (String, String)*): HttpResponse[String] =
    Http(s"$serviceUrl$urlPath").headers(headers).asString
}
