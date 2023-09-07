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

  "Gets correct internal endpoints" when {
    "using first scope" in {
      val endpoints = scopesService.getInternalEndpoints(Seq(mockScopeOne))
      endpoints.size shouldBe 2
      endpoints.map(_.link).toSet shouldBe Set("/internal/1", "/internal/2")
      endpoints.map(_.title).toSet shouldBe Set("Get the first endpoint", "Get the second endpoint")
    }

    "using second scope" in {
      val endpoints = scopesService.getInternalEndpoints(Seq(mockScopeTwo))
      endpoints.map(_.link).toSet shouldBe Set("/internal/2", "/internal/3")
      endpoints.map(_.title).toSet shouldBe Set("Get the second endpoint", "Get the third endpoint")
    }

    "using invalid scope" in {
      val endpoints = scopesService.getInternalEndpoints(Seq("invalidScope"))
      endpoints.size shouldBe 0
    }
  }

  "Get correct filters" when {

    "using third scope" in {
      val filters = scopesService.getValidFilters(Seq(mockScopeThree), Seq(endpointThree))
      filters.size shouldBe 1
      filters shouldBe Seq("contains(path/to/g,'FILTERED_VALUE_1')")
    }

    "using fourth scope" in {
      val filters = scopesService.getValidFilters(Seq(mockScopeFour), Seq(endpointThree))
      filters.size shouldBe 1
      filters shouldBe Seq("contains(path/to/g,'FILTERED_VALUE_2')")
    }

    "using third and fourth scopes" in {
      val filters = scopesService.getValidFilters(Seq(mockScopeThree, mockScopeFour), Seq(endpointThree))
      filters.size shouldBe 2
      filters shouldBe Seq("contains(path/to/g,'FILTERED_VALUE_1')", "contains(path/to/g,'FILTERED_VALUE_2')")
    }

    "using invalid scope" in {
      val filters = scopesService.getValidFilters(Seq("invalidScope"), Seq(endpointThree))
      filters.size shouldBe 0
    }
  }

  "Get correct cache key" when {

    "using first scope and first endpoint" in {
      val endpoints = scopesService.getValidFieldsForCacheKey(Seq(mockScopeOne), Seq(endpointOne))
      endpoints shouldBe "ABC"
    }

    "using second scope and first endpoint" in {
      val endpoints = scopesService.getValidFieldsForCacheKey(Seq(mockScopeTwo), Seq(endpointOne))
      endpoints shouldBe ""
    }

    "using first scope and second endpoint" in {
      val endpoints = scopesService.getValidFieldsForCacheKey(Seq(mockScopeOne), Seq(endpointTwo))
      endpoints shouldBe "D"
    }

    "using second scope and second endpoint" in {
      val endpoints = scopesService.getValidFieldsForCacheKey(Seq(mockScopeTwo), Seq(endpointTwo))
      endpoints shouldBe "EF"
    }

  }

  "Gets all scopes correctly" in {
    val scopes = scopesService.getAllScopes
    scopes shouldBe Seq(mockScopeFour, mockScopeOne, mockScopeThree, mockScopeTwo)
  }
}
