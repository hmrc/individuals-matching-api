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

import org.slf4j.LoggerFactory
import play.api.hal.Hal.links
import play.api.hal.HalLink
import play.api.libs.json.Json
import play.api.mvc.hal._
import play.api.mvc.{BodyParsers, ControllerComponents}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.individualsmatchingapi.audit.AuditHelper
import uk.gov.hmrc.individualsmatchingapi.controllers.Environment._
import uk.gov.hmrc.individualsmatchingapi.controllers.{CommonController, PrivilegedAuthentication}
import uk.gov.hmrc.individualsmatchingapi.domain.CitizenMatchingRequest
import uk.gov.hmrc.individualsmatchingapi.domain.JsonFormatters._
import uk.gov.hmrc.individualsmatchingapi.play.RequestHeaderUtils.{maybeCorrelationId, validateCorrelationId}
import uk.gov.hmrc.individualsmatchingapi.services.{CitizenMatchingService, LiveCitizenMatchingService, SandboxCitizenMatchingService, ScopesService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

abstract class PrivilegedCitizenMatchingController(
  citizenMatchingService: CitizenMatchingService,
  scopeService: ScopesService,
  cc: ControllerComponents,
  implicit val auditHelper: AuditHelper)(implicit val ec: ExecutionContext)
    extends CommonController(cc) with PrivilegedAuthentication {

  val logger = LoggerFactory.getLogger(this.getClass)

  def matchCitizen = Action.async(BodyParsers.parse.json) { implicit request =>
    authenticate(scopeService.getAllScopes, request.body.toString()) { _ =>
      withJsonBodyV2[CitizenMatchingRequest] { matchCitizen =>
        val correlationId = validateCorrelationId(request)
        citizenMatchingService.matchCitizen(matchCitizen) map { matchId =>
          val selfLink = HalLink("self", s"/individuals/matching/")
          val individualLink = HalLink(
            "individual",
            s"/individuals/matching/$matchId",
            name = Option("GET"),
            title = Option("Get a matched individual’s information"))

          val response = links(selfLink, individualLink);

          auditHelper.auditApiResponse(
            correlationId.toString,
            matchId.toString,
            Some(scopeService.getAllScopes.mkString(",")),
            request,
            selfLink.toString,
            Json.toJson(response))

          Ok(links(selfLink, individualLink))
        }
      }
    } recover recoveryWithAudit(maybeCorrelationId(request), request.body.toString, "/individuals/matching/")
  }
}

@Singleton
class LivePrivilegedCitizenMatchingController @Inject()(
  liveCitizenMatchingService: LiveCitizenMatchingService,
  scopeService: ScopesService,
  val authConnector: AuthConnector,
  auditHelper: AuditHelper,
  cc: ControllerComponents)(override implicit val ec: ExecutionContext)
    extends PrivilegedCitizenMatchingController(liveCitizenMatchingService, scopeService, cc, auditHelper) {
  override val environment = PRODUCTION
}

@Singleton
class SandboxPrivilegedCitizenMatchingController @Inject()(
  sandboxCitizenMatchingService: SandboxCitizenMatchingService,
  scopeService: ScopesService,
  val authConnector: AuthConnector,
  auditHelper: AuditHelper,
  cc: ControllerComponents)(override implicit val ec: ExecutionContext)
    extends PrivilegedCitizenMatchingController(sandboxCitizenMatchingService, scopeService, cc, auditHelper) {
  override val environment = SANDBOX
}
