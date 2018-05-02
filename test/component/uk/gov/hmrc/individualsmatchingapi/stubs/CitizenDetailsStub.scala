/*
 * Copyright 2018 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlPathEqualTo}
import play.api.http.Status

object CitizenDetailsStub extends MockHost(22001) {

  def getByNinoReturnsCitizenDetails(nino: String, firstName: String, lastName: String, dateOfBirth: String) = {
    mock.register(get(urlPathEqualTo(s"/citizen-details/nino/$nino"))
      .willReturn(aResponse().withStatus(Status.OK)
        .withBody(
          s"""{
                "name":{
                  "current":{"firstName":"$firstName","lastName":"$lastName"},
                  "previous":[]
                },
                "ids":{"sautr":"2432552635","nino":"$nino"},
                "dateOfBirth":"$dateOfBirth"
              }""")))
  }

  def getByNinoReturnsError(nino: String, errorCode: Int, body: String = ""): Unit = {
    mock.register(get(urlPathEqualTo(s"/citizen-details/nino/$nino"))
      .willReturn(aResponse().withStatus(errorCode).withBody(body)))
  }
}
