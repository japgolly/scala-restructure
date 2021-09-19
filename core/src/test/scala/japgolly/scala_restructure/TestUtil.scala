package japgolly.scala_restructure

import sourcecode.Line

object TestUtil extends japgolly.microlibs.testutil.TestUtil {
  implicit def univEqPath: UnivEq[Path         ] = UnivEq.derive
  implicit def univEqCmd : UnivEq[Cmd          ] = UnivEq.derive
  implicit def univEqCmds: UnivEq[Cmds         ] = UnivEq.derive
  implicit def univEqEngE: UnivEq[Engine.Error ] = UnivEq.derive
  implicit def univEqEngR: UnivEq[Engine.Result] = UnivEq.derive

  implicit def path(s: String): Path =
    Path(s)

  trait EngineTester {
    protected val engine: Engine

    def assertEngineSuccess(contents: (String, String)*)(expect: Cmd*)(implicit q: Line): Unit = {

      // Test pass 1
      val fs = FS(contents.iterator.map { case (f, c) => (path(f), c)}.toMap)
      val cmds = {
        val r = engine.scanFS(fs)
        val e = expect.toVector
        assert(r.errors.isEmpty)
        val a = r.cmds.asVector
        assertSeq(a, e)
        r.cmds
      }

      // Test pass 2: idempotency
      val fs2 = fs(cmds).getOrThrow()
      assertEq("Pass 2: idempotency", engine.scanFS(fs2), Engine.Result.empty)
    }
  }

}
