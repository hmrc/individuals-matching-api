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

package unit.uk.gov.hmrc.individualsmatchingapi.services

import java.time.LocalDate
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.IdiomaticMockito
import org.mockito.Mockito.verifyNoInteractions
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsmatchingapi.connectors.{CitizenDetailsConnector, MatchingConnector}
import uk.gov.hmrc.individualsmatchingapi.domain.SandboxData.sandboxMatchId
import uk.gov.hmrc.individualsmatchingapi.domain._
import uk.gov.hmrc.individualsmatchingapi.repository.NinoMatchRepository
import uk.gov.hmrc.individualsmatchingapi.services.{LiveCitizenMatchingService, SandboxCitizenMatchingService}
import unit.uk.gov.hmrc.individualsmatchingapi.support.SpecBase

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class CitizenMatchingServiceSpec(implicit executionContext: ExecutionContext)
    extends SpecBase with Matchers with IdiomaticMockito with ScalaFutures {

  val authBearerToken = "AUTH_BEARER_TOKEN"
  val matchId: UUID = UUID.randomUUID()
  val ninoString = "NA000799C"
  val nino: Nino = Nino(ninoString)
  val ninoMatch: NinoMatch = NinoMatch(nino, matchId)

  trait Setup {

    implicit val headers: HeaderCarrier = HeaderCarrier()

    val mockNinoMatchRepository: NinoMatchRepository = mock[NinoMatchRepository]
    val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
    val mockMatchingConnector: MatchingConnector = mock[MatchingConnector]

    val liveService =
      new LiveCitizenMatchingService(mockNinoMatchRepository, mockCitizenDetailsConnector, mockMatchingConnector)

    val sandboxService = new SandboxCitizenMatchingService
  }

  "liveCitizenMatchingService match citizen function" should {

    val details = citizenDetails()
    val citizenMatchingRequest = aCitizenMatchingRequest()
    val detailsMatchRequest = aDetailsMatchRequest(citizenMatchingRequest, citizenDetails())

    "return matchId for a matched citizen" in new Setup {

      mockCitizenDetailsConnector
        .citizenDetails(refEq(ninoString))(any[HeaderCarrier])
        .returns(Future.successful(details))
      mockMatchingConnector.validateMatch(refEq(detailsMatchRequest))(any[HeaderCarrier]).returns(Future.successful(()))
      mockNinoMatchRepository.create(refEq(nino)).returns(Future.successful(ninoMatch))

      val result: UUID = await(liveService.matchCitizen(citizenMatchingRequest))

      result shouldBe matchId
    }

    "propagate exception when citizen details are not found" in new Setup {
      mockCitizenDetailsConnector
        .citizenDetails(refEq(ninoString))(any[HeaderCarrier])
        .returns(Future.failed(new CitizenNotFoundException))

      intercept[CitizenNotFoundException](await(liveService.matchCitizen(citizenMatchingRequest)))
      verifyNoInteractions(mockMatchingConnector, mockNinoMatchRepository)
    }

    "propagate exception for an invalid nino" in new Setup {
      mockCitizenDetailsConnector
        .citizenDetails(refEq(ninoString))(any[HeaderCarrier])
        .returns(Future.failed(new InvalidNinoException))

      intercept[InvalidNinoException](await(liveService.matchCitizen(citizenMatchingRequest)))
    }

    "propagate exception for a non-match" in new Setup {
      mockCitizenDetailsConnector
        .citizenDetails(refEq(ninoString))(any[HeaderCarrier])
        .returns(Future.successful(details))
      mockMatchingConnector
        .validateMatch(refEq(detailsMatchRequest))(any[HeaderCarrier])
        .returns(Future.failed(new MatchingException))

      intercept[MatchingException](await(liveService.matchCitizen(citizenMatchingRequest)))
    }
  }

  "liveCitizenMatchingService fetch citizen details by matchId function" should {

    "throw a match not found exception when a match id fails to match a nino" in new Setup {
      mockNinoMatchRepository.read(matchId).returns(Future.successful(None))
      intercept[MatchNotFoundException] {
        await(liveService.fetchCitizenDetailsByMatchId(matchId))
      }
    }

    "propagate a citizen not found exception when a nino fails to match a citizen" in new Setup {
      mockNinoMatchRepository.read(matchId).returns(Future.successful(Some(ninoMatch)))
      mockCitizenDetailsConnector.citizenDetails(ninoString).throws(new CitizenNotFoundException)
      intercept[CitizenNotFoundException] {
        await(liveService.fetchCitizenDetailsByMatchId(matchId))
      }
    }

    "return citizen details when a match id matches a nino which matches a citizen" in new Setup {
      mockNinoMatchRepository.read(matchId).returns(Future.successful(Some(ninoMatch)))
      mockCitizenDetailsConnector.citizenDetails(ninoString).returns(Future.successful(citizenDetails()))
      await(liveService.fetchCitizenDetailsByMatchId(matchId)) shouldBe citizenDetails()
    }

  }

  "liveCitizenMatchingService fetch matched citizen record function" should {

    "return a matched citizen record for a valid matchId" in new Setup {
      val matchedCitizenRecord: MatchedCitizenRecord = MatchedCitizenRecord(Nino(ninoString), matchId)
      mockNinoMatchRepository.read(matchId).returns(Future.successful(Some(ninoMatch)))

      val result: MatchedCitizenRecord = await(liveService.fetchMatchedCitizenRecord(matchId))
      result shouldBe matchedCitizenRecord
    }

    "throw an exception for an invalid matchId" in new Setup {
      mockNinoMatchRepository.read(matchId).returns(Future.successful(None))
      intercept[MatchNotFoundException](await(liveService.fetchMatchedCitizenRecord(matchId)))
    }
  }

  "sandboxCitizenMatchingService match citizen function" should {

    "return default matchId for a successful match" in new Setup {
      val result: UUID = await(sandboxService.matchCitizen(aCitizenMatchingRequest()))

      result shouldBe sandboxMatchId
    }

    "return default matchId for a successful match on first letter of first name only" in new Setup {
      val result: UUID = await(sandboxService.matchCitizen(aCitizenMatchingRequest(firstName = "A")))

      result shouldBe sandboxMatchId
    }

    "return default matchId for a successful match on first three letters of last name only" in new Setup {
      val result: UUID = await(sandboxService.matchCitizen(aCitizenMatchingRequest(lastName = "Jos")))

      result shouldBe sandboxMatchId
    }

    "throw matching exception for an invalid firstName" in new Setup {
      intercept[MatchingException](await(sandboxService.matchCitizen(aCitizenMatchingRequest(firstName = "Sarah"))))
    }

    "throw matching exception for an invalid lastName" in new Setup {
      intercept[MatchingException](await(sandboxService.matchCitizen(aCitizenMatchingRequest(lastName = "Connor"))))
    }

    "throw matching exception for an invalid first letter of firstName" in new Setup {
      intercept[MatchingException](await(sandboxService.matchCitizen(aCitizenMatchingRequest(firstName = "B"))))
    }

    "throw matching exception for invalid first three letters of lastName" in new Setup {
      intercept[MatchingException](await(sandboxService.matchCitizen(aCitizenMatchingRequest(lastName = "Jopeph"))))
    }

    "throw matching exception for an invalid nino" in new Setup {
      intercept[MatchingException](await(sandboxService.matchCitizen(aCitizenMatchingRequest(nino = "AA000799D"))))
    }

    "throw matching exception for an invalid dateOfBirth" in new Setup {
      intercept[MatchingException](
        await(sandboxService.matchCitizen(aCitizenMatchingRequest(dateOfBirth = "1971-01-15")))
      )
    }
  }

  "sandboxCitizenMatchingService fetch citizen details by matchId function" should {

    "throw a match not found exception when a matchId fails to match sandbox employment data" in new Setup {
      intercept[MatchNotFoundException] {
        await(sandboxService.fetchCitizenDetailsByMatchId(UUID.randomUUID()))
      }
    }

    "return citizen details when a nino matches sandbox employment data" in new Setup {
      await(sandboxService.fetchCitizenDetailsByMatchId(sandboxMatchId)) shouldBe citizenDetails()
    }
  }

  "SandboxCitizenMatchingService fetch matched citizen record function" should {
    "throw a not implemented exception" in new Setup {
      intercept[NotImplementedException](await(sandboxService.fetchMatchedCitizenRecord(sandboxMatchId)))
    }
  }

  def aCitizenMatchingRequest(
    firstName: String = "Amanda",
    lastName: String = "Joseph",
    nino: String = ninoString,
    dateOfBirth: String = "1960-01-15"
  ): CitizenMatchingRequest =
    CitizenMatchingRequest(firstName, lastName, nino, dateOfBirth)

  def aDetailsMatchRequest(
    citizenMatchingRequest: CitizenMatchingRequest,
    citizenDetails: CitizenDetails
  ): DetailsMatchRequest =
    DetailsMatchRequest(citizenMatchingRequest, Seq(citizenDetails))

  def citizenDetails(
    firstName: Option[String] = Some("Amanda"),
    lastName: Option[String] = Some("Joseph"),
    nino: Option[String] = Some(ninoString),
    dateOfBirth: Option[String] = Some("1960-01-15")
  ): CitizenDetails =
    CitizenDetails(firstName, lastName, nino, dateOfBirth.map(LocalDate.parse))
}
