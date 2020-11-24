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

package uk.gov.hmrc.individualsmatchingapi.controllers.v2

import javax.inject.{Inject, Singleton}
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.individualsmatchingapi.controllers.Environment._
import uk.gov.hmrc.individualsmatchingapi.controllers.{CommonController, PrivilegedAuthentication}
import uk.gov.hmrc.individualsmatchingapi.services.{CitizenMatchingService, LiveCitizenMatchingService, SandboxCitizenMatchingService, ScopesService}

import scala.concurrent.ExecutionContext.Implicits.global

abstract class PrivilegedIndividualsController(
  citizenMatchingService: CitizenMatchingService,
  scopeService: ScopesService,
  cc: ControllerComponents)
    extends CommonController(cc) with PrivilegedAuthentication {

  def matchedIndividual(matchId: String) = Action.async { implicit request =>
    val scopes = scopeService.getAllScopes
    requiresPrivilegedAuthentication(scopes)
      .flatMap { authScopes =>
        throw new Exception("NOT_IMPLEMENTED")
      }
      .recover(recovery)
  }
}

@Singleton
class LivePrivilegedIndividualsController @Inject()(
  liveCitizenMatchingService: LiveCitizenMatchingService,
  scopeService: ScopesService,
  val authConnector: AuthConnector,
  cc: ControllerComponents)
    extends PrivilegedIndividualsController(liveCitizenMatchingService, scopeService, cc) {
  override val environment = PRODUCTION
}

@Singleton
class SandboxPrivilegedIndividualsController @Inject()(
  sandboxCitizenMatchingService: SandboxCitizenMatchingService,
  scopeService: ScopesService,
  val authConnector: AuthConnector,
  cc: ControllerComponents)
    extends PrivilegedIndividualsController(sandboxCitizenMatchingService, scopeService, cc) {
  override val environment = SANDBOX
}
