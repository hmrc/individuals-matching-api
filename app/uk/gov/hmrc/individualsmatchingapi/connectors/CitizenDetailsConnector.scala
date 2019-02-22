/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.Singleton
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpGet, NotFoundException}
import uk.gov.hmrc.individualsmatchingapi.config.{ConfigSupport, WSHttp}
import uk.gov.hmrc.individualsmatchingapi.domain.JsonFormatters.citizenDetailsFormat
import uk.gov.hmrc.individualsmatchingapi.domain.{CitizenDetails, CitizenNotFoundException, InvalidNinoException}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class CitizenDetailsConnector extends ServicesConfig with ConfigSupport {

  val serviceUrl = baseUrl("citizen-details")
  val http: HttpGet = WSHttp

  def citizenDetails(nino: String)(implicit hc: HeaderCarrier): Future[CitizenDetails] = {
    http.GET[CitizenDetails](s"$serviceUrl/citizen-details/nino/$nino") recover {
      case _: NotFoundException => throw new CitizenNotFoundException
      case _: BadRequestException => throw new InvalidNinoException
    }
  }
}