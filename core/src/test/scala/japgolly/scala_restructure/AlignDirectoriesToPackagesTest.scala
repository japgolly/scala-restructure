package japgolly.scala_restructure

import japgolly.scala_restructure.TestUtil._
import utest._

object AlignDirectoriesToPackagesTest extends TestSuite with EngineTester {

  override protected val engine = AlignDirectoriesToPackages

  override def tests = Tests {

    // -----------------------------------------------------------------------------------------------------------------
    "simple" - assertEngineSuccess(
      "a/b/S.scala" ->
        """// asd
          |/* asd */
          |/** asd */
          |package d.ee.f
          |
          |class Y
          |class Z
          |""".stripMargin
    )(Cmd.Rename("a/b/S.scala", "d/ee/f/S.scala"))

    // -----------------------------------------------------------------------------------------------------------------
    "root" - assertEngineSuccess(
      "a/b/S.scala" ->
        """class Y
          |class Z
          |""".stripMargin
    )(Cmd.Rename("a/b/S.scala", "S.scala"))

    // -----------------------------------------------------------------------------------------------------------------
    "split" - assertEngineSuccess(
      "a/b/S.scala" ->
        """package x.y
          |// ah
          |package q.z
          |import X.R.ASD
          |class Z
          |""".stripMargin
    )(Cmd.Rename("a/b/S.scala", "x/y/q/z/S.scala"))

    // -----------------------------------------------------------------------------------------------------------------
    "multiple" - assertEngineSuccess(
      "a/b/S.scala" ->
        """package x.y
          |class X
          |package q.z { class Z }
          |""".stripMargin
    )(Cmd.Rename("a/b/S.scala", "x/y/S.scala"))

    // -----------------------------------------------------------------------------------------------------------------
    "pkgobj" - assertEngineSuccess(
      "a/b/S.scala" ->
        """package x.y
          |import java.io._
          |package object ok {
          |  type X=Int
          |  val X=1
          |  class Y
          |}
          |""".stripMargin
    )(Cmd.Rename("a/b/S.scala", "x/y/ok/package.scala"))

  }
}
