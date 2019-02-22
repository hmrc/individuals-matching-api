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

package uk.gov.hmrc.individualsmatchingapi.controllers

import java.util.UUID

import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, Enrolment}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsmatchingapi.controllers.Environment.SANDBOX
import uk.gov.hmrc.individualsmatchingapi.domain._
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.Future
import scala.concurrent.Future.successful
import scala.util.{Failure, Success, Try}

import scala.concurrent.ExecutionContext.Implicits.global

trait CommonController extends BaseController {

  override protected def withJsonBody[T]
  (f: (T) => Future[Result])(implicit request: Request[JsValue], m: Manifest[T], reads: Reads[T]): Future[Result] = {
    Try(request.body.validate[T]) match {
      case Success(JsSuccess(payload, _)) => f(payload)
      case Success(JsError(errs)) =>
        successful(ErrorInvalidRequest(s"${fieldName(errs)} is required").toHttpResponse)
      case Failure(e) if e.isInstanceOf[ValidationException] =>
        successful(ErrorInvalidRequest(e.getMessage).toHttpResponse)
      case Failure(_) =>
        successful(ErrorInvalidRequest("Unable to process request").toHttpResponse)
    }
  }

  protected def withUuid(uuidString: String)(f: UUID => Future[Result]): Future[Result] = {
    Try(UUID.fromString(uuidString)) match {
      case Success(uuid) => f(uuid)
      case _ => successful(ErrorNotFound.toHttpResponse)
    }
  }

  private def fieldName[T](errs: Seq[(JsPath, Seq[ValidationError])]) = {
    errs.head._1.toString().substring(1)
  }

  private[controllers] def recovery: PartialFunction[Throwable, Result] = {
    case _: CitizenNotFoundException | _: InvalidNinoException | _: MatchingException => ErrorMatchingFailed.toHttpResponse
    case _: MatchNotFoundException => ErrorNotFound.toHttpResponse
    case e: IllegalArgumentException => ErrorInvalidRequest(e.getMessage).toHttpResponse
  }
}

trait PrivilegedAuthentication extends AuthorisedFunctions {

  val environment: String

  def requiresPrivilegedAuthentication(body: => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    if (environment == SANDBOX) body
    else authorised(Enrolment("read:individuals-matching"))(body)
  }
}

object Environment {
  val SANDBOX = "SANDBOX"
  val PRODUCTION = "PRODUCTION"
}
