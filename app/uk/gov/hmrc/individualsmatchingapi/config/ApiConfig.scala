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

package uk.gov.hmrc.individualsmatchingapi.config

import com.typesafe.config.Config
import play.api.ConfigLoader

import scala.collection.JavaConverters._

case class ApiConfig(scopes: List[ScopeConfig], endpoints: List[EndpointConfig]) {

  def getScope(scope: String): Option[ScopeConfig] =
    scopes.find(c => c.name == scope)

  def getEndpoint(endpoint: String): Option[EndpointConfig] =
    endpoints.find(e => e.name == endpoint)

}

case class ScopeConfig(name: String, endpoints: List[String]) {}

case class EndpointConfig(key: String, name: String, link: String, title: String)

object ApiConfig {

  implicit val configLoader: ConfigLoader[ApiConfig] =
    (rootConfig: Config, path: String) => {
      val config = rootConfig.getConfig(path)

      def getKeys(path2: String): Iterable[String] =
        config
          .getConfig(path2)
          .entrySet()
          .asScala
          .map(x => x.getKey.replaceAllLiterally("\"", ""))
          .map(x => x.split("\\.").head)

      val endpointConfig: List[EndpointConfig] = getKeys("endpoints")
        .map(
          key =>
            EndpointConfig(
              key = config.getString(s"endpoints.$key.key"),
              name = key,
              link = config.getString(s"endpoints.$key.endpoint"),
              title = config.getString(s"endpoints.$key.title")
          ))
        .toList

      val scopesConfig: List[ScopeConfig] = getKeys("scopes")
        .map(key =>
          ScopeConfig(name = key, endpoints = config.getStringList(s"""scopes."$key".endpoints""").asScala.toList))
        .toList

      ApiConfig(
        scopes = scopesConfig,
        endpoints = endpointConfig
      )
    }
}
