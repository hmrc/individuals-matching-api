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
import uk.gov.hmrc.individualsmatchingapi.services.ScopesService

class ScopesServiceSpec extends AnyWordSpec with Matchers with ScopesConfig {

  val scopesService = new ScopesService(mockConfig)

  "Gets correct external endpoints" when {
    "using first scope" in {
      val endpoints = scopesService.getExternalEndpoints(Seq(mockScopeOne))
      endpoints.size shouldBe 2
      endpoints.map(_.key).toSet shouldBe Set(endpointKeyOne, endpointKeyTwo)
      endpoints.map(_.link).toSet shouldBe Set("/external/1", "/external/2")
      endpoints.map(_.title).toSet shouldBe Set("Get the first endpoint", "Get the second endpoint")
    }

    "using second scope" in {
      val endpoints = scopesService.getExternalEndpoints(Seq(mockScopeTwo))
      endpoints.size shouldBe 2
      endpoints.map(_.key).toSet shouldBe Set(endpointKeyTwo, endpointKeyThree)
      endpoints.map(_.link).toSet shouldBe Set("/external/2", "/external/3")
      endpoints.map(_.title).toSet shouldBe Set("Get the second endpoint", "Get the third endpoint")
    }

    "using invalid scope" in {
      val endpoints = scopesService.getExternalEndpoints(Seq("invalidScope"))
      endpoints.size shouldBe 0
    }
  }

  "Gets all scopes correctly" in {
    val scopes = scopesService.getAllScopes
    scopes shouldBe Seq(mockScopeFive, mockScopeFour, mockScopeOne, mockScopeThree, mockScopeTwo)
  }

  "using fifth scope" in {
    val endpoints = scopesService.getExternalEndpoints(Seq(mockScopeFive))
    endpoints.size shouldBe 3
    endpoints.map(_.key).toSet shouldBe Set(endpointKeyTwo, endpointKeyThree, endpointKeyFour)
    endpoints.map(_.link).toSet shouldBe Set("/external/2", "/external/3", "/external/4")
    endpoints.map(_.title).toSet shouldBe Set(
      "Get the second endpoint",
      "Get the third endpoint",
      "Get the fourth endpoint"
    )
  }

}
