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

import play.api.hal.Hal._
import play.api.hal.HalLink
import play.api.mvc.Action
import play.api.mvc.hal._
import uk.gov.hmrc.individualsmatchingapi.domain.JsonFormatters.citizenDetailsFormat
import uk.gov.hmrc.individualsmatchingapi.services.{CitizenMatchingService, SandboxCitizenMatchingService}
import scala.concurrent.ExecutionContext.Implicits.global

abstract class PrivilegedIndividualsController(citizenMatchingService: CitizenMatchingService) extends CommonController {

  def matchedIndividual(matchId: String) = Action.async { implicit request =>
    withUuid(matchId) { matchUuid =>
      citizenMatchingService.fetchCitizenDetailsByMatchId(matchUuid) map { citizenDetails =>
        val selfLink = HalLink("self", s"/individuals/matching/$matchId")
        val incomeLink = HalLink("income", s"/individuals/income?matchId=$matchId", name = Option("GET"), title = Option("View individual's income"))
        val employmentsLink = HalLink("employments", s"/individuals/employments?matchId=$matchId", name = Option("GET"), title = Option("View individual's employments"))
        Ok(state(citizenDetails) ++ links(selfLink, incomeLink, employmentsLink))
      } recover recovery
    }
  }
}

@Singleton
class SandboxPrivilegedIndividualsController @Inject()(sandboxCitizenMatchingService: SandboxCitizenMatchingService)
  extends PrivilegedIndividualsController(sandboxCitizenMatchingService)
