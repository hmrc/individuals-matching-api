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

package uk.gov.hmrc.individualsmatchingapi.audit.events

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsmatchingapi.audit.HttpExtendedAuditEvent
import uk.gov.hmrc.individualsmatchingapi.audit.models.ScopesAuditEventModel
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import javax.inject.Inject

class ScopesAuditEvent @Inject()(httpAuditEvent: HttpExtendedAuditEvent) {

  import httpAuditEvent.extendedDataEvent

  def auditType = "AuthScopesAuditEvent"
  def transactionName = "AuditCall"
  def apiVersion = "2.0"

  def apply(matchId: String, scopes: String, request: RequestHeader)(
    implicit hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)): ExtendedDataEvent = {

    val event = extendedDataEvent(
      auditType,
      transactionName,
      request,
      Json.toJson(ScopesAuditEventModel(apiVersion, matchId, scopes)))

    Logger.debug(s"$auditType - AuditEvent: $event")

    event

  }
}
