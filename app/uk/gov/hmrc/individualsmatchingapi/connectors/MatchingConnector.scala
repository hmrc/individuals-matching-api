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

package uk.gov.hmrc.individualsmatchingapi.connectors
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.individualsmatchingapi.domain.JsonFormatters.detailsMatchRequestFormat
import uk.gov.hmrc.individualsmatchingapi.domain.{DetailsMatchRequest, MatchingException}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MatchingConnector @Inject() (http: HttpClientV2, servicesConfig: ServicesConfig)(implicit
  executionContext: ExecutionContext
) {
  val serviceUrl: String = servicesConfig.baseUrl("matching")

  def validateMatch(
    matchingRequest: DetailsMatchRequest
  )(implicit hc: HeaderCarrier): Future[Unit] =
    http
      .post(url"$serviceUrl/matching/perform-match/cycle3")
      .withBody(Json.toJson(matchingRequest))
      .execute[HttpResponse]
      .map { response =>
        (response.json \ "errorCodes").asOpt[Seq[Int]] match {
          case Some(Nil) => ()
          case _         => throw new MatchingException
        }
      }
}
