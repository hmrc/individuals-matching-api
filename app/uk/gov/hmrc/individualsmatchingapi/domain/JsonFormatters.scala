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

package uk.gov.hmrc.individualsmatchingapi.domain

import play.api.libs.json.*

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

object JsonFormatters {

  private val format = DateTimeFormatter.ofPattern("ddMMyyyy")

  implicit val citizenMatchingFormat: OFormat[CitizenMatchingRequest] = Json.format[CitizenMatchingRequest]

  implicit val citizenDetailsFormat: Format[CitizenDetails] = new Format[CitizenDetails] {
    override def reads(json: JsValue): JsResult[CitizenDetails] =
      JsSuccess(
        CitizenDetails(
          (json \ "name" \ "current" \ "firstName").asOpt[String],
          (json \ "name" \ "current" \ "lastName").asOpt[String],
          (json \ "ids" \ "nino").asOpt[String],
          (json \ "dateOfBirth").asOpt[String].map(LocalDate.parse(_, format))
        )
      )

    override def writes(citizenDetails: CitizenDetails): JsValue =
      Json.obj(
        "firstName"   -> citizenDetails.firstName,
        "lastName"    -> citizenDetails.lastName,
        "nino"        -> citizenDetails.nino,
        "dateOfBirth" -> citizenDetails.dateOfBirth
      )
  }

  implicit val detailsMatchRequestFormat: Format[DetailsMatchRequest] = new Format[DetailsMatchRequest] {
    def reads(json: JsValue): JsResult[DetailsMatchRequest] =
      JsSuccess(
        DetailsMatchRequest(
          (json \ "verifyPerson").as[CitizenMatchingRequest],
          (json \ "cidPersons").as[List[CitizenDetails]]
        )
      )

    def writes(matchingRequest: DetailsMatchRequest): JsValue =
      Json.obj("verifyPerson" -> matchingRequest.verifyPerson, "cidPersons" -> matchingRequest.cidPersons)
  }

  implicit val errorResponseWrites: Writes[ErrorResponse] = new Writes[ErrorResponse] {
    def writes(e: ErrorResponse): JsValue =
      Json.obj("code" -> e.errorCode, "message" -> e.message)
  }

  implicit val errorInvalidRequestFormat: Format[ErrorInvalidRequest] = new Format[ErrorInvalidRequest] {
    def reads(json: JsValue): JsResult[ErrorInvalidRequest] = JsSuccess(
      ErrorInvalidRequest((json \ "message").as[String])
    )

    def writes(error: ErrorInvalidRequest): JsValue =
      Json.obj("code" -> error.errorCode, "message" -> error.message)
  }

  implicit val uuidJsonFormat: Format[UUID] = new Format[UUID] {
    override def writes(uuid: UUID): JsValue = JsString(uuid.toString)

    override def reads(json: JsValue): JsResult[UUID] =
      JsSuccess(UUID.fromString(json.asInstanceOf[JsString].value))
  }

  implicit val matchedCitizenRecordJsonFormat: OFormat[MatchedCitizenRecord] =
    Json.format[MatchedCitizenRecord]
}
