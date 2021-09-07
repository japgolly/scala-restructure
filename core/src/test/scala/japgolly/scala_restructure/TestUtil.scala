package japgolly.scala_restructure

object TestUtil extends japgolly.microlibs.testutil.TestUtil {
  implicit def univEqPath: UnivEq[Path         ] = UnivEq.derive
  implicit def univEqCmd : UnivEq[Cmd          ] = UnivEq.derive
  implicit def univEqCmds: UnivEq[Cmds         ] = UnivEq.derive
  implicit def univEqEngE: UnivEq[Engine.Error ] = UnivEq.derive
  implicit def univEqEngR: UnivEq[Engine.Result] = UnivEq.derive

  implicit def path(s: String): Path =
    Path(s)
}