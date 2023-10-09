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

import play.api.Configuration
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, Upstream4xxResponse, UpstreamErrorResponse}
import uk.gov.hmrc.individualsmatchingapi.domain.JsonFormatters.citizenDetailsFormat
import uk.gov.hmrc.individualsmatchingapi.domain.{CitizenDetails, CitizenNotFoundException, InvalidNinoException}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.UpstreamErrorResponse.Upstream5xxResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CitizenDetailsConnector @Inject()(config: Configuration, http: HttpClient, serviceConfig: ServicesConfig)(
  implicit executionContext: ExecutionContext) {

  val serviceUrl: String = serviceConfig.baseUrl("citizen-details")

  def citizenDetails(nino: String)(implicit hc: HeaderCarrier): Future[CitizenDetails] =
    http.GET[CitizenDetails](s"$serviceUrl/citizen-details/nino/$nino") recover {
      case Upstream4xxResponse(_, 404, _, _) => throw new CitizenNotFoundException
      case Upstream4xxResponse(_, 400, _, _) => throw new InvalidNinoException
      case Upstream5xxResponse(response) =>
        if (response.message.contains("Response body: ''"))
          throw UpstreamErrorResponse(
            s"GET of $serviceUrl/citizen-details/nino/$nino returned ${response.statusCode} and an empty body.",
            response.statusCode)
        else
          throw UpstreamErrorResponse(
            s"GET of $serviceUrl/citizen-details/nino/$nino returned ${response.statusCode}",
            response.statusCode)
    }
}
