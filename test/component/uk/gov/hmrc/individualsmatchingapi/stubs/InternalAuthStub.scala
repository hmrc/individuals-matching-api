/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.http.HeaderNames.AUTHORIZATION

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping

object InternalAuthStub extends MockHost(22003) {

  def willAuthorizeToken(authBearerToken: String): StubMapping =
    mock.register(
      post(urlEqualTo("/internal-auth/auth"))
        .withHeader(AUTHORIZATION, equalTo(authBearerToken))
        .willReturn(okJson("""{ "retrievals": [ true ] }"""))
    )

  def willNotAuthorizeToken(authBearerToken: String): StubMapping =
    mock.register(
      post(urlEqualTo("/internal-auth/auth"))
        .withHeader(AUTHORIZATION, equalTo(authBearerToken))
        .willReturn(forbidden())
    )

  def verifyAuthRequestCount(expectedCount: Int): Unit =
    server.verify(expectedCount, postRequestedFor(urlEqualTo("/internal-auth/auth")))
}
