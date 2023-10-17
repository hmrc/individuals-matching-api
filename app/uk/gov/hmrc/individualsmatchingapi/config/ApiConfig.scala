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

package uk.gov.hmrc.individualsmatchingapi.config

import com.typesafe.config.{Config, ConfigValue}
import play.api.ConfigLoader
import play.twirl.api.TwirlHelperImports.twirlJavaCollectionToScala
import uk.gov.hmrc.individualsmatchingapi.services.PathTree

import java.util.Map.Entry
case class ApiConfig(scopes: List[ScopeConfig], externalEndpoints: List[ExternalEndpointConfig]) {

  def getScope(scope: String): Option[ScopeConfig] =
    scopes.find(c => c.name == scope)

  def getExternalEndpoint(endpoint: String): Option[ExternalEndpointConfig] =
    externalEndpoints.find(e => e.name == endpoint)
}

case class ScopeConfig(name: String, fields: List[String], endpoints: List[String], filters: List[String])

trait EndpointConfig {
  val name: String
  val link: String
  val title: String
}

case class ExternalEndpointConfig(
  override val name: String,
  override val link: String,
  override val title: String,
  key: String)
    extends EndpointConfig

object ApiConfig {

  implicit val configLoader: ConfigLoader[ApiConfig] = (rootConfig: Config, path: String) => {

    val config = rootConfig.getConfig(path)

    def parseConfig(path: String): Option[PathTree] =
      if (config.hasPath(path)) {
        val keys: List[String] = config
          .getConfig(path)
          .entrySet()
          .toSet
          .map((x: Entry[String, ConfigValue]) => x.getKey.replace("\"", ""))
          .toList
        Some(PathTree(keys, "\\."))
      } else None

    def getStringList(key: String): List[String] =
      if (config.hasPath(key))
        config.getStringList(key).toList
      else List()

    val extEndpointsOpt = parseConfig("endpoints.external")
    val externalEndpointConfig: List[ExternalEndpointConfig] =
      extEndpointsOpt
        .map(
          extEndpoints =>
            extEndpoints.listChildren
              .map(key =>
                ExternalEndpointConfig(
                  name = key,
                  key = config.getString(s"endpoints.external.$key.key"),
                  link = config.getString(s"endpoints.external.$key.endpoint"),
                  title = config.getString(s"endpoints.external.$key.title")
              ))
              .toList)
        .getOrElse(List())

    val scopesOpt = parseConfig("scopes")
    val scopeConfig = scopesOpt
      .map(
        scopes =>
          scopes.listChildren
            .map(key =>
              ScopeConfig(
                name = key,
                fields = getStringList(s"""scopes."$key".fields"""),
                endpoints = getStringList(s"""scopes."$key".endpoints"""),
                filters = getStringList(s"""scopes."$key".filters""")
            ))
            .toList)
      .getOrElse(List())

    ApiConfig(
      scopes = scopeConfig,
      externalEndpoints = externalEndpointConfig
    )
  }
}
