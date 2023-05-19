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

package unit.uk.gov.hmrc.individualsmatchingapi.services

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.individualsmatchingapi.services.{ScopesHelper, ScopesService}

class ScopesHelperSpec extends AnyWordSpec with ScopesConfig with Matchers {

  "Scopes helper" should {

    val scopesService = new ScopesService(mockConfig)
    val scopesHelper = new ScopesHelper(scopesService)

    "return correct query string" in {
      val scopeOneResult =
        scopesHelper.getQueryStringFor(List(mockScopeOne), List(endpointOne, endpointTwo, endpointThree))
      scopeOneResult shouldBe "path(to(a,b,c,d))"

      val scopeOneEndpointOneResult = scopesHelper.getQueryStringFor(List(mockScopeOne), List(endpointOne))
      scopeOneEndpointOneResult shouldBe "path(to(a,b,c))"

      val scopeOneEndpointTwoResult = scopesHelper.getQueryStringFor(List(mockScopeOne), List(endpointTwo))
      scopeOneEndpointTwoResult shouldBe "path(to(d))"

      val scopeOneEndpointThreeResult = scopesHelper.getQueryStringFor(List(mockScopeOne), List(endpointThree))
      scopeOneEndpointThreeResult shouldBe ""

      val scopeTwoResult =
        scopesHelper.getQueryStringFor(List(mockScopeTwo), List(endpointOne, endpointTwo, endpointThree))
      scopeTwoResult shouldBe "path(to(e,f,g,h,i))"

      val twoScopesResult =
        scopesHelper.getQueryStringFor(List(mockScopeOne, mockScopeTwo), List(endpointOne, endpointTwo, endpointThree))
      twoScopesResult shouldBe "path(to(a,b,c,d,e,f,g,h,i))"
    }

    "return correct query string with filter" in {
      val scopeThreeResult = scopesHelper.getQueryStringFor(List(mockScopeThree), List(endpointThree))
      scopeThreeResult shouldBe "path(to(g,h,i))&filter=contains(path/to/g,'FILTERED_VALUE_1')"

      val scopeFourResult = scopesHelper.getQueryStringFor(List(mockScopeFour), List(endpointThree))
      scopeFourResult shouldBe "path(to(g,h,i))&filter=contains(path/to/g,'FILTERED_VALUE_2')"
    }

    "generate Hal response" in {

      val mockData = Json.obj(
        "employments" -> Json.obj(
          "field1" -> Json.toJson("value1"),
          "field2" -> Json.toJson("value2")
        )
      )

      val response = scopesHelper.getHalResponse(
        endpoint = endpointOne,
        scopes = List(mockScopeOne),
        data = Some(mockData)
      )

      response.links.links.size shouldBe 5

      response.links.links.exists(halLink => halLink.rel == endpointOne && halLink.href == "/internal/1") shouldBe true

      response.links.links.exists(halLink => halLink.rel == "self" && halLink.href == "/internal/1") shouldBe true

      val response2 = scopesHelper.getHalResponse(
        endpoint = endpointTwo,
        scopes = List(mockScopeOne, mockScopeTwo),
        data = Some(mockData)
      )

      response2.links.links.size shouldBe 7

      response2.links.links.exists(halLink => halLink.rel == endpointTwo && halLink.href == "/external/2") shouldBe true

      response2.links.links
        .exists(halLink => halLink.rel == endpointThree && halLink.href == "/external/3") shouldBe true

      response2.links.links.exists(halLink => halLink.rel == "self" && halLink.href == "/internal/2") shouldBe true

    }
  }
}
