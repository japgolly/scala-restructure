package japgolly.scala_restructure

import japgolly.scala_restructure.TestUtil._
import utest._

object AlignFilesToTypesTest extends TestSuite with EngineTester {

  override protected val engine = AlignFileToTypes

  override def tests = Tests {

    // -----------------------------------------------------------------------------------------------------------------
    "twoDiff" - assertEngineSuccess(
      "dir/S.scala" ->
        """// ah
          |package a.b
          |package c.d
          |
          |// JAVA!
          |import java.net._
          |
          |// yay
          |class Yay
          |
          |import java.io._
          |trait Z
          |
          |
          |/** blah1
          |  * blah2
          |  */
          |object Yay { def hehe = 1 }
          |""".stripMargin
    )(
      Cmd.Delete("dir/S.scala"),
      Cmd.Write("dir/Yay.scala",
        """// ah
          |package a.b
          |package c.d
          |
          |// JAVA!
          |import java.net._
          |
          |// yay
          |class Yay
          |
          |import java.io._
          |
          |/** blah1
          |  * blah2
          |  */
          |object Yay { def hehe = 1 }
          |""".stripMargin
      ),
      Cmd.Write("dir/Z.scala",
        """// ah
          |package a.b
          |package c.d
          |
          |// JAVA!
          |import java.net._
          |
          |import java.io._
          |trait Z
          |""".stripMargin
      ),
    )

    // -----------------------------------------------------------------------------------------------------------------
    "plusOne" - assertEngineSuccess(
      "dir/S.scala" ->
        """package x
          |
          |// JAVA1
          |import java.io._
          |
          |object S
          |
          |/** omg */
          |object Y
          |
          |import java.net._
          |
          |// JAVA2
          |
          |class S
          |""".stripMargin
    )(
      Cmd.Write("dir/S.scala",
        """package x
          |
          |// JAVA1
          |import java.io._
          |
          |object S
          |
          |import java.net._
          |
          |// JAVA2
          |
          |class S
          |""".stripMargin
      ),
      Cmd.Write("dir/Y.scala",
        """package x
          |
          |// JAVA1
          |import java.io._
          |
          |/** omg */
          |object Y
          |""".stripMargin
      ),
    )

    // -----------------------------------------------------------------------------------------------------------------
    "pkgObj" - assertEngineSuccess(
      "dir/S.scala" ->
        """package x.y
          |
          |// JAVA1
          |import java.io._
          |
          |package object S {
          |  type A = Int
          |  object O
          |  class C
          |  def d = 1
          |}
          |""".stripMargin
    )(
      Cmd.Rename("dir/S.scala","dir/package.scala"),
    )

    // -----------------------------------------------------------------------------------------------------------------
    "pkgObj2" - assertEngineSuccess(
      "dir/S.scala" ->
        """package x.y
          |
          |// JAVA1
          |import java.io._
          |
          |package object S {
          |  type A = Int
          |  object O
          |  class C
          |  def d = 1
          |}
          |
          |// JAVA2
          |import java.net._
          |
          |package object Q
          |""".stripMargin
    )(
      Cmd.Rename("dir/S.scala","dir/package.scala"),
    )

    // -----------------------------------------------------------------------------------------------------------------
    "mix" - assertEngineSuccess(
      "dir/S.scala" ->
        """package x.y
          |
          |// JAVA1
          |import java.io._
          |
          |package object Q
          |package object S {
          |  type A = Int
          |  object O
          |  class C
          |  def d = 1
          |}
          |
          |// JAVA2
          |
          |class Y
          |""".stripMargin
    )(
      Cmd.Delete("dir/S.scala"),
      Cmd.Write("dir/package.scala",
        """package x.y
          |
          |// JAVA1
          |import java.io._
          |
          |package object Q
          |package object S {
          |  type A = Int
          |  object O
          |  class C
          |  def d = 1
          |}
          |""".stripMargin
      ),
      Cmd.Write("dir/Y.scala",
        """package x.y
          |
          |// JAVA1
          |import java.io._
          |
          |// JAVA2
          |
          |class Y
          |""".stripMargin
      ),
    )

  }
}
