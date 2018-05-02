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

package unit.uk.gov.hmrc.individualsmatchingapi.services

import java.util.UUID

import org.joda.time.LocalDate
import org.mockito.Matchers.{any, refEq}
import org.mockito.Mockito.{verifyZeroInteractions, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.individualsmatchingapi.connectors.{CitizenDetailsConnector, MatchingConnector}
import uk.gov.hmrc.individualsmatchingapi.domain.SandboxData.sandboxMatchId
import uk.gov.hmrc.individualsmatchingapi.domain._
import uk.gov.hmrc.individualsmatchingapi.repository.NinoMatchRepository
import uk.gov.hmrc.individualsmatchingapi.services.{LiveCitizenMatchingService, SandboxCitizenMatchingService}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future.{failed, successful}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization

class CitizenMatchingServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  val authBearerToken = "AUTH_BEARER_TOKEN"
  val matchId = UUID.randomUUID()
  val ninoString = "NA000799C"
  val nino = Nino(ninoString)
  val ninoMatch = NinoMatch(nino, matchId)

  trait Setup {
    implicit val headers = HeaderCarrier().copy(authorization = Some(Authorization(s"Bearer $authBearerToken")))

    val mockNinoMatchRepository = mock[NinoMatchRepository]
    val mockCitizenDetailsConnector = mock[CitizenDetailsConnector]
    val mockMatchingConnector = mock[MatchingConnector]

    val liveService =
      new LiveCitizenMatchingService(mockNinoMatchRepository, mockCitizenDetailsConnector, mockMatchingConnector)

    val sandboxService = new SandboxCitizenMatchingService
  }

  "liveCitizenMatchingService match citizen function" should {

    val details = citizenDetails()
    val citizenMatchingRequest = aCitizenMatchingRequest()
    val detailsMatchRequest = aDetailsMatchRequest(citizenMatchingRequest, citizenDetails())

    "return matchId for a matched citizen" in new Setup {

      when(mockCitizenDetailsConnector.
        citizenDetails(refEq(ninoString))(any[HeaderCarrier])).thenReturn(successful(details))
      when(mockMatchingConnector.validateMatch(refEq(detailsMatchRequest))(any[HeaderCarrier])).
        thenReturn(successful(HasSucceeded))
      when(mockNinoMatchRepository.create(refEq(nino))).thenReturn(successful(ninoMatch))

      val result = await(liveService.matchCitizen(citizenMatchingRequest))

      result shouldBe matchId
    }

    "propagate exception when citizen details are not found" in new Setup {
      when(mockCitizenDetailsConnector.
        citizenDetails(refEq(ninoString))(any[HeaderCarrier])).thenReturn(failed(new CitizenNotFoundException))

      intercept[CitizenNotFoundException](await(liveService.matchCitizen(citizenMatchingRequest)))
      verifyZeroInteractions(mockMatchingConnector, mockNinoMatchRepository)
    }

    "propagate exception for an invalid nino" in new Setup {
      when(mockCitizenDetailsConnector.
        citizenDetails(refEq(ninoString))(any[HeaderCarrier])).thenReturn(failed(new InvalidNinoException))

      intercept[InvalidNinoException](await(liveService.matchCitizen(citizenMatchingRequest)))
    }

    "propagate exception for a non-match" in new Setup {
      when(mockCitizenDetailsConnector.
        citizenDetails(refEq(ninoString))(any[HeaderCarrier])).thenReturn(successful(details))
      when(mockMatchingConnector.validateMatch(refEq(detailsMatchRequest))(any[HeaderCarrier])).
        thenReturn(failed(new MatchingException))

      intercept[MatchingException](await(liveService.matchCitizen(citizenMatchingRequest)))
    }
  }

  "liveCitizenMatchingService fetch citizen details by matchId function" should {

    "throw a match not found exception when a match id fails to match a nino" in new Setup {
      when(mockNinoMatchRepository.read(matchId)).thenReturn(successful(None))
      intercept[MatchNotFoundException] {
        await(liveService.fetchCitizenDetailsByMatchId(matchId))
      }
    }

    "propagate a citizen not found exception when a nino fails to match a citizen" in new Setup {
      when(mockNinoMatchRepository.read(matchId)).thenReturn(successful(Some(ninoMatch)))
      when(mockCitizenDetailsConnector.citizenDetails(ninoString)).thenThrow(new CitizenNotFoundException)
      intercept[CitizenNotFoundException] {
        await(liveService.fetchCitizenDetailsByMatchId(matchId))
      }
    }

    "return citizen details when a match id matches a nino which matches a citizen" in new Setup {
      when(mockNinoMatchRepository.read(matchId)).thenReturn(successful(Some(ninoMatch)))
      when(mockCitizenDetailsConnector.citizenDetails(ninoString)).thenReturn(successful(citizenDetails()))
      await(liveService.fetchCitizenDetailsByMatchId(matchId)) shouldBe citizenDetails()
    }

  }

  "liveCitizenMatchingService fetch matched citizen record function" should {

    "return a matched citizen record for a valid matchId" in new Setup {
      val matchedCitizenRecord = MatchedCitizenRecord(Nino(ninoString), matchId)
      when(mockNinoMatchRepository.read(matchId)).thenReturn(successful(Some(ninoMatch)))

      val result = await(liveService.fetchMatchedCitizenRecord(matchId))
      result shouldBe matchedCitizenRecord
    }

    "throw an exception for an invalid matchId" in new Setup {
      when(mockNinoMatchRepository.read(matchId)).thenReturn(successful(None))
      intercept[MatchNotFoundException](await(liveService.fetchMatchedCitizenRecord(matchId)))
    }
  }

  "sandboxCitizenMatchingService match citizen function" should {

    "return default matchId for a successful match" in new Setup {
      val result = await(sandboxService.matchCitizen(aCitizenMatchingRequest()))

      result shouldBe sandboxMatchId
    }

    "return default matchId for a successful match on first letter of first name only" in new Setup {
      val result = await(sandboxService.matchCitizen(aCitizenMatchingRequest(firstName = "A")))

      result shouldBe sandboxMatchId
    }

    "return default matchId for a successful match on first three letters of last name only" in new Setup {
      val result = await(sandboxService.matchCitizen(aCitizenMatchingRequest(lastName = "Jos")))

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
      intercept[MatchingException](await(sandboxService.matchCitizen(aCitizenMatchingRequest(dateOfBirth = "1971-01-15"))))
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

  def aCitizenMatchingRequest(firstName: String = "Amanda",
                              lastName: String = "Joseph",
                              nino: String = ninoString,
                              dateOfBirth: String = "1960-01-15") = {
    CitizenMatchingRequest(firstName, lastName, nino, dateOfBirth)
  }

  def aDetailsMatchRequest(citizenMatchingRequest: CitizenMatchingRequest, citizenDetails: CitizenDetails) = {
    DetailsMatchRequest(citizenMatchingRequest, Seq(citizenDetails))
  }

  def citizenDetails(firstName: Option[String] = Some("Amanda"),
                     lastName: Option[String] = Some("Joseph"),
                     nino: Option[String] = Some(ninoString),
                     dateOfBirth: Option[String] = Some("1960-01-15")) = {
    CitizenDetails(firstName, lastName, nino, dateOfBirth.map(LocalDate.parse(_)))
  }
}
