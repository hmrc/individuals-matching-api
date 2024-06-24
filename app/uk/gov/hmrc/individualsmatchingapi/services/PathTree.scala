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

package uk.gov.hmrc.individualsmatchingapi.services

class PathTree(node: B) {

  def listChildren: Iterable[String] = node.listChildren

  def children: Iterable[Node] = node.children

  def hasChild(key: String): Boolean = node.hasChild(key)

  def getChild(key: String): Option[Node] = node.getChild(key)

  override def toString: String =
    children.toList.sortBy(c => c.get).map(c => c.toString).mkString(",")
}

object PathTree {

  def apply(paths: Iterable[String], divider: String): PathTree = {
    def pathToNode(p: Iterable[String]): Node =
      if (p.size == 1) {
        L(p.head)
      } else {
        B(p.head, Seq(pathToNode(p.tail)))
      }

    new PathTree(
      B("root", Seq()) ++ paths
        .map(path => path.split(divider))
        .map(path => pathToNode(path))
    )
  }

  def apply(paths: Iterable[String]): PathTree =
    PathTree(paths, "/")
}

sealed abstract class Node {

  def get: String

  def listChildren: Iterable[String] = List()

  def hasChild(key: String): Boolean = false

  def getChild(key: String): Option[Node] = None

  private[services] def +(node: Node): B

  private[services] def ++(nodes: Iterable[Node]): B
}

case class L(value: String) extends Node {

  def get: String = this.value

  private[services] def +(node: Node): B = B(value, Seq(node))

  private[services] def ++(nodes: Iterable[Node]): B =
    this + nodes.head ++ nodes.tail

  override def toString: String = value
}

case class B(value: String, children: Iterable[Node]) extends Node {

  def get: String = this.value

  private[services] def +(node: Node): B =
    getChild(node.get)
      .map(child => B(value, children.filter(n => n.get != child.get) ++ Seq(merge(child, node))))
      .getOrElse(B(value, children ++ Seq(node)))

  private[services] def ++(nodes: Iterable[Node]): B =
    nodes.foldLeft(this)((a: Node, b: Node) => a + b)

  override def hasChild(key: String): Boolean =
    children.exists(child => child.get == key)

  override def getChild(key: String): Option[Node] =
    children.find(child => child.get == key)

  override def listChildren: Iterable[String] = children.map(child => child.get)

  override def toString: String =
    s"$value(${children.toList.sortBy(c => c.get).mkString(",")})"

  private def merge(a1: Node, b1: Node): Node =
    (a1, b1) match {
      case (a: B, b: B) =>
        B(
          a.get,
          a.children.map(n1 =>
            if (b.hasChild(n1.get)) {
              merge(n1, b.getChild(n1.get).get)
            } else {
              n1
            }
          ) ++ b.children.filter(n1 => !a.hasChild(n1.get))
        )
      case (a: L, b: B) => b
      case _            => a1
    }
}
