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

package unit.uk.gov.hmrc.individualsmatchingapi.services

import org.mockito.IdiomaticMockito
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.individualsmatchingapi.services.PathTree

class PathTreeSpec extends AnyWordSpec with Matchers with IdiomaticMockito with BeforeAndAfterEach {

  "PathTree" should {

    val paths = List("x/y/z", "x/y/t", "x/y/t/v", "x/a/b", "c/d/e", "x/b/f")
    val pathtree: PathTree = PathTree(paths)

    "parse paths correctly" in {
      pathtree.listChildren.size shouldBe 2
      pathtree.hasChild("x") shouldBe true
      pathtree.hasChild("c") shouldBe true

      val x = pathtree.getChild("x").get
      x.listChildren.size shouldBe 3
      x.hasChild("y") shouldBe true
      x.hasChild("a") shouldBe true
      x.hasChild("b") shouldBe true

      val y = x.getChild("y").get
      y.listChildren.size shouldBe 2
      y.hasChild("z") shouldBe true
      y.hasChild("t") shouldBe true

      val a = x.getChild("a").get
      a.listChildren.size shouldBe 1
      a.hasChild("b") shouldBe true
    }

    "serialise to query string correctly" in {
      val expectedString = "c(d(e)),x(a(b),b(f),y(t(v),z))"
      pathtree.toString shouldBe expectedString
    }
  }
}
