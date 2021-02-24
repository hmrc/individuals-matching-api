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

package uk.gov.hmrc.individualsmatchingapi.audit

import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsmatchingapi.audit.events._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AuditHelper @Inject()(
  auditConnector: AuditConnector,
  apiResponseEvent: ApiResponseEvent,
  apiFailureEvent: ApiFailureEvent,
  ifApiResponseEvent: IfApiResponseEvent,
  ifApiFailureEvent: IfApiFailureEvent,
  scopesAuditEvent: ScopesAuditEvent)(implicit ec: ExecutionContext) {

  def auditApiResponse(
    correlationId: String,
    matchId: String,
    scopes: Option[String],
    request: RequestHeader,
    endpoint: String,
    response: JsValue)(implicit hc: HeaderCarrier) =
    auditConnector.sendExtendedEvent(
      apiResponseEvent(
        Some(correlationId),
        scopes,
        matchId,
        request,
        Some(endpoint),
        response.toString
      )
    )

  def auditApiFailure(
    correlationId: Option[String],
    matchId: String,
    request: RequestHeader,
    requestUrl: String,
    msg: String)(implicit hc: HeaderCarrier) =
    auditConnector.sendExtendedEvent(
      apiFailureEvent(
        correlationId,
        None,
        matchId,
        request,
        Some(requestUrl),
        msg
      )
    )

  def auditIfApiResponse(
    correlationId: String,
    scopes: Option[String],
    matchId: String,
    request: RequestHeader,
    requestUrl: String,
    response: JsValue)(implicit hc: HeaderCarrier) =
    auditConnector.sendExtendedEvent(
      ifApiResponseEvent(
        Some(correlationId),
        scopes,
        matchId,
        request,
        Some(requestUrl),
        response.toString
      )
    )

  def auditIfApiFailure(
    correlationId: String,
    scopes: Option[String],
    matchId: String,
    request: RequestHeader,
    requestUrl: String,
    msg: String)(implicit hc: HeaderCarrier) =
    auditConnector.sendExtendedEvent(
      ifApiFailureEvent(
        Some(correlationId),
        scopes,
        matchId,
        request,
        Some(requestUrl),
        msg
      )
    )

  def auditAuthScopes(auditableData: String, scopes: String, request: RequestHeader)(implicit hc: HeaderCarrier) =
    auditConnector.sendExtendedEvent(
      scopesAuditEvent(
        auditableData,
        scopes,
        request
      )
    )
}
