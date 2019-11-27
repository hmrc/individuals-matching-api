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

package uk.gov.hmrc.individualsmatchingapi.config

import javax.inject.Inject
import play.api.Configuration
import play.api.http.{HttpConfiguration, HttpErrorHandler, HttpFilters}
import play.api.mvc.{Handler, RequestHeader}
import play.api.routing.Router
import uk.gov.hmrc.individualsmatchingapi.play.RequestHeaderUtils._
import uk.gov.hmrc.play.bootstrap.http.RequestHandler

class IndividualMatchingApiRequestHandler @Inject()(
    router: Router,
    errorHandler: HttpErrorHandler,
    configuration: HttpConfiguration,
    filters: HttpFilters,
    config: Configuration)
    extends RequestHandler(router, errorHandler, configuration, filters) {

  private lazy val unversionedContexts = config
    .getOptional[Seq[String]]("versioning.unversionedContexts")
    .getOrElse(Seq.empty[String])

  override def routeRequest(request: RequestHeader): Option[Handler] = {
    val requestContext = extractUriContext(request)
    if (unversionedContexts.contains(requestContext)) {
      super.routeRequest(request)
    } else {
      super.routeRequest(getVersionedRequest(request))
    }

  }

}
