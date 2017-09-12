/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.individualsmatchingapi.controllers

import javax.inject.{Inject, Singleton}

import play.api.libs.json.Json
import play.api.mvc.{Action, BodyParsers}
import uk.gov.hmrc.individualsmatchingapi.config.ServiceAuthConnector
import uk.gov.hmrc.individualsmatchingapi.controllers.Environment.SANDBOX
import uk.gov.hmrc.individualsmatchingapi.domain.CitizenMatchingRequest
import uk.gov.hmrc.individualsmatchingapi.domain.JsonFormatters.citizenMatchingFormat
import uk.gov.hmrc.individualsmatchingapi.services.{CitizenMatchingService, SandboxCitizenMatchingService}

import scala.concurrent.ExecutionContext.Implicits.global

abstract class PrivilegedCitizenMatchingController(citizenMatchingService: CitizenMatchingService) extends CommonController with PrivilegedAuthentication {

  private def seeOthers(location: String) = new Status(SEE_OTHER)(Json.obj()).withHeaders(LOCATION -> location)

  def matchCitizen = Action.async(BodyParsers.parse.json) { implicit request =>
    requiresPrivilegedAuthentication {
      withJsonBody[CitizenMatchingRequest] { matchCitizen =>
        citizenMatchingService.matchCitizen(matchCitizen) map (id => seeOthers(s"/individuals/matching/$id"))
      } recover recovery
    }
  }
}

@Singleton
class SandboxPrivilegedCitizenMatchingController @Inject()(sandboxCitizenMatchingService: SandboxCitizenMatchingService, val authConnector: ServiceAuthConnector)
  extends PrivilegedCitizenMatchingController(sandboxCitizenMatchingService) {
  override val environment = SANDBOX
}