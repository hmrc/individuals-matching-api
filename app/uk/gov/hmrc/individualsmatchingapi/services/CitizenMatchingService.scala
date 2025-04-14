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

package uk.gov.hmrc.individualsmatchingapi.services

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsmatchingapi.connectors.{CitizenDetailsConnector, MatchingConnector}
import uk.gov.hmrc.individualsmatchingapi.domain._
import uk.gov.hmrc.individualsmatchingapi.repository.NinoMatchRepository

import java.time.LocalDate
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.{failed, successful}
import scala.concurrent.{ExecutionContext, Future}

trait CitizenMatchingService {
  def matchCitizen(citizenMatchingRequest: CitizenMatchingRequest)(implicit hc: HeaderCarrier): Future[UUID]

  def fetchCitizenDetailsByMatchId(matchId: UUID)(implicit hc: HeaderCarrier): Future[CitizenDetails]

  def fetchMatchedCitizenRecord(matchId: UUID)(implicit hc: HeaderCarrier): Future[MatchedCitizenRecord]
}

@Singleton
class LiveCitizenMatchingService @Inject() (
  liveNinoMatchRepository: NinoMatchRepository,
  citizenDetailsConnector: CitizenDetailsConnector,
  matchingConnector: MatchingConnector
)(implicit executionContext: ExecutionContext)
    extends CitizenMatchingService {

  override def matchCitizen(citizenMatchingRequest: CitizenMatchingRequest)(implicit hc: HeaderCarrier): Future[UUID] =
    for {
      details   <- citizenDetailsConnector.citizenDetails(citizenMatchingRequest.nino)
      _         <- matchingConnector.validateMatch(DetailsMatchRequest(citizenMatchingRequest, Seq(details)))
      ninoMatch <- liveNinoMatchRepository.create(Nino(citizenMatchingRequest.nino))
    } yield ninoMatch.id

  override def fetchCitizenDetailsByMatchId(matchId: UUID)(implicit hc: HeaderCarrier): Future[CitizenDetails] =
    liveNinoMatchRepository.read(matchId) flatMap {
      case Some(ninoMatch) =>
        citizenDetailsConnector.citizenDetails(ninoMatch.nino.nino)
      case _ => failed(new MatchNotFoundException)
    }

  override def fetchMatchedCitizenRecord(matchId: UUID)(implicit hc: HeaderCarrier): Future[MatchedCitizenRecord] =
    liveNinoMatchRepository.read(matchId) flatMap {
      case Some(ninoMatch) =>
        successful(MatchedCitizenRecord(ninoMatch.nino, ninoMatch.id))
      case _ => failed(new MatchNotFoundException)
    }
}

@Singleton
class SandboxCitizenMatchingService extends CitizenMatchingService {

  override def matchCitizen(
    citizenMatchingRequest: CitizenMatchingRequest
  )(implicit hc: HeaderCarrier): Future[UUID] = {

    def firstNLetters(length: Int, value: String): String =
      value.trim.take(length)

    def isFirstNameMatch(requestName: String, individualName: String) =
      firstNLetters(1, requestName) equalsIgnoreCase firstNLetters(1, individualName)

    def isLastNameMatch(requestLastName: String, individualLastName: String) =
      firstNLetters(3, requestLastName) equalsIgnoreCase firstNLetters(3, individualLastName)

    def isMatch(individual: Individual) =
      isFirstNameMatch(citizenMatchingRequest.firstName, individual.firstName) &&
        isLastNameMatch(citizenMatchingRequest.lastName, individual.lastName) &&
        citizenMatchingRequest.nino == individual.nino &&
        LocalDate.parse(citizenMatchingRequest.dateOfBirth) == individual.dateOfBirth

    SandboxData.findByNino(citizenMatchingRequest.nino) match {
      case Some(data) if isMatch(data) => successful(data.matchId)
      case _                           => failed(new MatchingException)
    }
  }

  override def fetchCitizenDetailsByMatchId(matchId: UUID)(implicit hc: HeaderCarrier): Future[CitizenDetails] =
    SandboxData.findByMatchId(matchId) match {
      case Some(individual) =>
        successful(
          CitizenDetails(
            Option(individual.firstName),
            Option(individual.lastName),
            Option(individual.nino),
            Option(individual.dateOfBirth)
          )
        )
      case _ => failed(new MatchNotFoundException)
    }

  override def fetchMatchedCitizenRecord(matchId: UUID)(implicit hc: HeaderCarrier): Future[MatchedCitizenRecord] =
    failed(new NotImplementedException)
}
