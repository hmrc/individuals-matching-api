/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.individualsmatchingapi.controllers

import play.api.Configuration
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, Predicate as IAPredicate, Retrieval}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InternalAuthHelper @Inject() (backendAuthComponents: BackendAuthComponents, configuration: Configuration) {

  private val logger: Logger = play.api.Logger(this.getClass)

  private val internalAuthPermission: IAPredicate = InternalAuthPermission.readPermission
  private val internalAuthEnabled: Boolean = configuration.get[Boolean](InternalAuthHelper.InternalAuthFeatureFlag)

  def isAuthorised(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Boolean] =
    if (internalAuthEnabled) {
      backendAuthComponents
        .verify(Retrieval.hasPredicate(internalAuthPermission))
        .map(_.contains(true))
        .recover { case e =>
          logger.warn("Internal auth failed, falling back to scopes auth", e)
          false
        }
    } else {
      logger.info("Internal auth disabled, falling back to scopes auth")
      Future.successful(false)
    }
}

object InternalAuthHelper {
  val InternalAuthFeatureFlag: String = "features.internal-auth.enabled"
}
