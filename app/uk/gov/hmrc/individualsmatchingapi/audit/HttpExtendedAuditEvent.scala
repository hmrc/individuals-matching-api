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

import com.google.inject.ImplementedBy
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import javax.inject.{Inject, Named}

class DefaultHttpExtendedAuditEvent @Inject()(@Named("appName") val appName: String) extends HttpExtendedAuditEvent

@ImplementedBy(classOf[DefaultHttpExtendedAuditEvent])
trait HttpExtendedAuditEvent {
  def appName: String

  protected object auditDetailKeys {
    val Input = "input"
    val Method = "method"
    val UserAgentString = "userAgentString"
    val Referrer = "referrer"
  }

  protected object headers {
    val UserAgent = "User-Agent"
    val Referer = "Referer"
  }

  def extendedDataEvent(
    eventType: String,
    transactionName: String,
    request: RequestHeader,
    detail: JsValue = JsString(""))(
    implicit hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)): ExtendedDataEvent = {

    import auditDetailKeys._
    import headers._
    import uk.gov.hmrc.play.audit.http.HeaderFieldsExtractor._

    val requiredFields = Map(
      "ipAddress"            -> hc.forwarded.map(_.value).getOrElse("-"),
      hc.names.authorisation -> hc.authorization.map(_.value).getOrElse("-"),
      hc.names.deviceID      -> hc.deviceID.getOrElse("-"),
      Input                  -> s"Request to ${request.path}",
      Method                 -> request.method.toUpperCase,
      UserAgentString        -> request.headers.get(UserAgent).getOrElse("-"),
      Referrer               -> request.headers.get(Referer).getOrElse("-")
    )

    val tags = hc.toAuditTags(transactionName, request.path)

    ExtendedDataEvent(
      auditSource = appName,
      auditType = eventType,
      detail = detail.as[JsObject] ++
        Json.toJson(requiredFields).as[JsObject] ++
        Json.toJson(optionalAuditFieldsSeq(request.headers.toMap)).as[JsObject],
      tags = tags
    )
  }
}
