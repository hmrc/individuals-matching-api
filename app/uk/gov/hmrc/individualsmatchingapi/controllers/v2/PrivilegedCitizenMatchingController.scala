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

import play.api.hal.*
import play.api.hal.Hal.links
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.hal.*
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.individualsmatchingapi.audit.AuditHelper
import uk.gov.hmrc.individualsmatchingapi.controllers.Environment.*
import uk.gov.hmrc.individualsmatchingapi.controllers.{CommonController, PrivilegedAuthentication}
import uk.gov.hmrc.individualsmatchingapi.domain.CitizenMatchingRequest
import uk.gov.hmrc.individualsmatchingapi.domain.JsonFormatters.*
import uk.gov.hmrc.individualsmatchingapi.play.RequestHeaderUtils.{maybeCorrelationId, validateCorrelationId}
import uk.gov.hmrc.individualsmatchingapi.services.{LiveCitizenMatchingService, ScopesService}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PrivilegedCitizenMatchingController @Inject() (
  citizenMatchingService: LiveCitizenMatchingService,
  scopeService: ScopesService,
  val authConnector: AuthConnector,
  cc: ControllerComponents,
  implicit private val auditHelper: AuditHelper
)(implicit ec: ExecutionContext)
    extends CommonController(cc) with PrivilegedAuthentication {

  def matchCitizen: Action[JsValue] = Action.async(parse.json) { implicit request =>
    authenticate(scopeService.getAllScopes, request.body.toString()) { authScopes =>
      withJsonBodyV2[CitizenMatchingRequest] { matchCitizen =>
        val correlationId = validateCorrelationId(request)
        citizenMatchingService.matchCitizen(matchCitizen) map { matchId =>
          val selfLink = HalLink("self", s"/individuals/matching/")
          val individualLink = HalLink(
            "individual",
            s"/individuals/matching/$matchId",
            title = Option("Get a matched individual’s information")
          )

          val response = links(selfLink, individualLink)

          auditHelper.auditApiResponse(
            correlationId.toString,
            matchId.toString,
            authScopes.mkString(","),
            request,
            selfLink.toString,
            Some(Json.toJson(response))
          )

          Ok(response)
        }
      }
    } recover recoveryWithAudit(maybeCorrelationId(request), request.body.toString, "/individuals/matching/")
  }
  val environment: String = PRODUCTION
}
