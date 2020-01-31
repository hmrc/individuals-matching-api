/*
 * Copyright 2020 HM Revenue & Customs
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

import org.mockito.BDDMockito.given
import org.mockito.Matchers.{any, refEq}
import org.mockito.Mockito.{verifyZeroInteractions, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, MustMatchers}
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, _}
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, InsufficientEnrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsmatchingapi.controllers.{LivePrivilegedIndividualsController, SandboxPrivilegedIndividualsController}
import uk.gov.hmrc.individualsmatchingapi.domain.MatchNotFoundException
import uk.gov.hmrc.individualsmatchingapi.domain.SandboxData.sandboxMatchId
import uk.gov.hmrc.individualsmatchingapi.services.{LiveCitizenMatchingService, SandboxCitizenMatchingService}
import unit.uk.gov.hmrc.individualsmatchingapi.support.SpecBase
import unit.uk.gov.hmrc.individualsmatchingapi.util.Individuals

import scala.concurrent.Future.{failed, successful}

class PrivilegedIndividualsControllerSpec
    extends SpecBase with MustMatchers with Results with MockitoSugar with BeforeAndAfterEach with Individuals {

  implicit val headerCarrier = new HeaderCarrier()
  val uuid = UUID.randomUUID()

  trait Setup {

    val mockCitizenMatchingService = mock[LiveCitizenMatchingService]
    val mockAuthConnector = mock[AuthConnector]
    val controllerComponents =
      fakeApplication.injector.instanceOf[ControllerComponents]

    val liveController =
      new LivePrivilegedIndividualsController(mockCitizenMatchingService, mockAuthConnector, controllerComponents)
    val sandboxController = new SandboxPrivilegedIndividualsController(
      new SandboxCitizenMatchingService(),
      mockAuthConnector,
      controllerComponents)

    given(mockAuthConnector.authorise(any(), refEq(EmptyRetrieval))(any(), any()))
      .willReturn(successful(()))
  }

  "The live matched individual function" should {
    "respond with http 404 (not found) for an invalid matchId" in new Setup {
      when(mockCitizenMatchingService.fetchCitizenDetailsByMatchId(refEq(uuid))(any[HeaderCarrier]))
        .thenReturn(failed(new MatchNotFoundException))

      val eventualResult =
        liveController.matchedIndividual(uuid.toString).apply(FakeRequest())
      status(eventualResult) mustBe NOT_FOUND
      contentAsJson(eventualResult) mustBe Json.parse(
        """{"code":"NOT_FOUND","message":"The resource can not be found"}""")
    }

    "respond with http 200 (ok) when a nino match is successful and citizen details exist" in new Setup {
      given(mockCitizenMatchingService.fetchCitizenDetailsByMatchId(refEq(uuid))(any[HeaderCarrier]))
        .willReturn(successful(citizenDetails("Joe", "Bloggs", "AB123456C", "1969-01-15")))
      val eventualResult =
        liveController.matchedIndividual(uuid.toString).apply(FakeRequest())
      status(eventualResult) mustBe OK
      contentAsJson(eventualResult) mustBe Json.parse(response(uuid, "Joe", "Bloggs", "AB123456C", "1969-01-15"))
    }

    "fail with AuthorizedException when the bearer token does not have enrolment read:individuals-matching" in new Setup {
      given(
        mockAuthConnector.authorise(refEq(Enrolment("read:individuals-matching")), refEq(EmptyRetrieval))(any(), any()))
        .willReturn(failed(new InsufficientEnrolments()))

      intercept[InsufficientEnrolments] {
        await(liveController.matchedIndividual(uuid.toString).apply(FakeRequest()))
      }

      verifyZeroInteractions(mockCitizenMatchingService)
    }
  }

  "The sandbox matched individual function" should {

    "respond with http 404 (not found) for an invalid matchId" in new Setup {
      val eventualResult =
        sandboxController.matchedIndividual(uuid.toString).apply(FakeRequest())
      status(eventualResult) mustBe NOT_FOUND
      contentAsJson(eventualResult) mustBe Json.parse(
        """{"code":"NOT_FOUND","message":"The resource can not be found"}""")
    }

    "respond with http 200 (ok) for sandbox valid matchId and citizen details exist" in new Setup {
      val eventualResult = sandboxController
        .matchedIndividual(sandboxMatchId.toString)
        .apply(FakeRequest())
      status(eventualResult) mustBe OK
      contentAsJson(eventualResult) mustBe Json.parse(response(sandboxMatchId))
    }

    "not require bearer token authentication" in new Setup {
      val eventualResult = sandboxController
        .matchedIndividual(sandboxMatchId.toString)
        .apply(FakeRequest())
      status(eventualResult) mustBe OK
      verifyZeroInteractions(mockAuthConnector)
    }
  }

  private def response(
    matchId: UUID,
    firstName: String = "Amanda",
    lastName: String = "Joseph",
    nino: String = "NA000799C",
    dateOfBirth: String = "1960-01-15") =
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
