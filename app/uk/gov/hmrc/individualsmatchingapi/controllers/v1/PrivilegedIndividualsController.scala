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

package uk.gov.hmrc.individualsmatchingapi.controllers.v1

import play.api.hal.Hal.*
import play.api.libs.json.Json.{obj, toJson}
import play.api.mvc.hal.*
import play.api.hal.*
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.individualsmatchingapi.audit.AuditHelper
import uk.gov.hmrc.individualsmatchingapi.controllers.{CommonController, PrivilegedAuthentication}
import uk.gov.hmrc.individualsmatchingapi.domain.JsonFormatters.citizenDetailsFormat
import uk.gov.hmrc.individualsmatchingapi.play.RequestHeaderUtils.validateCorrelationId
import uk.gov.hmrc.individualsmatchingapi.services.{CitizenMatchingService, ScopesService}

import scala.concurrent.ExecutionContext

abstract class PrivilegedIndividualsController(
  citizenMatchingService: CitizenMatchingService,
  scopeService: ScopesService,
  cc: ControllerComponents,
  implicit private val auditHelper: AuditHelper
)(implicit executionContext: ExecutionContext)
    extends CommonController(cc) with PrivilegedAuthentication {

  def matchedIndividual(matchId: String): Action[AnyContent] = Action.async { implicit request =>
    authenticate(scopeService.v1Scopes, matchId) { authScopes =>
      val correlationId = validateCorrelationId(request)
      withValidUuid(matchId) { matchUuid =>
        citizenMatchingService.fetchCitizenDetailsByMatchId(matchUuid) map { citizenDetails =>
          val selfLink = HalLink("self", s"/individuals/matching/$matchId")
          val incomeLink = HalLink(
            "income",
            s"/individuals/income/?matchId=$matchId",
            name = Option("GET"),
            title = Option("View individual's income")
          )
          val employmentsLink: HalLink =
            HalLink(
              "employments",
              s"/individuals/employments/?matchId=$matchId",
              name = Option("GET"),
              title = Option("View individual's employments")
            )

          val response =
            state(obj("individual" -> toJson(citizenDetails))) ++ links(selfLink, incomeLink, employmentsLink)

          auditHelper.auditApiResponse(
            correlationId.toString,
            matchId,
            authScopes.mkString(","),
            request,
            selfLink.toString,
            Some(Json.toJson(response))
          )

          Ok(state(obj("individual" -> toJson(citizenDetails))) ++ links(selfLink, incomeLink, employmentsLink))

        }
      }
    }.recover(recovery)
  }
}
