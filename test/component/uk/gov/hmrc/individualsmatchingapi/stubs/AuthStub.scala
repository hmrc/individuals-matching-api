/*
 * Copyright 2017 HM Revenue & Customs
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
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.http.Status

object AuthStub extends MockHost(22000) {

  def willAuthorizePrivilegedAuthToken(authBearerToken: String) = {
    mock.register(get(urlEqualTo(s"/authorise/enrolment/read:individuals?confidenceLevel=300"))
      .withHeader(AUTHORIZATION, equalTo(authBearerToken))
      .willReturn(aResponse().withStatus(Status.OK)))
  }

  def willAuthorizeNinoWithAuthToken(nino: String, authBearerToken: String) = {
    mock.register(get(urlEqualTo(s"/authorise/read/paye/$nino?confidenceLevel=50"))
      .withHeader(AUTHORIZATION, equalTo(authBearerToken))
      .willReturn(aResponse().withStatus(Status.OK)))
  }
}