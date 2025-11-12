/*
 * Copyright 2025 HM Revenue & Customs
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

package unit.uk.gov.hmrc.individualsmatchingapi.domain

import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.individualsmatchingapi.domain.NinoMatch

import java.util.UUID
import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeParseException

class NinoMatchSpec extends AnyWordSpec with Matchers {

  "ModifiedDetails JSON format" should {

    val validNino = "CS700100A"
    val nino = Nino(validNino)
    val uuid = UUID.randomUUID()
    val createdAt: Instant = Instant.now()
    val dateTimeCreatedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)

    val ninoMatch = NinoMatch(nino, uuid, createdAt)
    val json = Json.toJson(ninoMatch)

    "serialize ModifiedDetails to JSON correctly" in {

      val ninoMatch = NinoMatch(nino, uuid, createdAt)

      Json.toJson(ninoMatch) shouldBe json
    }

    "deserialize JSON to ModifiedDetails correctly" in {

      val result = json.validate[NinoMatch]

      result.isSuccess shouldBe true
      result.get.createdAt.truncatedTo(ChronoUnit.SECONDS) shouldBe ninoMatch.createdAt.truncatedTo(ChronoUnit.SECONDS)
    }

    "deserialize JSON when createdAt is a string" in {
      val ninoMatchJson = Json.obj(
        "nino"      -> validNino,
        "id"        -> uuid.toString,
        "createdAt" -> createdAt.toString
      )

      val result = ninoMatchJson.validate[NinoMatch]

      result.isSuccess shouldBe true
      result.get.createdAt shouldBe createdAt
    }

    "deserialize JSON when createdAt is a string without offset" in {
      val ninoMatchJson = Json.obj(
        "nino"      -> validNino,
        "id"        -> uuid.toString,
        "createdAt" -> dateTimeCreatedAt.toString
      )

      val result = ninoMatchJson.validate[NinoMatch]

      result.isSuccess shouldBe true
      result.get.createdAt shouldBe dateTimeCreatedAt.toInstant(ZoneOffset.UTC)
    }

    "deserialize JSON when createdAt is a Mongo ISODate object" in {
      val ninoMatchJson = Json.obj(
        "nino" -> validNino,
        "id"   -> uuid.toString,
        "createdAt" -> Json.obj(
          "$date" -> Json.obj(
            "$numberLong" -> createdAt.toEpochMilli.toString
          )
        )
      )

      val result = ninoMatchJson.validate[NinoMatch]

      result.isSuccess shouldBe true
      result.get.createdAt shouldBe createdAt.truncatedTo(ChronoUnit.MILLIS)
    }

    "fail to deserialize invalid JSON" in {
      val invalidJson = Json.obj(
        "nino"      -> validNino,
        "id"        -> uuid.toString,
        "createdAt" -> "not a date"
      )

      intercept[DateTimeParseException](invalidJson.validate[NinoMatch])
    }
  }
}
