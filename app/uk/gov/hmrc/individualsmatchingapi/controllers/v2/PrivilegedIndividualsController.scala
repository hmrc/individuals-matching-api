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

package uk.gov.hmrc.individualsmatchingapi.controllers.v2

import play.api.hal.Hal.state
import play.api.hal.HalLink
import play.api.libs.json.Json
import play.api.libs.json.Json.{obj, toJson}
import play.api.mvc.hal._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.individualsmatchingapi.audit.AuditHelper
import uk.gov.hmrc.individualsmatchingapi.controllers.Environment._
import uk.gov.hmrc.individualsmatchingapi.controllers.{CommonController, PrivilegedAuthentication}
import uk.gov.hmrc.individualsmatchingapi.domain.JsonFormatters.citizenDetailsFormat
import uk.gov.hmrc.individualsmatchingapi.play.RequestHeaderUtils.{maybeCorrelationId, validateCorrelationId}
import uk.gov.hmrc.individualsmatchingapi.services.{LiveCitizenMatchingService, ScopesHelper, ScopesService}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PrivilegedIndividualsController @Inject()(
  citizenMatchingService: LiveCitizenMatchingService,
  scopeService: ScopesService,
  scopesHelper: ScopesHelper,
  implicit private val auditHelper: AuditHelper,
  val authConnector: AuthConnector,
  cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends CommonController(cc) with PrivilegedAuthentication {

  def matchedIndividual(matchId: String): Action[AnyContent] = Action.async { implicit request =>
    authenticate(scopeService.getAllScopes, matchId) { authScopes =>
      val correlationId = validateCorrelationId(request)
      withValidUuid(matchId) { matchUuid =>
        citizenMatchingService.fetchCitizenDetailsByMatchId(matchUuid) map { citizenDetails =>
          val selfLink = HalLink("self", s"/individuals/matching/$matchId")
          val data = obj("individual" -> toJson(citizenDetails))
          val links = scopesHelper.getHalLinks(matchUuid, None, authScopes, None) ++ selfLink
          val response = state(data) ++ links

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

  val environment: String = PRODUCTION
}
