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

package uk.gov.hmrc.individualsmatchingapi.services

import javax.inject.Inject
import play.api.Configuration
import uk.gov.hmrc.individualsmatchingapi.config.{ApiConfig, EndpointConfig}

class ScopesService @Inject()(configuration: Configuration) {

  private[services] lazy val apiConfig =
    configuration.get[ApiConfig]("api-config")

  private[services] def getScopeItemsKeys(scope: String): Iterable[String] =
    apiConfig
      .getScope(scope)
      .map(s => s.endpoints)
      .getOrElse(List())

  def getAllScopes: Iterable[String] = apiConfig.scopes.map(_.name).sorted

  private[services] def getAccessibleEndpoints(scopes: Iterable[String]): Iterable[String] = {
    val scopeKeys = scopes.flatMap(s => getScopeItemsKeys(s)).toList
    apiConfig.endpoints
      .filter(endpoint => scopeKeys.contains(endpoint.key))
      .map(endpoint => endpoint.name)
  }

  def getEndpoints(scopes: Iterable[String]): Iterable[EndpointConfig] =
    getAccessibleEndpoints(scopes)
      .flatMap(endpoint => apiConfig.getEndpoint(endpoint))
}
