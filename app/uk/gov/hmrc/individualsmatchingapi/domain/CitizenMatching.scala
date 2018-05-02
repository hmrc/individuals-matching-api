/*
 * Copyright 2018 HM Revenue & Customs
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
import org.joda.time.format.DateTimeFormat
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.individualsmatchingapi.domain.Validators.fieldRequired

import scala.util.Try

case class CitizenMatchingRequest(firstName: String, lastName: String, nino: String, dateOfBirth: String) {
  fieldRequired(firstName.nonEmpty, "firstName is required")
  fieldRequired(lastName.nonEmpty, "lastName is required")
  fieldRequired(nino.nonEmpty, "nino is required")
  fieldRequired(Try(Nino(nino)).isSuccess, "Malformed nino submitted")
  fieldRequired(dateOfBirth.nonEmpty, "dateOfBirth is required")
  fieldRequired(Try(LocalDate.parse(dateOfBirth, DateTimeFormat.forPattern("yyyy-MM-dd"))).isSuccess, "dateOfBirth: invalid date format")
}

case class CitizenDetails(firstName: Option[String], lastName: Option[String], nino: Option[String], dateOfBirth: Option[LocalDate])

case class DetailsMatchRequest(verifyPerson: CitizenMatchingRequest, cidPersons: Seq[CitizenDetails])

case class MatchedCitizenRecord(nino: Nino, matchId: UUID)
