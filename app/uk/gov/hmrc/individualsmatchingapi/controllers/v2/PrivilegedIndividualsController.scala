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

import play.api.hal.Hal.{linksSeq, state}
import play.api.hal.HalLink
import play.api.libs.json.Json
import play.api.libs.json.Json.{obj, toJson}
import play.api.mvc.ControllerComponents
import play.api.mvc.hal._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.individualsmatchingapi.audit.AuditHelper
import uk.gov.hmrc.individualsmatchingapi.controllers.Environment._
import uk.gov.hmrc.individualsmatchingapi.controllers.{CommonController, PrivilegedAuthentication}
import uk.gov.hmrc.individualsmatchingapi.domain.JsonFormatters.citizenDetailsFormat
import uk.gov.hmrc.individualsmatchingapi.play.RequestHeaderUtils.{maybeCorrelationId, validateCorrelationId}
import uk.gov.hmrc.individualsmatchingapi.services.{CitizenMatchingService, ScopesService}

import scala.concurrent.ExecutionContext

class PrivilegedIndividualsController(
  citizenMatchingService: CitizenMatchingService,
  scopeService: ScopesService,
  implicit val auditHelper: AuditHelper,
  val authConnector: AuthConnector,
  cc: ControllerComponents)(implicit val ec: ExecutionContext)
    extends CommonController(cc) with PrivilegedAuthentication {

  def matchedIndividual(matchId: String) = Action.async { implicit request =>
    authenticate(scopeService.getAllScopes, matchId) { authScopes =>
      val correlationId = validateCorrelationId(request)
      withUuid(matchId) { matchUuid =>
        citizenMatchingService.fetchCitizenDetailsByMatchId(matchUuid) map { citizenDetails =>
          val selfLink = HalLink("self", s"/individuals/matching/$matchId")
          val data = obj("individual" -> toJson(citizenDetails))
          val response = state(data) ++ linksSeq(getApiLinks(matchId, authScopes) ++ Seq(selfLink))

          auditHelper.auditApiResponse(
            correlationId.toString,
            matchId,
            authScopes.mkString(","),
            request,
            selfLink.toString,
            Some(Json.toJson(response)))

          Ok(response)
        }
      }
    } recover recoveryWithAudit(maybeCorrelationId(request), request.body.toString, s"/individuals/matching/$matchId")
  }

  val environment = PRODUCTION

  private def getApiLinks(matchId: String, scopes: Iterable[String]): Seq[HalLink] =
    scopeService
      .getEndpoints(scopes)
      .map(endpoint =>
        HalLink(endpoint.name, endpoint.link.replaceAllLiterally("<matchId>", matchId), title = Some(endpoint.title)))
      .toSeq
}
