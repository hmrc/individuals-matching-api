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

package unit.uk.gov.hmrc.individualsmatchingapi.controllers

import org.scalatest.Matchers
import play.api.http.Status
import play.api.test.FakeRequest
import uk.gov.hmrc.individualsmatchingapi.controllers.MicroserviceHelloWorld
import unit.uk.gov.hmrc.individualsmatchingapi.support.SpecBase


class MicroserviceHelloWorldControllerSpec extends SpecBase with Matchers {

  val fakeRequest = FakeRequest("GET", "/")


  "GET /" should {
    "return 200" in {
      val result = MicroserviceHelloWorld.hello()(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }


}
