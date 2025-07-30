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

package unit.uk.gov.hmrc.individualsmatchingapi.controllers

import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsmatchingapi.controllers.MatchedCitizenController
import uk.gov.hmrc.individualsmatchingapi.domain.{MatchNotFoundException, MatchedCitizenRecord}
import uk.gov.hmrc.individualsmatchingapi.services.LiveCitizenMatchingService
import unit.uk.gov.hmrc.individualsmatchingapi.support.SpecBase

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MatchedCitizenControllerSpec extends SpecBase with Matchers with MockitoSugar {
  implicit lazy val materializer: Materializer = app.materializer

  trait Setup {
    val matchId: UUID = UUID.randomUUID()
    val ninoString: String = "AA100009B"
    val matchedCitizenRecord: MatchedCitizenRecord =
      MatchedCitizenRecord(Nino(ninoString), matchId)
    val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    val mockCitizenMatchingService: LiveCitizenMatchingService =
      mock[LiveCitizenMatchingService]
    val controllerComponents: ControllerComponents =
      app.injector.instanceOf[ControllerComponents]

    val matchedCitizenController = new MatchedCitizenController(controllerComponents, mockCitizenMatchingService)
  }

  "matched citizen controller" should {

    "return 200 (OK) for a valid matchId" in new Setup {
      when(
        mockCitizenMatchingService
          .fetchMatchedCitizenRecord(eqTo(matchId))(using any[HeaderCarrier])
      )
        .thenReturn(Future.successful(matchedCitizenRecord))

      val result: Future[Result] = matchedCitizenController.matchedCitizen(matchId.toString)(fakeRequest)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.parse(
        s"""{"matchId": "$matchId", "nino": "$ninoString"}"""
      )
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      when(
        mockCitizenMatchingService
          .fetchMatchedCitizenRecord(eqTo(matchId))(using any[HeaderCarrier])
      ).thenReturn(Future.failed(new MatchNotFoundException))

      val result: Future[Result] = matchedCitizenController.matchedCitizen(matchId.toString)(fakeRequest)

      status(result) shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.parse(
        s"""{"code":"NOT_FOUND","message":"The resource can not be found"}"""
      )
    }
  }
}
