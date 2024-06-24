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

package uk.gov.hmrc.individualsmatchingapi.services

import play.api.Configuration
import uk.gov.hmrc.individualsmatchingapi.config.{ApiConfig, ExternalEndpointConfig}

import javax.inject.Inject

class ScopesService @Inject() (configuration: Configuration) {

  private lazy val apiConfig =
    configuration.get[ApiConfig]("api-config")

  private def getScopeEndpointKeys(scope: String): Iterable[String] =
    apiConfig
      .getScope(scope)
      .map(s => s.endpoints)
      .getOrElse(List())

  def getAllScopes: List[String] = apiConfig.scopes.map(_.name).sorted

  def getExternalEndpoints(scopes: Iterable[String]): Iterable[ExternalEndpointConfig] = {
    val scopeKeys = scopes.flatMap(s => getScopeEndpointKeys(s)).toSeq

    apiConfig.externalEndpoints
      .filter(endpoint => scopeKeys.contains(endpoint.key))
      .map(endpoint => endpoint.name)
      .flatMap(endpoint => apiConfig.getExternalEndpoint(endpoint))
  }
}
