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

package uk.gov.hmrc.individualsmatchingapi.config

import com.typesafe.config.Config
import play.api.ConfigLoader
import uk.gov.hmrc.individualsmatchingapi.services.PathTree

import scala.collection.JavaConverters._

case class ApiConfig(scopes: List[ScopeConfig], endpoints: List[EndpointConfig]) {

  def getScope(scope: String): Option[ScopeConfig] =
    scopes.find(c => c.name == scope)

  def getEndpoint(endpoint: String): Option[EndpointConfig] =
    endpoints.find(e => e.name == endpoint)

}

case class ScopeConfig(name: String, fields: List[String]) {}

case class EndpointConfig(name: String, link: String, fields: Map[String, String])

object ApiConfig {

  implicit val configLoader: ConfigLoader[ApiConfig] =
    new ConfigLoader[ApiConfig] {
      def load(rootConfig: Config, path: String): ApiConfig = {
        val config = rootConfig.getConfig(path)

        def parseConfig(path: String): PathTree = {
          val keys: List[String] = config
            .getConfig(path)
            .entrySet()
            .asScala
            .map(x => x.getKey.replaceAllLiterally("\"", ""))
            .toList

          PathTree(keys, "\\.")
        }

        val endpointTree = parseConfig("endpoints")
        val endpointConfig: List[EndpointConfig] = endpointTree.listChildren
          .flatMap(
            key =>
              endpointTree
                .getChild(key)
                .flatMap(node => node.getChild("fields"))
                .map(node =>
                  EndpointConfig(
                    name = key,
                    link = config.getString(s"endpoints.$key.endpoint"),
                    fields = node.listChildren.toList.sorted
                      .map(field => (field, config.getString(s"endpoints.$key.fields.$field")))
                      .toMap
                )))
          .toList

        val scopeTree = parseConfig("scopes")
        val scopeConfig = scopeTree.listChildren
          .map(key =>
            ScopeConfig(name = key, fields = config.getStringList(s"""scopes."$key".fields""").asScala.toList))
          .toList

        ApiConfig(
          scopes = scopeConfig,
          endpoints = endpointConfig
        )
      }
    }
}
