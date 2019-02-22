/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.individualsmatchingapi.domain

import java.util.UUID

import org.joda.time.LocalDate
import org.joda.time.LocalDate._
import uk.gov.hmrc.domain.Nino

case class Individual(matchId: UUID,
                      nino: String,
                      firstName: String,
                      lastName: String,
                      dateOfBirth: LocalDate)

object SandboxData {

  val sandboxNino = Nino("NA000799C")

  val sandboxMatchId = UUID.fromString("57072660-1df9-4aeb-b4ea-cd2d7f96e430")

  def findByMatchId(matchId: UUID) = individuals.find(_.matchId == matchId)

  def findByNino(nino: String) = individuals.find(_.nino == nino)

  private lazy val individuals = Seq(amanda())

  private def amanda() = Individual(
    sandboxMatchId,
    sandboxNino.nino,
    "Amanda",
    "Joseph",
    parse("1960-01-15"))
}
