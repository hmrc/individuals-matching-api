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

package component.uk.gov.hmrc.individualsmatchingapi.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.individualsmatchingapi.domain.{CitizenDetails, CitizenMatchingRequest}

object MatchingStub extends MockHost(22002) {

  def performMatchReturnsNoErrorCodes(
    matchingRequest: CitizenMatchingRequest,
    citizenDetails: CitizenDetails): StubMapping =
    mock.register(
      post(urlPathEqualTo(s"/matching/perform-match/cycle3"))
        .withRequestBody(equalToJson(
          s"""{
              "verifyPerson": {
              "firstName":"${matchingRequest.firstName}",
              "lastName":"${matchingRequest.lastName}",
              "nino":"${matchingRequest.nino}",
              "dateOfBirth":"${matchingRequest.dateOfBirth}"
            },
            "cidPersons":[{
              "firstName":"${citizenDetails.firstName.get}",
              "lastName":"${citizenDetails.lastName.get}",
              "nino":"${citizenDetails.nino.get}",
              "dateOfBirth":"${citizenDetails.dateOfBirth.get}"}]
            }
          """
        ))
        .willReturn(aResponse().withStatus(200).withBody("""{"errorCodes":[]}""")))

  def performMatchReturnsErrorCodes(
    matchingRequest: CitizenMatchingRequest,
    citizenDetails: CitizenDetails): StubMapping =
    mock.register(
      post(urlPathEqualTo(s"/matching/perform-match/cycle3"))
        .withRequestBody(equalToJson(
          s"""{
              "verifyPerson": {
              "firstName":"${matchingRequest.firstName}",
              "lastName":"${matchingRequest.lastName}",
              "nino":"${matchingRequest.nino}",
              "dateOfBirth":"${matchingRequest.dateOfBirth}"
            },
            "cidPersons":[{
              "firstName":"${citizenDetails.firstName.get}",
              "lastName":"${citizenDetails.lastName.get}",
              "nino":"${citizenDetails.nino.get}",
              "dateOfBirth":"${citizenDetails.dateOfBirth.get}"}]
            }
          """
        ))
        .willReturn(aResponse().withStatus(200).withBody("""{"errorCodes":[31,34]}""")))
}
