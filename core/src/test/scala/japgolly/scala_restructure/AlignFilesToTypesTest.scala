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
      Cmd.Create("dir/Yay.scala",
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
      Cmd.Create("dir/Z.scala",
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
      Cmd.Create("dir/Y.scala",
        """package x
          |
          |// JAVA1
          |import java.io._
          |
          |/** omg */
          |object Y
          |""".stripMargin
      ),
      Cmd.Update("dir/S.scala",
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
    )

    // -----------------------------------------------------------------------------------------------------------------
    "pkgObj" - assertEngineSuccess(
      "dir/S.scala" ->
        """package x.y
          |
          |// JAVA1
          |import java.io._
          |
          |package object s {
          |  type A = Int
          |  object O
          |  class C
          |  def d = 1
          |}
          |""".stripMargin
    )(
      Cmd.Rename("dir/S.scala", "dir/s/package.scala"),
    )

    // -----------------------------------------------------------------------------------------------------------------
    "mix" - assertEngineSuccess(
      "dir/S.scala" ->
        """package x.y
          |
          |// JAVA1
          |import java.io._
          |
          |package object s {
          |  type A = Int
          |  object O
          |  class C
          |  def d = 1
          |}
          |
          |// JAVA2
          |import java.net._
          |
          |/** hehe */
          |package object q
          |
          |// Why?
          |class Y
          |""".stripMargin
    )(
      Cmd.Delete("dir/S.scala"),
      Cmd.Create("dir/s/package.scala",
        """package x.y
          |
          |// JAVA1
          |import java.io._
          |
          |package object s {
          |  type A = Int
          |  object O
          |  class C
          |  def d = 1
          |}
          |""".stripMargin
      ),
      Cmd.Create("dir/q/package.scala",
        """package x.y
          |
          |// JAVA1
          |import java.io._
          |
          |// JAVA2
          |import java.net._
          |
          |/** hehe */
          |package object q
          |""".stripMargin
      ),
      Cmd.Create("dir/Y.scala",
        """package x.y
          |
          |// JAVA1
          |import java.io._
          |
          |// JAVA2
          |import java.net._
          |
          |// Why?
          |class Y
          |""".stripMargin
      ),
    )

  }
}
