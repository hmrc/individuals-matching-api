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

import akka.actor.ActorSystem
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import play.api.Mode.Mode
import play.api.libs.json.Json
import play.api.mvc.{Handler, RequestHeader, Result}
import play.api.{Application, Configuration, Logger, Play}
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.individualsmatchingapi.domain.JsonFormatters._
import uk.gov.hmrc.individualsmatchingapi.domain.{ErrorInternalServer, ErrorInvalidRequest, ErrorUnauthorized}
import uk.gov.hmrc.individualsmatchingapi.play.RequestHeaderUtils._
import uk.gov.hmrc.play.config.{AppName, ControllerConfig}
import uk.gov.hmrc.play.microservice.bootstrap.DefaultMicroserviceGlobal
import uk.gov.hmrc.play.microservice.filters.{AuditFilter, LoggingFilter, MicroserviceFilterSupport}

import scala.concurrent.Future
import scala.util.Try
import scala.util.matching.Regex


object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

object MicroserviceAuditFilter extends AuditFilter with AppName with MicroserviceFilterSupport with ConfigSupport {
  override val auditConnector = MicroserviceAuditConnector
  override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing
}

object MicroserviceLoggingFilter extends LoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object MicroserviceGlobal
  extends DefaultMicroserviceGlobal
    with MicroserviceFilterSupport
    with ConfigSupport {

  override val auditConnector = MicroserviceAuditConnector

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"microservice.metrics")

  override val loggingFilter = MicroserviceLoggingFilter

  override val microserviceAuditFilter = MicroserviceAuditFilter

  override val authFilter = None

  private lazy val unversionedContexts = Play.current.configuration.getStringSeq("versioning.unversionedContexts").getOrElse(Seq.empty[String])

  override def onRequestReceived(originalRequest: RequestHeader) = {
    val requestContext = extractUriContext(originalRequest)
    if (unversionedContexts.contains(requestContext)) {
      super.onRequestReceived(originalRequest)
    } else {
      super.onRequestReceived(getVersionedRequest(originalRequest))
    }
  }

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
    ex match {
      case _: AuthorisationException => Future.successful(ErrorUnauthorized.toHttpResponse)
      case _ =>
        Logger.error("An unexpected error occurred", ex)
        Future.successful(ErrorInternalServer.toHttpResponse)
    }
  }

  override def onBadRequest(request: RequestHeader, error: String): Future[Result] = {

    val maybeInvalidRequest = Try(Json.parse(error).as[ErrorInvalidRequest]).toOption

    maybeInvalidRequest match {
      case Some(errorResponse) => Future.successful(errorResponse.toHttpResponse)
      case _ => Future.successful(ErrorInvalidRequest("Invalid Request").toHttpResponse)
    }
  }

  private lazy val headerOpt = playConfiguration.getString("router.header")
  private lazy val regexOpt  = playConfiguration.getString("router.regex")
  private lazy val prefixOpt = playConfiguration.getString("router.prefix")

  lazy val router: Option[(String, String, String)] = {
    (headerOpt, regexOpt, prefixOpt) match {
      case (Some(a:String), Some(b:String), Some(c:String)) => Some((a, b, c))
      case _ => None
    }
  }

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    val overrideRequest = router.fold(request) {
      case (header, regex, prefix) => request.headers.get(header) match {
        case Some(value) =>
          val found = new Regex(regex).findFirstIn(value)
          found.fold(request) { _ =>
            Logger.info(s"Overriding request due to $router")
            request.copy(path = prefix + request.path)
          }
        case _ => request
      }
    }
    super.onRouteRequest(overrideRequest)
  }
}

trait ConfigSupport {
  private def current: Application = Play.current

  def playConfiguration: Configuration = current.configuration
  def mode: Mode = current.mode

  def runModeConfiguration: Configuration = playConfiguration
  def appNameConfiguration: Configuration = playConfiguration
  def actorSystem: ActorSystem = current.actorSystem
}