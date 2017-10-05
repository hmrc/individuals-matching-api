/*
 * Copyright 2017 HM Revenue & Customs
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

import play.api.libs.json.JsValue
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost}
import uk.gov.hmrc.individualsmatchingapi.config.WSHttp
import uk.gov.hmrc.individualsmatchingapi.domain.JsonFormatters.detailsMatchRequestFormat
import uk.gov.hmrc.individualsmatchingapi.domain.{DetailsMatchRequest, HasSucceeded, MatchingException}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class MatchingConnector extends ServicesConfig {

  val serviceUrl = baseUrl("matching")
  val http: HttpPost = WSHttp

  def validateMatch(matchingRequest: DetailsMatchRequest)(implicit hc: HeaderCarrier): Future[HasSucceeded] = {
    http.POST[DetailsMatchRequest, JsValue](s"$serviceUrl/matching/perform-match/cycle3", matchingRequest) map { response =>
      (response \ "errorCodes").asOpt[Seq[Int]] match {
        case Some(Nil) => HasSucceeded
        case _ => throw new MatchingException
      }
    }
  }
}
