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

package unit.uk.gov.hmrc.individualsmatchingapi.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.matchers.should.Matchers
import play.api.Configuration
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.individualsmatchingapi.connectors.MatchingConnector
import uk.gov.hmrc.individualsmatchingapi.domain._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import unit.uk.gov.hmrc.individualsmatchingapi.support.SpecBase

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MatchingConnectorSpec extends SpecBase with Matchers with WireMockSupport {
  val http: DefaultHttpClient = fakeApplication.injector.instanceOf[DefaultHttpClient]
  val config: Configuration = fakeApplication.injector.instanceOf[Configuration]
  val servicesConfig: ServicesConfig = fakeApplication.injector.instanceOf[ServicesConfig]

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val underTest: MatchingConnector = new MatchingConnector(config, http, servicesConfig) {
      override val serviceUrl = s"http://$wireMockHost:$wireMockPort"
    }
  }

  "isMatch" should {
    "succeed for a citizen with matching details" in new Setup {
      stubFor(
        post(urlMatching(s"/matching/perform-match/cycle3"))
          .withRequestBody(
            equalToJson(
              s"""{
                "verifyPerson": {
                "firstName":"John",
                "lastName":"Smith",
                "nino":"NA000799C",
                "dateOfBirth":"1972-10-15"
              },
              "cidPersons":[{
                "firstName":"John",
                "lastName":"Smith",
                "nino":"NA000799C",
                "dateOfBirth":"1972-10-15"}]
              }
            """
            )
          )
          .willReturn(aResponse().withStatus(200).withBody("""{"errorCodes":[]}"""))
      )

      val f: Future[Unit] =
        underTest.validateMatch(DetailsMatchRequest(citizenMatchingRequest(), Seq(citizenDetails())))
      noException should be thrownBy await(f)
    }

    "throw matching exception for a citizen with non-matching details" in new Setup {
      stubFor(
        post(urlMatching(s"/matching/perform-match/cycle3"))
          .withRequestBody(
            equalToJson(
              s"""{
                "verifyPerson": {
                "firstName":"John",
                "lastName":"Smith",
                "nino":"NA000799C",
                "dateOfBirth":"1972-10-15"
              },
              "cidPersons":[{
                "firstName":"Ana",
                "lastName":"Smith",
                "nino":"NA000799C",
                "dateOfBirth":"1972-10-14"}]
              }
            """
            )
          )
          .willReturn(aResponse().withStatus(200).withBody("""{"errorCodes":[31,32]}"""))
      )

      intercept[MatchingException](
        await(
          underTest.validateMatch(
            DetailsMatchRequest(
              citizenMatchingRequest(),
              Seq(citizenDetails(firstName = Some("Ana"), dateOfBirth = Some(LocalDate.parse("1972-10-14"))))
            )
          )
        )
      )
    }

    "throw matching exception for an invalid json response" in new Setup {
      stubFor(
        post(urlMatching(s"/matching/perform-match/cycle3"))
          .withRequestBody(
            equalToJson(
              s"""{
                "verifyPerson": {
                  "firstName":"John",
                  "lastName":"Smith",
                  "nino":"NA000799C",
                  "dateOfBirth":"1972-10-15"
                },
                "cidPersons":[{
                  "firstName":"John",
                  "lastName":"Smith",
                  "nino":"NA000799C",
                  "dateOfBirth":"1972-10-15"
                }]
              }
            """
            )
          )
          .willReturn(aResponse().withStatus(200).withBody("""{"invalid":"value"}"""))
      )

      intercept[MatchingException](
        await(underTest.validateMatch(DetailsMatchRequest(citizenMatchingRequest(), Seq(citizenDetails()))))
      )
    }
  }

  private def citizenMatchingRequest(
    firstName: String = "John",
    lastName: String = "Smith",
    nino: String = "NA000799C",
    dateOfBirth: String = "1972-10-15"
  ) =
    CitizenMatchingRequest(firstName, lastName, nino, dateOfBirth)

  private def citizenDetails(
    firstName: Option[String] = Some("John"),
    lastName: Option[String] = Option("Smith"),
    nino: Option[String] = Some("NA000799C"),
    dateOfBirth: Option[LocalDate] = Some(LocalDate.parse("1972-10-15"))
  ) =
    CitizenDetails(firstName, lastName, nino, dateOfBirth)
}
