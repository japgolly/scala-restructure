package japgolly.scala_restructure

sealed trait Cmd {

  final def toCmds: Cmds =
    Cmds(Vector.empty[Cmd] :+ this)

  final def toEngineResult: Engine.Result =
    toCmds.toEngineResult
}

object Cmd {
  final case class Create(file: Path, content: String) extends Cmd
  final case class Update(file: Path, content: String) extends Cmd
  final case class Rename(from: Path, to: Path)        extends Cmd
  final case class Delete(file: Path)                  extends Cmd
}
