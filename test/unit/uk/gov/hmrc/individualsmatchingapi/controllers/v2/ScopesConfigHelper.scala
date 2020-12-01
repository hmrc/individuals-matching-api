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

package unit.uk.gov.hmrc.individualsmatchingapi.controllers.v2

import play.api.Configuration

trait ScopesConfigHelper {

  val mockScopesConfig = Configuration(
    (s"api-config.scopes.test-scope.endpoints", List("A", "B", "C", "D")),
    (s"api-config.endpoints.benefits-and-credits.key", "A"),
    (s"api-config.endpoints.benefits-and-credits.endpoint", "/individuals/benefits-and-credits/?matchId=<matchId>"),
    (s"api-config.endpoints.benefits-and-credits.title", "View individual's benefits and credits"),
    (s"api-config.endpoints.details.key", "B"),
    (s"api-config.endpoints.details.endpoint", "/individuals/details/?matchId=<matchId>"),
    (s"api-config.endpoints.details.title", "View individual's details"),
    (s"api-config.endpoints.employments.key", "C"),
    (s"api-config.endpoints.employments.endpoint", "/individuals/employments/?matchId=<matchId>"),
    (s"api-config.endpoints.employments.title", "View individual's employments"),
    (s"api-config.endpoints.income.key", "D"),
    (s"api-config.endpoints.income.endpoint", "/individuals/income/?matchId=<matchId>"),
    (s"api-config.endpoints.income.title", "View individual's income")
  )
}
