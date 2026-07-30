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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verifyNoInteractions, when}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Configuration, Environment, Mode}
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, RequestHeader, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments, InsufficientEnrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsmatchingapi.audit.AuditHelper
import uk.gov.hmrc.individualsmatchingapi.config.AppConfig
import uk.gov.hmrc.individualsmatchingapi.controllers.InternalAuthHelper
import uk.gov.hmrc.individualsmatchingapi.controllers.v1.live.LivePrivilegedIndividualsController
import uk.gov.hmrc.individualsmatchingapi.controllers.v1.sandbox.SandboxPrivilegedIndividualsController
import uk.gov.hmrc.individualsmatchingapi.domain.MatchNotFoundException
import uk.gov.hmrc.individualsmatchingapi.domain.SandboxData.sandboxMatchId
import uk.gov.hmrc.individualsmatchingapi.services.{LiveCitizenMatchingService, SandboxCitizenMatchingService, ScopesService}
import uk.gov.hmrc.internalauth.client.BackendAuthComponents
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}
import unit.uk.gov.hmrc.individualsmatchingapi.controllers.v2.ScopesConfigHelper
import unit.uk.gov.hmrc.individualsmatchingapi.support.SpecBase
import unit.uk.gov.hmrc.individualsmatchingapi.util.Individuals

import java.util.UUID
import scala.concurrent.Future.{failed, successful}
import scala.concurrent.{ExecutionContext, Future}

class PrivilegedIndividualsControllerSpec extends SpecBase with Matchers with MockitoSugar with Individuals {

  val uuid: UUID = UUID.randomUUID()

  trait Setup extends ScopesConfigHelper {
    given ControllerComponents = stubControllerComponents()
    implicit val ec: ExecutionContext = ExecutionContext.global

    val sampleCorrelationId = "188e9400-b636-4a3b-80ba-230a8c72b92a"
    val mockCitizenMatchingService: LiveCitizenMatchingService = mock[LiveCitizenMatchingService]
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val controllerComponents: ControllerComponents =
      app.injector.instanceOf[ControllerComponents]

    val mockInternalAuthBehaviour: StubBehaviour = mock[StubBehaviour]
    val backendAuthComponents: BackendAuthComponents = BackendAuthComponentsStub(mockInternalAuthBehaviour)
    val internalAuthHelper = new InternalAuthHelper(
      backendAuthComponents,
      Configuration(InternalAuthHelper.InternalAuthFeatureFlag -> true)
    )
    val mockAuditHelper: AuditHelper = mock[AuditHelper]
    implicit val auditHelper: AuditHelper = mockAuditHelper
    val mockScopesService = new ScopesService(mockScopesConfig)
    when(
      mockAuthConnector
        .authorise(any(), eqTo(Retrievals.allEnrolments))(using any(), any())
    ).thenReturn(Future.successful(Enrolments(Set(Enrolment("test-scope")))))
  }

  trait NonLocalSetUp extends Setup {
    implicit val env: Environment = Environment.simple(mode = Mode.Prod)
    lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
    val liveController: LivePrivilegedIndividualsController =
      new LivePrivilegedIndividualsController(
        mockCitizenMatchingService,
        mockAuthConnector,
        internalAuthHelper,
        controllerComponents,
        mockScopesService
      )(using ec, auditHelper, appConfig, env)

  }

  trait LocalSetUp extends Setup {
    val appLocal: Application = new GuiceApplicationBuilder()
      .configure("localEnv" -> true)
      .build()
    implicit val env: Environment = Environment.simple(mode = Mode.Dev)
    lazy val appConfigLocal: AppConfig = appLocal.injector.instanceOf[AppConfig]
    val sandboxController: SandboxPrivilegedIndividualsController = new SandboxPrivilegedIndividualsController(
      new SandboxCitizenMatchingService(),
      mockAuthConnector,
      internalAuthHelper,
      controllerComponents,
      mockScopesService
    )(using ec, auditHelper, appConfigLocal, env)
  }

  "The live matched individual function" should {
    "respond with http 404 (not found) for an invalid matchId" in new NonLocalSetUp {
      when(
        mockCitizenMatchingService
          .fetchCitizenDetailsByMatchId(eqTo(uuid))(using any[HeaderCarrier], any[RequestHeader])
      )
        .thenReturn(failed(new MatchNotFoundException))

      val eventualResult: Future[Result] =
        liveController
          .matchedIndividual(uuid.toString)
          .apply(FakeRequest().withHeaders("CorrelationId" -> sampleCorrelationId))
      status(eventualResult) mustBe NOT_FOUND
      contentAsJson(eventualResult) mustBe Json.parse(
        """{"code":"NOT_FOUND","message":"The resource can not be found"}"""
      )
    }

    "respond with http 200 (ok) when a nino match is successful and citizen details exist" in new NonLocalSetUp {
      when(
        mockCitizenMatchingService
          .fetchCitizenDetailsByMatchId(eqTo(uuid))(using any[HeaderCarrier], any[RequestHeader])
      )
        .thenReturn(successful(citizenDetails("Joe", "Bloggs", "AB123456C", "1969-01-15")))
      val eventualResult: Future[Result] =
        liveController
          .matchedIndividual(uuid.toString)
          .apply(FakeRequest().withHeaders("CorrelationId" -> sampleCorrelationId))
      status(eventualResult) mustBe OK
      contentAsJson(eventualResult) mustBe Json.parse(response(uuid, "Joe", "Bloggs", "AB123456C", "1969-01-15"))
    }

    "fail with AuthorizedException when the bearer token does not have enrolment read:individuals-matching" in new NonLocalSetUp {

      when(
        mockAuthConnector.authorise(any(), any())(using any(), any())
      ).thenReturn(Future.failed(InsufficientEnrolments()))

      intercept[InsufficientEnrolments] {
        await(
          liveController
            .matchedIndividual(uuid.toString)
            .apply(FakeRequest().withHeaders("CorrelationId" -> sampleCorrelationId))
        )
      }
      verifyNoInteractions(mockCitizenMatchingService)
    }
  }

  "The sandbox matched individual function" should {

    "respond with http 404 (not found) for an invalid matchId" in new LocalSetUp {
      val eventualResult: Future[Result] =
        sandboxController
          .matchedIndividual(uuid.toString)
          .apply(FakeRequest().withHeaders("CorrelationId" -> sampleCorrelationId))
      status(eventualResult) mustBe NOT_FOUND
      contentAsJson(eventualResult) mustBe Json.parse(
        """{"code":"NOT_FOUND","message":"The resource can not be found"}"""
      )
    }

    "respond with http 200 (ok) for sandbox valid matchId and citizen details exist" in new LocalSetUp {
      val eventualResult: Future[Result] = sandboxController
        .matchedIndividual(sandboxMatchId.toString)
        .apply(FakeRequest().withHeaders("CorrelationId" -> sampleCorrelationId))
      status(eventualResult) mustBe OK
      contentAsJson(eventualResult) mustBe Json.parse(response(sandboxMatchId))
    }

    "not require bearer token authentication" in new LocalSetUp {
      val eventualResult: Future[Result] = sandboxController
        .matchedIndividual(sandboxMatchId.toString)
        .apply(FakeRequest().withHeaders("CorrelationId" -> sampleCorrelationId))
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
