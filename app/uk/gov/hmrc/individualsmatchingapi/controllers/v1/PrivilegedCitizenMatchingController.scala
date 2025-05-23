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

import play.api.hal.Hal.links
import play.api.hal.HalLink
import play.api.libs.json.JsValue
import play.api.mvc.hal.*
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.individualsmatchingapi.controllers.Environment.*
import uk.gov.hmrc.individualsmatchingapi.controllers.{CommonController, PrivilegedAuthentication}
import uk.gov.hmrc.individualsmatchingapi.domain.CitizenMatchingRequest
import uk.gov.hmrc.individualsmatchingapi.domain.JsonFormatters.citizenMatchingFormat
import uk.gov.hmrc.individualsmatchingapi.services.{CitizenMatchingService, LiveCitizenMatchingService, SandboxCitizenMatchingService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

abstract class PrivilegedCitizenMatchingController(
  liveCitizenMatchingService: CitizenMatchingService,
  cc: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends CommonController(cc) with PrivilegedAuthentication {

  def matchCitizen: Action[JsValue] = Action.async(parse.json) { implicit request =>
    requiresPrivilegedAuthentication {
      withJsonBody[CitizenMatchingRequest] { matchCitizen =>
        liveCitizenMatchingService.matchCitizen(matchCitizen) map { matchId =>
          val selfLink = HalLink("self", s"/individuals/matching/")
          val individualLink = HalLink(
            "individual",
            s"/individuals/matching/$matchId",
            name = Option("GET"),
            title = Option("Individual Details")
          )
          Ok(links(selfLink, individualLink))
        }
      } recover recovery
    }
  }
}

@Singleton
class LivePrivilegedCitizenMatchingController @Inject() (
  liveCitizenMatchingService: LiveCitizenMatchingService,
  val authConnector: AuthConnector,
  cc: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends PrivilegedCitizenMatchingController(liveCitizenMatchingService, cc) {
  override val environment: String = PRODUCTION
}

@Singleton
class SandboxPrivilegedCitizenMatchingController @Inject() (
  sandboxCitizenMatchingService: SandboxCitizenMatchingService,
  val authConnector: AuthConnector,
  cc: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends PrivilegedCitizenMatchingController(sandboxCitizenMatchingService, cc) {
  override val environment: String = SANDBOX
}
