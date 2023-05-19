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

package unit.uk.gov.hmrc.individualsmatchingapi.util

import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.individualsmatchingapi.utils.UuidValidator
import unit.uk.gov.hmrc.individualsmatchingapi.support.SpecBase

import java.util.UUID

class UuidValidatorSpec extends SpecBase with Matchers with ScalaCheckPropertyChecks {

  private val invalidUuid = "0-0-0-0-0"

  "Return true on a valid lower-cased UUID" in {
    forAll { uuid: UUID =>
      UuidValidator.validate(uuid.toString) shouldBe true
    }
  }

  "Return true on a valid upper-cased UUID" in {
    forAll { uuid: UUID =>
      UuidValidator.validate(uuid.toString.toUpperCase) shouldBe true
    }
  }

  "Return false on invalid UUID" in {
    UuidValidator.validate(invalidUuid) shouldBe false
  }
}
