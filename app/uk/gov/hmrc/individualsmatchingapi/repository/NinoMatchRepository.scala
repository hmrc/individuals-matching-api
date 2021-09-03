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

package uk.gov.hmrc.individualsmatchingapi.repository

import java.util.UUID
import java.util.UUID.randomUUID
import javax.inject.{Inject, Singleton}

import play.api.Configuration
import play.api.libs.json.Json
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.MultiBulkWriteResult
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.individualsmatchingapi.domain.{JsonFormatters, NinoMatch}
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NinoMatchRepository @Inject()(mongoConnectionProvider: MongoConnectionProvider, configuration: Configuration)(
  implicit ec: ExecutionContext)
    extends ReactiveRepository[NinoMatch, UUID](
      "ninoMatch",
      mongoConnectionProvider.mongoDatabase,
      JsonFormatters.ninoMatchJsonFormat,
      JsonFormatters.uuidJsonFormat) {

  private lazy val ninoMatchTtl =
    configuration.getOptional[Int]("mongo.ninoMatchTtlInSeconds").getOrElse(60 * 60 * 5)

  override lazy val indexes = Seq(
    Index(Seq(("id", Ascending)), Some("idIndex"), background = true, unique = true),
    Index(
      Seq(("createdAt", Ascending)),
      Some("createdAtIndex"),
      options = BSONDocument("expireAfterSeconds" -> ninoMatchTtl),
      background = true)
  )

  def create(nino: Nino): Future[NinoMatch] = {
    val ninoMatch = NinoMatch(nino, generateUuid)
    insert(ninoMatch) map { writeResult =>
      if (writeResult.n == 1) ninoMatch
      else throw new RuntimeException(s"failed to persist nino match $ninoMatch")
    }
  }

  def read(uuid: UUID): Future[Option[NinoMatch]] = findById(uuid)

  override def findById(id: UUID, readPreference: ReadPreference)(
    implicit ec: ExecutionContext): Future[Option[NinoMatch]] =
    collection.find(Json.obj("id" -> id.toString)).one[NinoMatch]

  override def bulkInsert(entities: Seq[NinoMatch])(implicit ec: ExecutionContext): Future[MultiBulkWriteResult] =
    throw new UnsupportedOperationException

  private def generateUuid = randomUUID()
}
