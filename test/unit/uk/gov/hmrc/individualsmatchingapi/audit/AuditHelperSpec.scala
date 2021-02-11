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

import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.{AsyncWordSpec, Matchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsmatchingapi.audit.{AuditHelper, DefaultHttpExtendedAuditEvent}
import uk.gov.hmrc.individualsmatchingapi.audit.events.{ApiFailureEvent, ApiResponseEvent, IfApiFailureEvent, IfApiResponseEvent, ScopesAuditEvent}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

class AuditHelperSpec extends AsyncWordSpec with Matchers with MockitoSugar {

  implicit val hc = HeaderCarrier()

  val nino = "CS700100A"
  val correlationId = "test"
  val scopes = Some("test")
  val matchId = "80a6bb14-d888-436e-a541-4000674c60aa"
  val request = FakeRequest()
  val response = Json.toJson("some" -> "json")
  val ifUrl =
    s"host/individuals/employments/paye/nino/$nino?startDate=2019-01-01&endDate=2020-01-01&fields=some(vals(val1),val2)"
  val endpoint = "/test"

  val auditConnector = mock[AuditConnector]
  val httpExtendedAuditEvent = new DefaultHttpExtendedAuditEvent("individuals-employments-api")

  val apiResponseEvent = new ApiResponseEvent(httpExtendedAuditEvent)
  val apiFailureEvent = new ApiFailureEvent(httpExtendedAuditEvent)
  val ifApiResponseEvent = new IfApiResponseEvent(httpExtendedAuditEvent)
  val ifApiFailureEvent = new IfApiFailureEvent(httpExtendedAuditEvent)
  val scopesAuditEvent = new ScopesAuditEvent(httpExtendedAuditEvent)

  val auditHelper = new AuditHelper(
    auditConnector,
    apiResponseEvent,
    apiFailureEvent,
    ifApiResponseEvent,
    ifApiFailureEvent,
    scopesAuditEvent
  )

  "Auth helper" should {

    "auditAuthScopes" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      auditHelper.auditAuthScopes(matchId, scopes.get, request)

      verify(auditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())

      val result = Json.parse("""
                                |{
                                |  "apiVersion": "2.0",
                                |  "matchId": "80a6bb14-d888-436e-a541-4000674c60aa",
                                |  "scopes": "test",
                                |  "method": "GET",
                                |  "deviceID": "-",
                                |  "ipAddress": "-",
                                |  "token": "-",
                                |  "referrer": "-",
                                |  "Authorization": "-",
                                |  "input": "Request to /",
                                |  "userAgentString": "-"
                                |}
                                |""".stripMargin)

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditSource shouldEqual "individuals-employments-api"
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditType shouldEqual "AuthScopesAuditEvent"
      capturedEvent.asInstanceOf[ExtendedDataEvent].detail shouldBe result

    }

    "auditApiResponse" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      auditHelper.auditApiResponse(correlationId, matchId, scopes, request, endpoint, response)

      verify(auditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())

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
                                |  "token": "-",
                                |  "referrer": "-",
                                |  "Authorization": "-",
                                |  "input": "Request to /",
                                |  "userAgentString": "-"
                                |}
                                |""".stripMargin)

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditSource shouldEqual "individuals-employments-api"
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditType shouldEqual "ApiResponseEvent"
      capturedEvent.asInstanceOf[ExtendedDataEvent].detail shouldBe result

    }

    "auditApiFailure" in {

      Mockito.reset(auditConnector)

      val msg = "Something went wrong"

      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      auditHelper.auditApiFailure(Some(correlationId), matchId, request, "/test", msg)

      verify(auditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())

      val result = Json.parse("""
                                |{
                                |  "apiVersion": "2.0",
                                |  "matchId": "80a6bb14-d888-436e-a541-4000674c60aa",
                                |  "correlationId": "test",
                                |  "requestUrl":"/test",
                                |  "response": "Something went wrong",
                                |  "method": "GET",
                                |  "deviceID": "-",
                                |  "ipAddress": "-",
                                |  "token": "-",
                                |  "referrer": "-",
                                |  "Authorization": "-",
                                |  "input": "Request to /",
                                |  "userAgentString": "-"
                                |}
                                |""".stripMargin)

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditSource shouldEqual "individuals-employments-api"
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditType shouldEqual "ApiFailureEvent"
      capturedEvent.asInstanceOf[ExtendedDataEvent].detail shouldBe result

    }

    "auditIfApiResponse" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      auditHelper.auditIfApiResponse(correlationId, scopes, matchId, request, ifUrl, response)

      verify(auditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())

      val result = Json.parse(
        """
          |{
          |  "apiVersion": "2.0",
          |  "matchId": "80a6bb14-d888-436e-a541-4000674c60aa",
          |  "correlationId": "test",
          |  "scopes": "test",
          |  "requestUrl": "host/individuals/employments/paye/nino/CS700100A?startDate=2019-01-01&endDate=2020-01-01&fields=some(vals(val1),val2)",
          |  "response": "[\"some\",\"json\"]",
          |  "method": "GET",
          |  "deviceID": "-",
          |  "ipAddress": "-",
          |  "token": "-",
          |  "referrer": "-",
          |  "Authorization": "-",
          |  "input": "Request to /",
          |  "userAgentString": "-"
          |}
          |""".stripMargin)

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditSource shouldEqual "individuals-employments-api"
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditType shouldEqual "IfApiResponseEvent"
      capturedEvent.asInstanceOf[ExtendedDataEvent].detail shouldBe result

    }

    "auditIfApiFailure" in {

      Mockito.reset(auditConnector)

      val msg = "Something went wrong"

      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      auditHelper.auditIfApiFailure(correlationId, scopes, matchId, request, ifUrl, msg)

      verify(auditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())

      val result = Json.parse(
        """
          |{
          |  "apiVersion": "2.0",
          |  "matchId": "80a6bb14-d888-436e-a541-4000674c60aa",
          |  "correlationId": "test",
          |  "scopes": "test",
          |  "requestUrl": "host/individuals/employments/paye/nino/CS700100A?startDate=2019-01-01&endDate=2020-01-01&fields=some(vals(val1),val2)",
          |  "response": "Something went wrong",
          |  "method": "GET",
          |  "deviceID": "-",
          |  "ipAddress": "-",
          |  "token": "-",
          |  "referrer": "-",
          |  "Authorization": "-",
          |  "input": "Request to /",
          |  "userAgentString": "-"
          |}
          |""".stripMargin)

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditSource shouldEqual "individuals-employments-api"
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditType shouldEqual "IfApiFailureEvent"
      capturedEvent.asInstanceOf[ExtendedDataEvent].detail shouldBe result

    }

  }

}
