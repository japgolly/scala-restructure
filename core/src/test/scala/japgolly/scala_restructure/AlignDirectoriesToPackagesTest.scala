package japgolly.scala_restructure

import japgolly.scala_restructure.TestUtil._
import sourcecode.Line
import utest._

object AlignDirectoriesToPackagesTest extends TestSuite {

  override def tests = Tests {

    // -----------------------------------------------------------------------------------------------------------------
    "simple" - test(
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
    "root" - test(
      "a/b/S.scala" ->
        """class Y
          |class Z
          |""".stripMargin
    )(Cmd.Rename("a/b/S.scala", "S.scala"))

    // -----------------------------------------------------------------------------------------------------------------
    "split" - test(
      "a/b/S.scala" ->
        """package x.y
          |// ah
          |package q.z
          |import X.R.ASD
          |class Z
          |""".stripMargin
    )(Cmd.Rename("a/b/S.scala", "x/y/q/z/S.scala"))

    // -----------------------------------------------------------------------------------------------------------------
    "multiple" - test(
      "a/b/S.scala" ->
        """package x.y
          |class X
          |package q.z { class Z }
          |""".stripMargin
    )(Cmd.Rename("a/b/S.scala", "x/y/S.scala"))

    // -----------------------------------------------------------------------------------------------------------------
    "pkgobj" - test(
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

  // ===================================================================================================================

  private def test(contents: (String, String)*)(expect: Cmd*)(implicit q: Line): Unit = {

    // Test pass 1
    val fs = FS(contents.iterator.map { case (f, c) => (path(f), c)}.toMap)
    val cmds = {
      val r = AlignDirectoriesToPackages(fs)
      val e = expect.toVector
      assert(r.errors.isEmpty)
      val a = r.cmds.asVector
      assertSeq(a, e)
      r.cmds
    }

    // Test pass 2: idempotency
    val fs2 = fs(cmds)
    assertEq(AlignDirectoriesToPackages(fs2), Engine.Result.empty)
  }
}
