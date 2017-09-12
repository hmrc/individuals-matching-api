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

package unit.uk.gov.hmrc.individualsmatchingapi.controllers

import java.util.UUID

import org.mockito.Matchers._
import org.mockito.Mockito.{verifyZeroInteractions, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.Results
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, status, _}
import uk.gov.hmrc.individualsmatchingapi.config.ServiceAuthConnector
import uk.gov.hmrc.individualsmatchingapi.controllers.SandboxPrivilegedIndividualsController
import uk.gov.hmrc.individualsmatchingapi.domain.MatchNotFoundException
import uk.gov.hmrc.individualsmatchingapi.services.SandboxCitizenMatchingService
import uk.gov.hmrc.play.http.HeaderCarrier
import unit.uk.gov.hmrc.individualsmatchingapi.util.Individuals

import scala.concurrent.Future.{failed, successful}

class PrivilegedIndividualsControllerSpec extends PlaySpec with Results with MockitoSugar with Individuals {

  trait Setup {
    val uuid = UUID.randomUUID()
    val mockSandboxCitizenMatchingService = mock[SandboxCitizenMatchingService]
    val mockAuthConnector = mock[ServiceAuthConnector]
    val individualsController = new SandboxPrivilegedIndividualsController(mockSandboxCitizenMatchingService, mockAuthConnector)

    implicit val headerCarrier = new HeaderCarrier()
  }

  "The matched individual function" should {

    "respond with http 404 (not found) for an invalid matchId" in new Setup {
      when(mockSandboxCitizenMatchingService.fetchCitizenDetailsByMatchId(refEq(uuid))(any[HeaderCarrier])).thenReturn(failed(new MatchNotFoundException))

      val eventualResult = individualsController.matchedIndividual(uuid.toString).apply(FakeRequest())
      status(eventualResult) mustBe NOT_FOUND
      contentAsJson(eventualResult) mustBe Json.parse("""{"code":"NOT_FOUND","message":"The resource can not be found"}""")
    }

    "respond with http 200 (ok) for a valid matchId and citizen details exist" in new Setup {
      when(mockSandboxCitizenMatchingService.fetchCitizenDetailsByMatchId(refEq(uuid))(any[HeaderCarrier])).thenReturn(successful(citizenDetails("Joe", "Bloggs", "AB123456C", "1960-01-15")))

      val eventualResult = individualsController.matchedIndividual(uuid.toString).apply(FakeRequest())
      status(eventualResult) mustBe OK
      contentAsJson(eventualResult) mustBe Json.parse(
        s"""
          {
            "_links":{
              "income":{
                "href":"/individuals/income?matchId=$uuid",
                "name":"GET",
                "title":"View individual's income"
              },
              "employments":{
                "href":"/individuals/employments?matchId=$uuid",
                "name":"GET",
                "title":"View individual's employments"
              },
              "self":{
                "href":"/individuals/matching/$uuid"
              }
            },
            "firstName":"Joe",
            "lastName":"Bloggs",
            "nino":"AB123456C",
            "dateOfBirth":"1960-01-15"
          }
        """)
    }

    "not require bearer token authentication" in new Setup {
      when(mockSandboxCitizenMatchingService.fetchCitizenDetailsByMatchId(refEq(uuid))(any[HeaderCarrier])).thenReturn(successful(citizenDetails("Joe", "Bloggs", "AB123456C", "1960-01-15")))

      val eventualResult = individualsController.matchedIndividual(uuid.toString).apply(FakeRequest())
      status(eventualResult) mustBe OK
      verifyZeroInteractions(mockAuthConnector)
    }
  }
}
