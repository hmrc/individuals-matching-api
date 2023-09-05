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

package uk.gov.hmrc.individualsmatchingapi.controllers

import play.api.Logging
import play.api.libs.json._
import play.api.mvc.{ControllerComponents, Request, RequestHeader, Result}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthorisationException, AuthorisedFunctions, Enrolment, InsufficientEnrolments}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, TooManyRequestException}
import uk.gov.hmrc.individualsmatchingapi.audit.AuditHelper
import uk.gov.hmrc.individualsmatchingapi.controllers.Environment.SANDBOX
import uk.gov.hmrc.individualsmatchingapi.domain._
import uk.gov.hmrc.individualsmatchingapi.utils.UuidValidator
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

abstract class CommonController @Inject()(cc: ControllerComponents)(implicit executionContext: ExecutionContext)
    extends BackendController(cc) with Logging {

  override protected def withJsonBody[T](
    f: T => Future[Result])(implicit request: Request[JsValue], m: Manifest[T], reads: Reads[T]): Future[Result] =
    Try(request.body.validate[T]) match {
      case Success(JsSuccess(payload, _)) => f(payload)
      case Success(JsError(errs)) =>
        successful(ErrorInvalidRequest(s"${fieldName(errs)} is required").toHttpResponse)
      case Failure(e) if e.isInstanceOf[ValidationException] =>
        successful(ErrorInvalidRequest(e.getMessage).toHttpResponse)
      case Failure(_) =>
        successful(ErrorInvalidRequest("Unable to process request").toHttpResponse)
    }

  protected def withJsonBodyV2[T](
    f: T => Future[Result])(implicit request: Request[JsValue], reads: Reads[T]): Future[Result] =
    Try(request.body.validate[T]) match {
      case Success(JsSuccess(payload, _))                    => f(payload)
      case Success(JsError(errs))                            => throw new InvalidBodyException(s"${fieldName(errs)} is required")
      case Failure(e) if e.isInstanceOf[ValidationException] => throw new InvalidBodyException(e.getMessage)
      case Failure(_)                                        => throw new InvalidBodyException("Unable to process request")
    }

  protected def withUuid(uuidString: String)(f: UUID => Future[Result]): Future[Result] =
    Try(UUID.fromString(uuidString)) match {
      case Success(uuid) => f(uuid)
      case _             => successful(ErrorNotFound.toHttpResponse)
    }

  protected def withValidUuid(uuidString: String)(f: UUID => Future[Result]): Future[Result] =
    if (UuidValidator.validate(uuidString)) {
      f(UUID.fromString(uuidString))
    } else {
      successful(ErrorNotFound.toHttpResponse)
    }

  private def fieldName(errs: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])]) =
    errs.head._1.toString().substring(1)

  private[controllers] def recovery: PartialFunction[Throwable, Result] = {
    case _: CitizenNotFoundException | _: InvalidNinoException | _: MatchingException =>
      ErrorMatchingFailed.toHttpResponse
    case _: MatchNotFoundException => ErrorNotFound.toHttpResponse
    case e: IllegalArgumentException =>
      ErrorInvalidRequest(e.getMessage).toHttpResponse
  }

  private[controllers] def recoveryWithAudit(correlationId: Option[String], matchId: String, url: String)(
    implicit request: RequestHeader,
    auditHelper: AuditHelper): PartialFunction[Throwable, Result] = {
    case _: MatchNotFoundException =>
      auditHelper.auditApiFailure(correlationId, matchId, request, url, "Not Found")
      ErrorNotFound.toHttpResponse
    case e: InvalidBodyException =>
      auditHelper.auditApiFailure(correlationId, matchId, request, url, e.getMessage)
      ErrorInvalidRequest(e.getMessage).toHttpResponse
    case _: CitizenNotFoundException =>
      auditHelper.auditApiFailure(correlationId, matchId, request, url, "Not Found")
      ErrorMatchingFailedNotFound.toHttpResponse
    case _: InvalidNinoException =>
      auditHelper.auditApiFailure(correlationId, matchId, request, url, "Not Found")
      ErrorMatchingFailedNotFound.toHttpResponse
    case _: MatchingException =>
      auditHelper.auditApiFailure(correlationId, matchId, request, url, "Not Found")
      ErrorMatchingFailedNotFound.toHttpResponse
    case e: InsufficientEnrolments =>
      auditHelper.auditApiFailure(correlationId, matchId, request, url, e.getMessage)
      ErrorUnauthorized("Insufficient Enrolments").toHttpResponse
    case e: AuthorisationException =>
      auditHelper.auditApiFailure(correlationId, matchId, request, url, e.getMessage)
      ErrorUnauthorized(e.getMessage).toHttpResponse
    case tmr: TooManyRequestException =>
      auditHelper.auditApiFailure(correlationId, matchId, request, url, tmr.getMessage)
      ErrorTooManyRequests.toHttpResponse
    case br: BadRequestException =>
      auditHelper.auditApiFailure(correlationId, matchId, request, url, br.getMessage)
      ErrorInvalidRequest(br.getMessage).toHttpResponse
    case e: IllegalArgumentException =>
      auditHelper.auditApiFailure(correlationId, matchId, request, url, e.getMessage)
      ErrorInvalidRequest(e.getMessage).toHttpResponse
    case e =>
      logger.error("Unexpected exception", e)
      auditHelper.auditApiFailure(correlationId, matchId, request, url, e.getMessage)
      ErrorInternalServer("Something went wrong.").toHttpResponse
  }
}

trait PrivilegedAuthentication extends AuthorisedFunctions {

  val environment: String

  def authPredicate(scopes: Iterable[String]): Predicate =
    scopes.map(Enrolment(_): Predicate).reduce(_ or _)

  def authenticate(endpointScopes: Iterable[String], matchId: String)(f: Iterable[String] => Future[Result])(
    implicit hc: HeaderCarrier,
    request: RequestHeader,
    auditHelper: AuditHelper,
    executionContext: ExecutionContext
  ): Future[Result] = {

    if (endpointScopes.isEmpty) throw new Exception("No scopes defined")

    if (environment == Environment.SANDBOX)
      f(endpointScopes.toList)
    else {
      authorised(authPredicate(endpointScopes)).retrieve(Retrievals.allEnrolments) { scopes =>
        auditHelper.auditAuthScopes(matchId, scopes.enrolments.map(_.key).mkString(","), request)

        f(scopes.enrolments.map(_.key))
      }
    }
  }

  def requiresPrivilegedAuthentication(
    body: => Future[Result])(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Result] =
    if (environment == SANDBOX) body
    else authorised(Enrolment("read:individuals-matching"))(body)
}

object Environment {
  val SANDBOX = "SANDBOX"
  val PRODUCTION = "PRODUCTION"
}
