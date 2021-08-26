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

package uk.gov.hmrc.individualsmatchingapi.services

import javax.inject.Inject
import play.api.Configuration
import uk.gov.hmrc.individualsmatchingapi.config.{ApiConfig, ExternalEndpointConfig, InternalEndpointConfig}

class ScopesService @Inject()(configuration: Configuration) {

  private[services] lazy val apiConfig =
    configuration.get[ApiConfig]("api-config")

  private[services] def getScopeFieldKeys(scope: String): List[String] =
    apiConfig
      .getScope(scope)
      .map(s => s.fields)
      .getOrElse(List())

  private[services] def getScopeFilterKeys(scope: String): List[String] =
    apiConfig
      .getScope(scope)
      .map(s => s.filters)
      .getOrElse(List())

  private[services] def getScopeEndpointKeys(scope: String): Iterable[String] =
    apiConfig
      .getScope(scope)
      .map(s => s.endpoints)
      .getOrElse(List())

  private[services] def getFieldPaths(keys: Iterable[String]): Iterable[String] =
    apiConfig.internalEndpoints
      .map(e => e.fields)
      .flatMap(value => keys.map(value.get))
      .flatten

  private[services] def getEndpointFieldKeys(endpointKey: String): Iterable[String] =
    apiConfig
      .getInternalEndpoint(endpointKey)
      .map(endpoint => endpoint.fields.keys.toList.sorted)
      .getOrElse(List())

  def getAllScopes: List[String] = apiConfig.scopes.map(_.name).sorted

  def getValidFilters(scopes: Iterable[String], endpoints: Iterable[String]): Iterable[String] = {
    val filterKeys = scopes.flatMap(getScopeFilterKeys).toList
    getInternalEndpoints(scopes).flatMap(endpoint =>
      endpoint.filters.filter(filterMap => filterKeys.contains(filterMap._1)).values)
  }

  def getIfDataPaths(scopes: Iterable[String], endpoints: List[String]): Set[String] = {
    val uniqueDataFields = scopes.flatMap(getScopeFieldKeys).toList.distinct
    val endpointDataItems = endpoints.flatMap(e => getEndpointFieldKeys(e).toSet)
    val authorizedDataItemsOnEndpoint = uniqueDataFields.filter(endpointDataItems.contains)
    getFieldPaths(authorizedDataItemsOnEndpoint).toSet
  }

  def getValidFieldsForCacheKey(scopes: Iterable[String], endpoints: Iterable[String]): String = {
    val uniqueDataFields = scopes.flatMap(getScopeFieldKeys).toList.distinct
    val endpointDataItems = endpoints.flatMap(e => getEndpointFieldKeys(e).toSet).toList
    val keys = uniqueDataFields.filter(endpointDataItems.contains)
    keys.nonEmpty match {
      case true => keys.reduce(_ + _)
      case _    => ""
    }
  }

  def getEndpointLink(endpoint: String): Option[String] =
    apiConfig.getInternalEndpoint(endpoint).map(c => c.link)

  def getInternalEndpoints(scopes: Iterable[String]): Iterable[InternalEndpointConfig] = {
    val scopeKeys = scopes.flatMap(s => getScopeFieldKeys(s)).toSeq
    apiConfig.internalEndpoints
      .filter(endpoint => endpoint.fields.keySet.exists(scopeKeys.contains))
      .map(endpoint => endpoint.name)
      .flatMap(endpoint => apiConfig.getInternalEndpoint(endpoint))
  }

  def getExternalEndpoints(scopes: Iterable[String]): Iterable[ExternalEndpointConfig] = {
    val scopeKeys = scopes.flatMap(s => getScopeEndpointKeys(s)).toSeq

    apiConfig.externalEndpoints
      .filter(endpoint => scopeKeys.contains(endpoint.key))
      .map(endpoint => endpoint.name)
      .flatMap(endpoint => apiConfig.getExternalEndpoint(endpoint))
  }

  def getEndPointScopes(endpointKey: String): Iterable[String] = {
    val keys = apiConfig
      .getInternalEndpoint(endpointKey)
      .map(endpoint => endpoint.fields.keys.toList.sorted)
      .getOrElse(List())

    apiConfig.scopes
      .filter(_.fields.toSet.intersect(keys.toSet).nonEmpty)
      .map(_.name)
      .sorted
  }
}
