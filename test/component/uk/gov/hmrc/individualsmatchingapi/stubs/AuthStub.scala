/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.{JsArray, Json}
import uk.gov.hmrc.auth.core.Enrolment

object AuthStub extends MockHost(22000) {

  def willAuthorizePrivilegedAuthToken(authBearerToken: String): StubMapping = {
    mock.register(post(urlEqualTo("/auth/authorise"))
      .withRequestBody(equalToJson(privilegedAuthority.toString()))
      .withHeader(AUTHORIZATION, equalTo(authBearerToken))
      .willReturn(aResponse()
        .withStatus(Status.OK)
        .withBody("""{"internalId": "some-id"}""")))
  }

  def willNotAuthorizePrivilegedAuthToken(authBearerToken: String): StubMapping = {
    mock.register(post(urlEqualTo("/auth/authorise"))
      .withRequestBody(equalToJson(privilegedAuthority.toString()))
      .withHeader(AUTHORIZATION, equalTo(authBearerToken))
      .willReturn(aResponse()
        .withStatus(Status.UNAUTHORIZED)
        .withHeader(HeaderNames.WWW_AUTHENTICATE, """MDTP detail="InsufficientConfidenceLevel"""")))
  }

  val privilegedAuthority = Json.obj(
    "authorise" -> Json.arr(Json.toJson(Enrolment("read:individuals-matching"))),
    "retrieve" -> JsArray()
  )
}