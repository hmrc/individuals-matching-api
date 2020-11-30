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

import component.uk.gov.hmrc.individualsmatchingapi.stubs.{AuthStub, BaseSpec, CitizenDetailsStub}
import play.api.http.Status
import play.api.libs.json.Json
import scalaj.http.Http
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.individualsmatchingapi.domain.SandboxData.sandboxMatchId

import scala.concurrent.Await.result

class PrivilegedIndividualsControllerSpec extends BaseSpec {

  val nino = "NA000799C"
  val scopes = List("read:individuals-matching")

  feature("matched individual is open and accessible") {

    scenario("valid request to the live implementation") {

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, scopes)

      And("A valid nino match exist")
      val matchId = result(mongoRepository.create(Nino(nino)), timeout).id.toString

      And("Citizen exists for the given NINO")
      CitizenDetailsStub.getByNinoReturnsCitizenDetails(nino, "John", "Smith", "13101972")

      When("I request available resources for a matched individual")
      val response = Http(s"$serviceUrl/$matchId").headers(requestHeaders(acceptHeaderP2)).asString

      Then("The response status should be 500")
      response.code shouldBe Status.INTERNAL_SERVER_ERROR

      And("The response contains a valid payload")
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("valid request to the sandbox implementation") {

      When("I request available resources for a matched individual")
      val response = Http(s"$serviceUrl/sandbox/$sandboxMatchId").headers(requestHeaders(acceptHeaderP2)).asString

      Then("The response status should be 500")
      response.code shouldBe Status.INTERNAL_SERVER_ERROR

      And("The response contains a valid payload")
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }
  }
}
