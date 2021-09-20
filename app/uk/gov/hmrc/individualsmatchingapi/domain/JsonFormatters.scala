/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.individualsmatchingapi.domain

import java.util.UUID

import org.joda.time.LocalDate
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import play.api.libs.json._

import scala.language.implicitConversions
object JsonFormatters {

  implicit val citizenMatchingFormat = Json.format[CitizenMatchingRequest]

  implicit val citizenDetailsFormat = new Format[CitizenDetails] {
    override def reads(json: JsValue): JsResult[CitizenDetails] =
      JsSuccess(
        CitizenDetails(
          (json \ "name" \ "current" \ "firstName").asOpt[String],
          (json \ "name" \ "current" \ "lastName").asOpt[String],
          (json \ "ids" \ "nino").asOpt[String],
          (json \ "dateOfBirth")
            .asOpt[LocalDate](jodaLocalDateReads("ddMMyyyy"))
        ))

    override def writes(citizenDetails: CitizenDetails): JsValue =
      Json.obj(
        "firstName"   -> citizenDetails.firstName,
        "lastName"    -> citizenDetails.lastName,
        "nino"        -> citizenDetails.nino,
        "dateOfBirth" -> citizenDetails.dateOfBirth)
  }

  implicit val detailsMatchRequestFormat = new Format[DetailsMatchRequest] {
    def reads(json: JsValue) =
      JsSuccess(
        DetailsMatchRequest(
          (json \ "verifyPerson").as[CitizenMatchingRequest],
          (json \ "cidPersons").as[List[CitizenDetails]]))

    def writes(matchingRequest: DetailsMatchRequest): JsValue =
      Json.obj("verifyPerson" -> matchingRequest.verifyPerson, "cidPersons" -> matchingRequest.cidPersons)
  }

  implicit val errorResponseWrites = new Writes[ErrorResponse] {
    def writes(e: ErrorResponse): JsValue =
      Json.obj("code" -> e.errorCode, "message" -> e.message)
  }

  implicit val errorInvalidRequestFormat = new Format[ErrorInvalidRequest] {
    def reads(json: JsValue): JsResult[ErrorInvalidRequest] = JsSuccess(
      ErrorInvalidRequest((json \ "message").as[String])
    )

    def writes(error: ErrorInvalidRequest): JsValue =
      Json.obj("code" -> error.errorCode, "message" -> error.message)
  }

  implicit val uuidJsonFormat = new Format[UUID] {
    override def writes(uuid: UUID) = JsString(uuid.toString)

    override def reads(json: JsValue) =
      JsSuccess(UUID.fromString(json.asInstanceOf[JsString].value))
  }

  implicit val ninoMatchJsonFormat = Json.format[NinoMatch]
  implicit val matchedCitizenRecordJsonFormat =
    Json.format[MatchedCitizenRecord]
}
