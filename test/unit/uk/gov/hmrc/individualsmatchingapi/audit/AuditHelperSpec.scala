/*
 * Copyright 2021 HM Revenue & Customs
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

package unit.uk.gov.hmrc.individualsmatchingapi.audit

import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.{AsyncWordSpec, Matchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsmatchingapi.audit.AuditHelper
import uk.gov.hmrc.individualsmatchingapi.audit.models.{ApiFailureResponseEventModel, ApiResponseEventModel, ScopesAuditEventModel}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

class AuditHelperSpec extends AsyncWordSpec with Matchers with MockitoSugar {

  implicit val hc = HeaderCarrier()

  val nino = "CS700100A"
  val correlationId = "test"
  val scopes = "test"
  val matchId = "80a6bb14-d888-436e-a541-4000674c60aa"
  val request = FakeRequest()
  val response = Some(Json.toJson("some" -> "json"))
  val ifUrl =
    s"host/individuals/employments/paye/nino/$nino?startDate=2019-01-01&endDate=2020-01-01&fields=some(vals(val1),val2)"
  val endpoint = "/test"

  val auditConnector = mock[AuditConnector]
  val auditHelper = new AuditHelper(auditConnector)

  "Auth helper" should {

    "auditAuthScopes" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[ScopesAuditEventModel])

      auditHelper.auditAuthScopes(matchId, scopes, request)

      verify(auditConnector, times(1))
        .sendExplicitAudit(eqTo("AuthScopesAuditEvent"), captor.capture())(any(), any(), any())

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[ScopesAuditEventModel].apiVersion shouldEqual "2.0"
      capturedEvent.asInstanceOf[ScopesAuditEventModel].matchId shouldEqual matchId
      capturedEvent.asInstanceOf[ScopesAuditEventModel].scopes shouldBe scopes

    }

    "auditApiResponse" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[ApiResponseEventModel])

      auditHelper.auditApiResponse(correlationId, matchId, scopes, request, endpoint, response)

      verify(auditConnector, times(1))
        .sendExplicitAudit(eqTo("ApiResponseEvent"), captor.capture())(any(), any(), any())

      val result = Json.parse("""
                                |{
                                |  "apiVersion": "2.0",
                                |  "matchId": "80a6bb14-d888-436e-a541-4000674c60aa",
                                |  "correlationId": "test",
                                |  "scopes": "test",
                                |  "requestUrl":"/test",
                                |  "response": "[\"some\",\"json\"]",
                                |  "method": "GET",
                                |  "deviceID": "-",
                                |  "ipAddress": "-",
                                |  "referrer": "-",
                                |  "Authorization": "-",
                                |  "input": "Request to /",
                                |  "userAgentString": "-"
                                |}
                                |""".stripMargin)

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[ApiResponseEventModel].matchId shouldEqual matchId
      capturedEvent.asInstanceOf[ApiResponseEventModel].correlationId shouldEqual Some(correlationId)
      capturedEvent.asInstanceOf[ApiResponseEventModel].scopes shouldBe scopes
      capturedEvent.asInstanceOf[ApiResponseEventModel].returnLinks shouldBe endpoint
      capturedEvent.asInstanceOf[ApiResponseEventModel].response shouldBe response

    }

    "auditApiFailure" in {

      Mockito.reset(auditConnector)

      val msg = "Something went wrong"

      val captor = ArgumentCaptor.forClass(classOf[ApiFailureResponseEventModel])

      auditHelper.auditApiFailure(Some(correlationId), matchId, request, "/test", msg)

      verify(auditConnector, times(1)).sendExplicitAudit(eqTo("ApiFailureEvent"), captor.capture())(any(), any(), any())

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[ApiFailureResponseEventModel].matchId shouldEqual matchId
      capturedEvent.asInstanceOf[ApiFailureResponseEventModel].correlationId shouldEqual Some(correlationId)
      capturedEvent.asInstanceOf[ApiFailureResponseEventModel].requestUrl shouldEqual endpoint
      capturedEvent.asInstanceOf[ApiFailureResponseEventModel].response shouldEqual msg

    }

  }

}
