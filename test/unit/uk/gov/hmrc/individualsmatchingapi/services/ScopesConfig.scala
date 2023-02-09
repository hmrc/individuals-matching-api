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

import play.api.Configuration

trait ScopesConfig {

  val mockScopeOne: String = "scopeOne"
  val mockScopeTwo: String = "scopeTwo"
  val mockScopeThree: String = "scopeThree"
  val mockScopeFour: String = "scopeFour"

  val endpointKeyOne: String = "A"
  val endpointKeyTwo: String = "B"
  val endpointKeyThree: String = "C"

  val endpointOne: String = "sampleEndpointOne"
  val endpointTwo: String = "sampleEndpointTwo"
  val endpointThree: String = "sampleEndpointThree"

  val mockConfig: Configuration = Configuration(
    (s"api-config.scopes.$mockScopeOne.endpoints", Seq(endpointKeyOne, endpointKeyTwo)),
    (s"api-config.scopes.$mockScopeOne.fields", Seq("A", "B", "C", "D")),
    (s"api-config.scopes.$mockScopeTwo.endpoints", Seq(endpointKeyTwo, endpointKeyThree)),
    (s"api-config.scopes.$mockScopeTwo.fields", Seq("E", "F", "G", "H", "I")),
    (s"api-config.scopes.$mockScopeThree.endpoints", Seq(endpointKeyThree)),
    (s"api-config.scopes.$mockScopeThree.fields", Seq("G", "H", "I")),
    (s"api-config.scopes.$mockScopeThree.filters", Seq("A")),
    (s"api-config.scopes.$mockScopeFour.endpoints", Seq(endpointKeyThree)),
    (s"api-config.scopes.$mockScopeFour.fields", Seq("G", "H", "I")),
    (s"api-config.scopes.$mockScopeFour.filters", Seq("B")),
    (s"api-config.endpoints.internal.$endpointOne.key", endpointKeyOne),
    (s"api-config.endpoints.internal.$endpointOne.endpoint", "/internal/1"),
    (s"api-config.endpoints.internal.$endpointOne.title", "Get the first endpoint"),
    (s"api-config.endpoints.internal.$endpointOne.fields", Seq("A", "B", "C")),
    (s"api-config.endpoints.internal.$endpointTwo.key", endpointKeyTwo),
    (s"api-config.endpoints.internal.$endpointTwo.endpoint", "/internal/2"),
    (s"api-config.endpoints.internal.$endpointTwo.title", "Get the second endpoint"),
    (s"api-config.endpoints.internal.$endpointTwo.fields", Seq("D", "E", "F")),
    (s"api-config.endpoints.internal.$endpointThree.key", endpointKeyThree),
    (s"api-config.endpoints.internal.$endpointThree.endpoint", "/internal/3"),
    (s"api-config.endpoints.internal.$endpointThree.title", "Get the third endpoint"),
    (s"api-config.endpoints.internal.$endpointThree.fields", Seq("G", "H", "I")),
    (s"api-config.endpoints.internal.$endpointThree.filters", Seq("A", "B")),
    (s"api-config.endpoints.external.$endpointOne.key", endpointKeyOne),
    (s"api-config.endpoints.external.$endpointOne.endpoint", "/external/1"),
    (s"api-config.endpoints.external.$endpointOne.title", "Get the first endpoint"),
    (s"api-config.endpoints.external.$endpointTwo.key", endpointKeyTwo),
    (s"api-config.endpoints.external.$endpointTwo.endpoint", "/external/2"),
    (s"api-config.endpoints.external.$endpointTwo.title", "Get the second endpoint"),
    (s"api-config.endpoints.external.$endpointThree.key", endpointKeyThree),
    (s"api-config.endpoints.external.$endpointThree.endpoint", "/external/3"),
    (s"api-config.endpoints.external.$endpointThree.title", "Get the third endpoint"),
    (s"api-config.fields.A", "path/to/a"),
    (s"api-config.fields.B", "path/to/b"),
    (s"api-config.fields.C", "path/to/c"),
    (s"api-config.fields.D", "path/to/d"),
    (s"api-config.fields.E", "path/to/e"),
    (s"api-config.fields.F", "path/to/f"),
    (s"api-config.fields.G", "path/to/g"),
    (s"api-config.fields.H", "path/to/h"),
    (s"api-config.fields.I", "path/to/i"),
    (s"api-config.filters.A", "contains(path/to/g,'FILTERED_VALUE_1')"),
    (s"api-config.filters.B", "contains(path/to/g,'FILTERED_VALUE_2')"),
  )
}
