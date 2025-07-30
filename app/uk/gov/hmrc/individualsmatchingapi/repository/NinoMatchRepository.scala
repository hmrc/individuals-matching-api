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

package uk.gov.hmrc.individualsmatchingapi.repository

import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions}
import play.api.Configuration
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.individualsmatchingapi.domain.NinoMatch
import uk.gov.hmrc.individualsmatchingapi.repository.MongoErrors.Duplicate
import uk.gov.hmrc.mdc.Mdc.preservingMdc
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.UUID
import java.util.UUID.randomUUID
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NinoMatchRepository @Inject() (mongo: MongoComponent, configuration: Configuration)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[NinoMatch](
      mongoComponent = mongo,
      collectionName = "ninoMatch",
      domainFormat = NinoMatch.format,
      replaceIndexes = true,
      indexes = Seq(
        IndexModel(ascending("id"), IndexOptions().name("idIndex").unique(true).background(true)),
        IndexModel(
          ascending("createdAt"),
          IndexOptions()
            .name("createdAtIndex")
            .expireAfter(
              configuration.getOptional[Int]("mongodb.ninoMatchTtlInSeconds").getOrElse(60 * 60 * 5).toLong,
              TimeUnit.SECONDS
            )
            .background(true)
        )
      )
    ) {

  def create(nino: Nino): Future[NinoMatch] = {
    val ninoMatch = NinoMatch(nino, generateUuid)

    preservingMdc {
      collection
        .insertOne(ninoMatch)
        .toFuture()
        .map(_ => ninoMatch)
        .recover { case Duplicate(_) =>
          throw new RuntimeException(s"failed to persist nino match $ninoMatch")
        }
    }
  }

  def read(id: UUID): Future[Option[NinoMatch]] =
    preservingMdc {
      collection.find(Filters.equal("id", toBson(id))).headOption()
    }

  private def generateUuid = randomUUID()
}
