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

import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.IdiomaticMockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions}
import org.scalatest.matchers.must.Matchers
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, _}
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
import scala.concurrent.Future.{failed, successful}
import scala.concurrent.{ExecutionContext, Future}

class PrivilegedIndividualsControllerSpec extends SpecBase with Matchers with IdiomaticMockito with Individuals {

  implicit val headerCarrier: HeaderCarrier = new HeaderCarrier()
  val uuid: UUID = UUID.randomUUID()
  val sampleCorrelationId = "188e9400-b636-4a3b-80ba-230a8c72b92a"

  trait Setup extends ScopesConfigHelper {

    val mockCitizenMatchingService: LiveCitizenMatchingService = mock[LiveCitizenMatchingService]

    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockAuditHelper: AuditHelper = mock[AuditHelper]

    val mockScopesService = new ScopesService(mockScopesConfig)
    val scopesHelper = new ScopesHelper(mockScopesService)

    val controllerComponents: ControllerComponents = fakeApplication.injector.instanceOf[ControllerComponents]

    implicit val ec: ExecutionContext = fakeApplication.injector.instanceOf[ExecutionContext]

    val liveController = new PrivilegedIndividualsController(
      mockCitizenMatchingService,
      mockScopesService,
      scopesHelper,
      mockAuditHelper,
      mockAuthConnector,
      controllerComponents)

    mockAuthConnector
      .authorise(any(), refEq(Retrievals.allEnrolments))(any(), any())
      .returns(Future.successful(Enrolments(Set(Enrolment("test-scope")))))
  }

  "The live matched individual function" should {
    "respond with http 404 (not found) for an invalid matchId" in new Setup {
      mockCitizenMatchingService
        .fetchCitizenDetailsByMatchId(refEq(uuid))(any[HeaderCarrier])
        .returns(failed(new MatchNotFoundException))

      val eventualResult: Future[Result] = liveController
        .matchedIndividual(uuid.toString)
        .apply(FakeRequest().withHeaders(("CorrelationId", sampleCorrelationId)))

      status(eventualResult) mustBe NOT_FOUND
      contentAsJson(eventualResult) mustBe Json.parse(
        """{"code":"NOT_FOUND","message":"The resource can not be found"}""")

      mockAuditHelper.auditApiFailure(any(), any(), any(), any(), any())(any()) was called
    }

    "respond with http 200 (ok) when a nino match is successful and citizen details exist" in new Setup {
      mockCitizenMatchingService
        .fetchCitizenDetailsByMatchId(refEq(uuid))(any[HeaderCarrier])
        .returns(successful(citizenDetails("Joe", "Bloggs", "AB123456C", "1969-01-15")))
      val eventualResult: Future[Result] =
        liveController
          .matchedIndividual(uuid.toString)
          .apply(FakeRequest().withHeaders(("CorrelationId", sampleCorrelationId)))
      status(eventualResult) mustBe OK
      contentAsJson(eventualResult) mustBe Json.parse(response(uuid, "Joe", "Bloggs", "AB123456C", "1969-01-15"))

      mockAuditHelper.auditApiResponse(any(), any(), any(), any(), any(), any())(any()) was called
    }

    "fail with AuthorizedException when the bearer token does not have a valid enrolment" in new Setup {

      mockAuthConnector
        .authorise(any(), refEq(Retrievals.allEnrolments))(any(), any())
        .returns(failed(InsufficientEnrolments()))

      mockCitizenMatchingService
        .fetchCitizenDetailsByMatchId(refEq(uuid))(any[HeaderCarrier])
        .returns(failed(new MatchNotFoundException))

      val res: Future[Result] = liveController
        .matchedIndividual(uuid.toString)
        .apply(FakeRequest().withHeaders(("CorrelationId", sampleCorrelationId)))

      status(res) mustBe UNAUTHORIZED
      contentAsJson(res) mustBe Json.parse("""{"code":"UNAUTHORIZED","message":"Insufficient Enrolments"}""")

      verifyNoInteractions(mockCitizenMatchingService)
      mockAuditHelper.auditApiFailure(any(), any(), any(), any(), any())(any()) was called
    }

    "respond with http 400 (Bad Request) for a malformed CorrelationId" in new Setup {
      mockCitizenMatchingService
        .fetchCitizenDetailsByMatchId(refEq(uuid))(any[HeaderCarrier])
        .returns(failed(new MatchNotFoundException))

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

      mockAuditHelper.auditApiFailure(any(), any(), any(), any(), any())(any()) was called
    }

    "respond with http 400 (Bad Request) for a missing CorrelationId" in new Setup {
      mockCitizenMatchingService
        .fetchCitizenDetailsByMatchId(refEq(uuid))(any[HeaderCarrier])
        .returns(failed(new MatchNotFoundException))

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

      mockAuditHelper.auditApiFailure(any(), any(), any(), any(), any())(any()) was called
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
