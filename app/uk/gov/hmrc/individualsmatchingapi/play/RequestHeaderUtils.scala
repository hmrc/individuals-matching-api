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

package uk.gov.hmrc.individualsmatchingapi.play

import play.api.http.HeaderNames.ACCEPT
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.individualsmatchingapi.utils.UuidValidator

import java.util.UUID
import scala.util.{Success, Try}

object RequestHeaderUtils {

  private val acceptHeaderRegex = "application/vnd\\.hmrc\\.(.*)\\+json".r

  private val uriRegex = "(/[a-zA-Z0-9-_]*)/?.*$".r

  def extractUriContext(requestHeader: RequestHeader): String =
    (uriRegex.findFirstMatchIn(requestHeader.uri) map (_.group(1))).get

  def validateCorrelationId(requestHeader: RequestHeader): UUID =
    requestHeader.headers.get("CorrelationId") match {
      case Some(uuidString) =>
        if (UuidValidator.validate(uuidString)) {
          UUID.fromString(uuidString)
        } else {
          throw new BadRequestException("Malformed CorrelationId")
        }
      case None => throw new BadRequestException("CorrelationId is required")
    }

  def maybeCorrelationId(requestHeader: RequestHeader): Option[String] =
    Try(validateCorrelationId(requestHeader)) match {
      case Success(value) => Some(value.toString)
      case _              => None
    }

  def getVersionedRequest(originalRequest: RequestHeader): RequestHeader = {
    val version = getVersion(originalRequest)

    originalRequest.withTarget(
      originalRequest.target
        .withUriString(versionedUri(originalRequest.uri, version))
        .withPath(versionedUri(originalRequest.path, version))
    )
  }

  private def getVersion(originalRequest: RequestHeader) =
    originalRequest.headers.get(ACCEPT) flatMap { acceptHeaderValue =>
      acceptHeaderRegex.findFirstMatchIn(acceptHeaderValue) map (_.group(1))
    } getOrElse "1.0"

  private def versionedUri(urlPath: String, version: String) =
    urlPath match {
      case "/" => s"/v$version"
      case uri => s"/v$version$urlPath"
    }
}
