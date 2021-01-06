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

package uk.gov.hmrc.individualsmatchingapi.controllers.v2

import javax.inject.{Inject, Singleton}
import play.api.hal.Hal.{links, linksSeq, state}
import play.api.hal.{HalLink, HalResource}
import play.api.libs.json.JsValue
import play.api.mvc.hal._
import play.api.libs.json.Json.{obj, toJson}
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.individualsmatchingapi.domain.JsonFormatters.citizenDetailsFormat
import uk.gov.hmrc.individualsmatchingapi.controllers.Environment._
import uk.gov.hmrc.individualsmatchingapi.controllers.{CommonController, PrivilegedAuthentication}
import uk.gov.hmrc.individualsmatchingapi.play.RequestHeaderUtils.extractCorrelationId
import uk.gov.hmrc.individualsmatchingapi.services.{CitizenMatchingService, LiveCitizenMatchingService, SandboxCitizenMatchingService, ScopesService}

import scala.concurrent.ExecutionContext

abstract class PrivilegedIndividualsController(
  citizenMatchingService: CitizenMatchingService,
  scopeService: ScopesService,
  cc: ControllerComponents)(implicit val ec: ExecutionContext)
    extends CommonController(cc) with PrivilegedAuthentication {

  def matchedIndividual(matchId: String) = Action.async { implicit request =>
    extractCorrelationId(request)
    requiresPrivilegedAuthentication(scopeService.getAllScopes) { authScopes =>
      withUuid(matchId) { matchUuid =>
        citizenMatchingService.fetchCitizenDetailsByMatchId(matchUuid) map { citizenDetails =>
          val selfLink = HalLink("self", s"/individuals/matching/$matchId")
          val data = obj("individual" -> toJson(citizenDetails))
          Ok(state(data) ++ linksSeq(getApiLinks(matchId, authScopes) ++ Seq(selfLink)))
        }
      } recover recovery
    }
  }

  private def getApiLinks(matchId: String, scopes: Iterable[String]): Seq[HalLink] =
    scopeService
      .getEndpoints(scopes)
      .map(
        endpoint =>
          HalLink(
            endpoint.name,
            endpoint.link.replaceAllLiterally("<matchId>", matchId),
            name = Some("GET"),
            title = Some(endpoint.title)))
      .toSeq
}

@Singleton
class LivePrivilegedIndividualsController @Inject()(
  liveCitizenMatchingService: LiveCitizenMatchingService,
  scopeService: ScopesService,
  val authConnector: AuthConnector,
  cc: ControllerComponents)(override implicit val ec: ExecutionContext)
    extends PrivilegedIndividualsController(liveCitizenMatchingService, scopeService, cc) {
  override val environment = PRODUCTION
}

@Singleton
class SandboxPrivilegedIndividualsController @Inject()(
  sandboxCitizenMatchingService: SandboxCitizenMatchingService,
  scopeService: ScopesService,
  val authConnector: AuthConnector,
  cc: ControllerComponents)(override implicit val ec: ExecutionContext)
    extends PrivilegedIndividualsController(sandboxCitizenMatchingService, scopeService, cc) {
  override val environment = SANDBOX
}
