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

package component.uk.gov.hmrc.individualsmatchingapi.stubs

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.HeaderNames.{ACCEPT, AUTHORIZATION, CONTENT_TYPE}
import play.api.inject.guice.GuiceApplicationBuilder
import play.mvc.Http.MimeTypes.JSON
import uk.gov.hmrc.individualsmatchingapi.repository.NinoMatchRepository
import play.api.test.{Helpers, TestServer}

import java.util.concurrent.TimeUnit
import scala.concurrent.Await.result
import scala.concurrent.duration.{Duration, FiniteDuration}
import java.net.ServerSocket

trait BaseSpec
    extends AnyFeatureSpec with BeforeAndAfterAll with BeforeAndAfterEach with Matchers with GuiceOneServerPerSuite
    with GivenWhenThen {
  protected def localEnv = false
  protected def appBuilder: GuiceApplicationBuilder = GuiceApplicationBuilder()
    .configure(
      "auditing.enabled"                           -> false,
      "auditing.traceRequests"                     -> false,
      "microservice.services.auth.port"            -> AuthStub.port,
      "microservice.services.internal-auth.port"   -> InternalAuthStub.port,
      "microservice.services.citizen-details.port" -> CitizenDetailsStub.port,
      "microservice.services.matching.port"        -> MatchingStub.port,
      "mongodb.uri"                                -> "mongodb://localhost:27017/nino-match-repository-it",
      "run.mode"                                   -> "It",
      "versioning.unversionedContexts"             -> List("/match-record"),
      "localEnv"                                   -> localEnv
    )
  implicit override lazy val app: Application = appBuilder.build()

  val timeout: FiniteDuration = Duration(5, TimeUnit.SECONDS)
  val serviceUrl = s"http://localhost:$port"
  val mocks = Seq(AuthStub, InternalAuthStub, CitizenDetailsStub, MatchingStub)
  val mongoRepository: NinoMatchRepository = app.injector.instanceOf[NinoMatchRepository]
  val authToken = "Bearer AUTH_TOKEN"

  val acceptHeaderV1: (String, String) = ACCEPT                        -> "application/vnd.hmrc.1.0+json"
  val acceptHeaderP1: (String, String) = ACCEPT                        -> "application/vnd.hmrc.P1.0+json"
  val acceptHeaderP2: (String, String) = ACCEPT                        -> "application/vnd.hmrc.2.0+json"
  val testCorrelationHeader: (String, String) = "CorrelationId"        -> "188e9400-b636-4a3b-80ba-230a8c72b92a"
  val invalidTestCorrelationHeader: (String, String) = "CorrelationId" -> "test"

  protected def requestHeaders(
    acceptHeader: (String, String) = acceptHeaderV1,
    correlationHeader: (String, String) = testCorrelationHeader
  ) =
    Map(CONTENT_TYPE -> JSON, AUTHORIZATION -> authToken, acceptHeader, correlationHeader)

  protected def errorResponse(message: String) =
    s"""{"code":"INVALID_REQUEST","message":"$message"}"""

  override protected def beforeEach(): Unit = {
    mocks.foreach(m => if (!m.server.isRunning) m.server.start())
    result(mongoRepository.collection.drop().headOption(), timeout)
    result(mongoRepository.ensureIndexes(), timeout)
  }

  override protected def afterEach(): Unit =
    mocks.foreach(_.mock.resetMappings())

  override def afterAll(): Unit = {
    mocks.foreach(_.server.stop())
    result(mongoRepository.collection.drop().headOption(), timeout)
  }
  protected def withConfiguredServer[A](overrides: (String, Any)*)(f: String => A): A = {
    val socket = new ServerSocket(0)
    val freePort = socket.getLocalPort
    socket.close()

    val testApp = appBuilder.configure(overrides*).build()

    Helpers.running(TestServer(freePort, testApp)) {
      f(s"http://127.0.0.1:$freePort")
    }
  }
}

case class MockHost(port: Int) {
  val server = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port))
  val mock = new WireMock("localhost", port)
  val url = s"http://localhost:$port"
}
