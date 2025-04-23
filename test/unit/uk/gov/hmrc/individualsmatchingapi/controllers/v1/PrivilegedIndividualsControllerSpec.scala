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

package unit.uk.gov.hmrc.individualsmatchingapi.controllers.v1

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verifyNoInteractions, when}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, InsufficientEnrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsmatchingapi.controllers.v1.{LivePrivilegedIndividualsController, SandboxPrivilegedIndividualsController}
import uk.gov.hmrc.individualsmatchingapi.domain.MatchNotFoundException
import uk.gov.hmrc.individualsmatchingapi.domain.SandboxData.sandboxMatchId
import uk.gov.hmrc.individualsmatchingapi.services.{LiveCitizenMatchingService, SandboxCitizenMatchingService}
import unit.uk.gov.hmrc.individualsmatchingapi.support.SpecBase
import unit.uk.gov.hmrc.individualsmatchingapi.util.Individuals

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class PrivilegedIndividualsControllerSpec extends SpecBase with Matchers with MockitoSugar with Individuals {

  val uuid: UUID = UUID.randomUUID()

  trait Setup {

    val mockCitizenMatchingService: LiveCitizenMatchingService = mock[LiveCitizenMatchingService]
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val controllerComponents: ControllerComponents =
      app.injector.instanceOf[ControllerComponents]

    val liveController: LivePrivilegedIndividualsController =
      new LivePrivilegedIndividualsController(mockCitizenMatchingService, mockAuthConnector, controllerComponents)
    val sandboxController: SandboxPrivilegedIndividualsController = new SandboxPrivilegedIndividualsController(
      new SandboxCitizenMatchingService(),
      mockAuthConnector,
      controllerComponents
    )
    when(mockAuthConnector.authorise(any(), eqTo(EmptyRetrieval))(any(), any())).thenReturn(successful(()))
  }

  "The live matched individual function" should {
    "respond with http 404 (not found) for an invalid matchId" in new Setup {
      when(
        mockCitizenMatchingService
          .fetchCitizenDetailsByMatchId(eqTo(uuid))(any[HeaderCarrier])
      )
        .thenReturn(failed(new MatchNotFoundException))

      val eventualResult: Future[Result] =
        liveController.matchedIndividual(uuid.toString).apply(FakeRequest())
      status(eventualResult) mustBe NOT_FOUND
      contentAsJson(eventualResult) mustBe Json.parse(
        """{"code":"NOT_FOUND","message":"The resource can not be found"}"""
      )
    }

    "respond with http 200 (ok) when a nino match is successful and citizen details exist" in new Setup {
      when(
        mockCitizenMatchingService
          .fetchCitizenDetailsByMatchId(eqTo(uuid))(any[HeaderCarrier])
      )
        .thenReturn(successful(citizenDetails("Joe", "Bloggs", "AB123456C", "1969-01-15")))
      val eventualResult: Future[Result] =
        liveController.matchedIndividual(uuid.toString).apply(FakeRequest())
      status(eventualResult) mustBe OK
      contentAsJson(eventualResult) mustBe Json.parse(response(uuid, "Joe", "Bloggs", "AB123456C", "1969-01-15"))
    }

    "fail with AuthorizedException when the bearer token does not have enrolment read:individuals-matching" in new Setup {

      when(
        mockAuthConnector
          .authorise(eqTo(Enrolment("read:individuals-matching")), eqTo(EmptyRetrieval))(any(), any())
      )
        .thenReturn(failed(InsufficientEnrolments()))

      intercept[InsufficientEnrolments] {
        await(liveController.matchedIndividual(uuid.toString).apply(FakeRequest()))
      }
      verifyNoInteractions(mockCitizenMatchingService)
    }
  }

  "The sandbox matched individual function" should {

    "respond with http 404 (not found) for an invalid matchId" in new Setup {
      val eventualResult: Future[Result] =
        sandboxController.matchedIndividual(uuid.toString).apply(FakeRequest())
      status(eventualResult) mustBe NOT_FOUND
      contentAsJson(eventualResult) mustBe Json.parse(
        """{"code":"NOT_FOUND","message":"The resource can not be found"}"""
      )
    }

    "respond with http 200 (ok) for sandbox valid matchId and citizen details exist" in new Setup {
      val eventualResult: Future[Result] = sandboxController
        .matchedIndividual(sandboxMatchId.toString)
        .apply(FakeRequest())
      status(eventualResult) mustBe OK
      contentAsJson(eventualResult) mustBe Json.parse(response(sandboxMatchId))
    }

    "not require bearer token authentication" in new Setup {
      val eventualResult: Future[Result] = sandboxController
        .matchedIndividual(sandboxMatchId.toString)
        .apply(FakeRequest())
      status(eventualResult) mustBe OK
      verifyNoInteractions(mockAuthConnector)
    }
  }

  private def response(
    matchId: UUID,
    firstName: String = "Amanda",
    lastName: String = "Joseph",
    nino: String = "NA000799C",
    dateOfBirth: String = "1960-01-15"
  ) =
    s"""
        {
           "_links": {
             "income": {
               "href": "/individuals/income/?matchId=$matchId",
               "name": "GET",
               "title": "View individual's income"
             },
             "employments": {
               "href": "/individuals/employments/?matchId=$matchId",
               "name": "GET",
               "title": "View individual's employments"
             },
             "self": {
               "href": "/individuals/matching/$matchId"
             }
           },
           "individual": {
             "firstName": "$firstName",
             "lastName": "$lastName",
             "nino": "$nino",
             "dateOfBirth": "$dateOfBirth"
           }
        }"""
}
