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

import play.api.hal.Hal.linksSeq
import play.api.hal.{HalLink, HalResource}
import uk.gov.hmrc.individualsmatchingapi.config.EndpointConfig

import java.util.UUID
import javax.inject.Inject

class ScopesHelper @Inject() (scopesService: ScopesService) {

  def getHalLinks(
    matchId: UUID,
    excludeList: Option[List[String]],
    scopes: Iterable[String],
    allowedList: Option[List[String]]
  ): HalResource = {

    val links = getAllHalLinks(matchId, excludeList, allowedList, () => scopesService.getExternalEndpoints(scopes))

    linksSeq(links)
  }

  private def getAllHalLinks(
    matchId: UUID,
    excludeList: Option[List[String]],
    allowedList: Option[List[String]],
    getEndpoints: () => Iterable[EndpointConfig]
  ): Seq[HalLink] =
    getEndpoints()
      .filter(c =>
        !excludeList.getOrElse(List()).contains(c.name) &&
          allowedList.getOrElse(getEndpoints().map(e => e.name).toList).contains(c.name)
      )
      .map(endpoint =>
        HalLink(
          rel = endpoint.name,
          href = endpoint.link.replace("<matchId>", s"$matchId"),
          title = Some(endpoint.title)
        )
      )
      .toSeq
}
