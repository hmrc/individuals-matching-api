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

package it.uk.gov.hmrc.individualsmatchingapi.repository

import java.util.UUID

import org.scalatest.{BeforeAndAfterEach, Matchers}
import play.api.Configuration
import play.api.inject.guice.GuiceableModule
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.individualsmatchingapi.repository.NinoMatchRepository
import unit.uk.gov.hmrc.individualsmatchingapi.support.SpecBase
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, DefaultPlayMongoRepositorySupport}

import scala.concurrent.ExecutionContext.Implicits.global

class NinoMatchRepositorySpec extends SpecBase with Matchers with BeforeAndAfterEach {

  val ninoMatchTtl = 60

  val bindModules: Seq[GuiceableModule] = Seq()

  protected def databaseName: String = "test-" + this.getClass.getSimpleName
  protected def mongoUri: String = s"mongodb://localhost:27017/$databaseName"

  override lazy val fakeApplication = buildFakeApplication(
    Configuration("mongodb.uri" -> mongoUri, "mongo.ninoMatchTtlInSeconds" -> ninoMatchTtl))

  val nino = Nino("AB123456A")
  val ninoMatchRepository = fakeApplication.injector.instanceOf[NinoMatchRepository]

  override def beforeEach() {
    await(ninoMatchRepository.collection.drop)
    await(ninoMatchRepository.ensureIndexes)
  }

  override def afterEach() {
    await(ninoMatchRepository.collection.drop)
  }

  "collection" should {
    "have a unique index on id" in {
      val indexes = await(ninoMatchRepository.collection.listIndexes().headOption())

      println("ACHI: " + indexes)
//      await(ninoMatchRepository.collection.indexesManager.list()).find({ i =>
//        i.name == Some("idIndex") &&
//          i.key == Seq("id" -> IndexType.Ascending) &&
//          i.background &&
//          i.unique
//      }) should not be None
    }

    "have a non-unique index and expiring ttl on createAt" in {
//      await(ninoMatchRepository.collection.indexesManager.list()).find({ i =>
//        i.key == Seq("createdAt" -> IndexType.Ascending) &&
//          i.name == Some("createdAtIndex") &&
//          i.background &&
//          !i.unique &&
//          i.options == BSONDocument("expireAfterSeconds" -> ninoMatchTtl)
//      }) should not be None
    }
  }

  "create" should {
    "save an ninoMatch" in {
      val ninoMatch = await(ninoMatchRepository.create(nino))

      val storedNinoMatch = await(ninoMatchRepository.read(ninoMatch.id))
      storedNinoMatch shouldBe Some(ninoMatch)
    }

    "allow the same nino to be saved multiple times" in {
      val ninoMatch1 = await(ninoMatchRepository.create(nino))
      val ninoMatch2 = await(ninoMatchRepository.create(nino))

      ninoMatch1 shouldNot be(ninoMatch2)
    }

  }

  "read" should {
    "return the ninoMatch when present in the database" in {
      val ninoMatch = await(ninoMatchRepository.create(nino))

      val result = await(ninoMatchRepository.read(ninoMatch.id))

      result shouldBe Some(ninoMatch)
    }

    "return None when there is no individual for the nino" in {
      val result = await(ninoMatchRepository.read(UUID.randomUUID()))

      result shouldBe None
    }
  }
}
