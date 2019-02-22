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

package unit.uk.gov.hmrc.individualsmatchingapi.controllers

import java.util.UUID

import org.mockito.Matchers.{any, refEq}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.individualsmatchingapi.controllers.MatchedCitizenController
import uk.gov.hmrc.individualsmatchingapi.domain.{MatchNotFoundException, MatchedCitizenRecord}
import uk.gov.hmrc.individualsmatchingapi.services.LiveCitizenMatchingService
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import play.api.http.Status._

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class MatchedCitizenControllerSpec extends UnitSpec with MockitoSugar with ScalaFutures with WithFakeApplication {
  implicit lazy val materializer = fakeApplication.materializer

  trait Setup {
    implicit val hc = HeaderCarrier()
    val matchId = UUID.randomUUID()
    val ninoString = "AA100009B"
    val matchedCitizenRecord = MatchedCitizenRecord(Nino(ninoString), matchId)
    val fakeRequest = FakeRequest()
    val mockCitizenMatchingService = mock[LiveCitizenMatchingService]

    val matchedCitizenController = new MatchedCitizenController(mockCitizenMatchingService)
  }

  "matched citizen controller" should {

    "return 200 (OK) for a valid matchId" in new Setup {
      when(mockCitizenMatchingService.fetchMatchedCitizenRecord(refEq(matchId))(any[HeaderCarrier])).thenReturn(matchedCitizenRecord)

      val result = await(matchedCitizenController.matchedCitizen(matchId.toString)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(
        s"""{"matchId": "$matchId", "nino": "$ninoString"}"""
      )
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      when(mockCitizenMatchingService.fetchMatchedCitizenRecord(refEq(matchId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new MatchNotFoundException))

      val result = await(matchedCitizenController.matchedCitizen(matchId.toString)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(
        s"""{"code":"NOT_FOUND","message":"The resource can not be found"}"""
      )
    }
  }
}
