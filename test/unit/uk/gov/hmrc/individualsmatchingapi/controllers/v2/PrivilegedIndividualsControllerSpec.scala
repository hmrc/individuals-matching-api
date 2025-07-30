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

package unit.uk.gov.hmrc.individualsmatchingapi.controllers.v2

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments, InsufficientEnrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsmatchingapi.audit.AuditHelper
import uk.gov.hmrc.individualsmatchingapi.controllers.v2.PrivilegedIndividualsController
import uk.gov.hmrc.individualsmatchingapi.domain.MatchNotFoundException
import uk.gov.hmrc.individualsmatchingapi.services.{LiveCitizenMatchingService, ScopesHelper, ScopesService}
import unit.uk.gov.hmrc.individualsmatchingapi.support.SpecBase
import unit.uk.gov.hmrc.individualsmatchingapi.util.Individuals

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class PrivilegedIndividualsControllerSpec extends SpecBase with Matchers with MockitoSugar with Individuals {
  val uuid: UUID = UUID.randomUUID()
  val sampleCorrelationId = "188e9400-b636-4a3b-80ba-230a8c72b92a"

  trait Setup extends ScopesConfigHelper {
    val mockCitizenMatchingService: LiveCitizenMatchingService = mock[LiveCitizenMatchingService]

    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockAuditHelper: AuditHelper = mock[AuditHelper]

    val mockScopesService = new ScopesService(mockScopesConfig)
    val scopesHelper = new ScopesHelper(mockScopesService)

    val controllerComponents: ControllerComponents = app.injector.instanceOf[ControllerComponents]

    val liveController = new PrivilegedIndividualsController(
      mockCitizenMatchingService,
      mockScopesService,
      scopesHelper,
      mockAuditHelper,
      mockAuthConnector,
      controllerComponents
    )

    when(
      mockAuthConnector
        .authorise(any(), eqTo(Retrievals.allEnrolments))(using any(), any())
    )
      .thenReturn(Future.successful(Enrolments(Set(Enrolment("test-scope")))))
  }

  "The live matched individual function" should {
    "respond with http 404 (not found) for an invalid matchId" in new Setup {
      when(
        mockCitizenMatchingService
          .fetchCitizenDetailsByMatchId(eqTo(uuid))(using any[HeaderCarrier])
      )
        .thenReturn(failed(new MatchNotFoundException))

      val eventualResult: Future[Result] = liveController
        .matchedIndividual(uuid.toString)
        .apply(FakeRequest().withHeaders(("CorrelationId", sampleCorrelationId)))

      status(eventualResult) mustBe NOT_FOUND
      contentAsJson(eventualResult) mustBe Json.parse(
        """{"code":"NOT_FOUND","message":"The resource can not be found"}"""
      )

      verify(mockAuditHelper).auditApiFailure(any(), any(), any(), any(), any())(using any())
    }

    "respond with http 200 (ok) when a nino match is successful and citizen details exist" in new Setup {
      when(
        mockCitizenMatchingService
          .fetchCitizenDetailsByMatchId(eqTo(uuid))(using any[HeaderCarrier])
      )
        .thenReturn(successful(citizenDetails("Joe", "Bloggs", "AB123456C", "1969-01-15")))
      val eventualResult: Future[Result] =
        liveController
          .matchedIndividual(uuid.toString)
          .apply(FakeRequest().withHeaders(("CorrelationId", sampleCorrelationId)))
      status(eventualResult) mustBe OK
      contentAsJson(eventualResult) mustBe Json.parse(response(uuid, "Joe", "Bloggs", "AB123456C", "1969-01-15"))

      verify(mockAuditHelper).auditApiResponse(any(), any(), any(), any(), any(), any())(using any())
    }

    "fail with AuthorizedException when the bearer token does not have a valid enrolment" in new Setup {

      when(
        mockAuthConnector
          .authorise(any(), eqTo(Retrievals.allEnrolments))(using any(), any())
      )
        .thenReturn(failed(InsufficientEnrolments()))

      when(
        mockCitizenMatchingService
          .fetchCitizenDetailsByMatchId(eqTo(uuid))(using any[HeaderCarrier])
      )
        .thenReturn(failed(new MatchNotFoundException))

      val res: Future[Result] = liveController
        .matchedIndividual(uuid.toString)
        .apply(FakeRequest().withHeaders(("CorrelationId", sampleCorrelationId)))

      status(res) mustBe UNAUTHORIZED
      contentAsJson(res) mustBe Json.parse("""{"code":"UNAUTHORIZED","message":"Insufficient Enrolments"}""")

      verifyNoInteractions(mockCitizenMatchingService)
      verify(mockAuditHelper).auditApiFailure(any(), any(), any(), any(), any())(using any())
    }

    "respond with http 400 (Bad Request) for a malformed CorrelationId" in new Setup {
      when(
        mockCitizenMatchingService
          .fetchCitizenDetailsByMatchId(eqTo(uuid))(using any[HeaderCarrier])
      )
        .thenReturn(failed(new MatchNotFoundException))

      val res: Future[Result] = liveController
        .matchedIndividual(uuid.toString)
        .apply(FakeRequest().withHeaders(("CorrelationId", "test")))

      status(res) mustBe BAD_REQUEST
      contentAsJson(res) mustBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(mockAuditHelper).auditApiFailure(any(), any(), any(), any(), any())(using any())
    }

    "respond with http 400 (Bad Request) for a missing CorrelationId" in new Setup {
      when(
        mockCitizenMatchingService
          .fetchCitizenDetailsByMatchId(eqTo(uuid))(using any[HeaderCarrier])
      )
        .thenReturn(failed(new MatchNotFoundException))

      val res: Future[Result] = liveController.matchedIndividual(uuid.toString).apply(FakeRequest())

      status(res) mustBe BAD_REQUEST
      contentAsJson(res) mustBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(mockAuditHelper).auditApiFailure(any(), any(), any(), any(), any())(using any())
    }
  }

  private def response(matchId: UUID, firstName: String, lastName: String, nino: String, dateOfBirth: String) =
    s"""
      {
         "individual": {
           "firstName": "$firstName",
           "lastName": "$lastName",
           "nino": "$nino",
           "dateOfBirth": "$dateOfBirth"
         },
         "_links": {
           "self": {
             "href": "/individuals/matching/$matchId"
           },
           "benefits-and-credits": {
             "href": "/individuals/benefits-and-credits/?matchId=$matchId",
             "title": "View individual's benefits and credits"
           },
           "details": {
             "href": "/individuals/details/?matchId=$matchId",
             "title": "View individual's details"
           },
           "employments": {
             "href": "/individuals/employments/?matchId=$matchId",
             "title": "View individual's employments"
           },
           "income": {
             "href": "/individuals/income/?matchId=$matchId",
             "title": "View individual's income"
           }
         }
      }"""
}
