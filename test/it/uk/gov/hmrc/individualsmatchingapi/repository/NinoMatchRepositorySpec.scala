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

package it.uk.gov.hmrc.individualsmatchingapi.repository

import org.mongodb.scala.model.IndexModel
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import play.api.inject.guice.GuiceableModule
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.{Application, Configuration}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.individualsmatchingapi.repository.NinoMatchRepository
import unit.uk.gov.hmrc.individualsmatchingapi.support.SpecBase

import java.util.UUID

class NinoMatchRepositorySpec extends SpecBase with Matchers with BeforeAndAfterEach {

  val ninoMatchTtl = 60

  val bindModules: Seq[GuiceableModule] = Seq()

  protected val databaseName: String = "test-" + this.getClass.getSimpleName
  protected val mongoUri: String =
    s"mongodb://127.0.0.1:27017/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"

  override lazy val fakeApplication: Application = buildFakeApplication(
    Configuration("mongodb.uri" -> mongoUri, "mongodb.ninoMatchTtlInSeconds" -> ninoMatchTtl))

  val nino: Nino = Nino("AB123456A")
  val ninoMatchRepository: NinoMatchRepository = fakeApplication.injector.instanceOf[NinoMatchRepository]

  override def beforeEach(): Unit = {
    ninoMatchRepository.collection.drop()
    await(ninoMatchRepository.ensureIndexes)
  }

  override def afterEach(): Unit =
    ninoMatchRepository.collection.drop()

  "collection" should {
    val indices = await(
      ninoMatchRepository.collection.listIndexes[Seq[IndexModel]].toFuture()
    ).toString
    "have the idIndex" in {
      indices should include(
        "name -> idIndex, " +
          "background -> true, " +
          "key -> Map(id -> 1), " +
          "v -> 2, " +
          "unique -> true")
    }

    "have the createdAtIndex" in {
      indices should include(
        "name -> createdAtIndex, " +
          "background -> true, " +
          "key -> Map(createdAt -> 1), " +
          "v -> 2, " +
          s"expireAfterSeconds -> $ninoMatchTtl")
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
