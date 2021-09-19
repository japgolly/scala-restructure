package japgolly.scala_restructure

import japgolly.microlibs.testutil.TestUtil._
import utest._
import sourcecode.Line

object PathTest extends TestSuite {

  private def assertUnification(p: String, q: String)(expect: String)(implicit l: Line): Unit = {
    val x = Path(p)
    val y = Path(q)
    val a = x.unify(y).map(_.desc)
    assertEq(a.getOrElse(""), expect)
  }

  override def tests = Tests {
    "unification" - {
      "same"        - assertUnification("a/b/c/omg.scala", "a/b/c/omg.scala")("")
      "ext"         - assertUnification("a/b/c/omg.scala", "a/b/c/omg.ah")("a/b/c/omg.{scala => ah}")
      "name"        - assertUnification("a/b/c/omg.scala", "a/b/c/wow.scala")("a/b/c/{omg => wow}.scala")
      "dir1u"       - assertUnification("a/b/c/omg.scala", "x/b/c/omg.scala")("{a => x}/b/c/omg.scala")
      "dir2u"       - assertUnification("a/b/c/omg.scala", "a/x/c/omg.scala")("a/{b => x}/c/omg.scala")
      "dir3u"       - assertUnification("a/b/c/omg.scala", "a/b/x/omg.scala")("a/b/{c => x}/omg.scala")
      "pathAndName" - assertUnification("a/b/c/omg.scala", "a/b/x/c/wow.scala")("a/b/{c/omg => x/c/wow}.scala")
    }
  }
}
