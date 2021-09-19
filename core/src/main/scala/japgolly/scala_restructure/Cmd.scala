package japgolly.scala_restructure

object Cmd {
  final case class Create(file: Path, content: String) extends Cmd
  final case class Update(file: Path, content: String) extends Cmd
  final case class Rename(from: Path, to: Path) extends Cmd
  final case class Delete(file: Path) extends Cmd
}

sealed trait Cmd {
  import Cmd._

  final def toCmds: Cmds =
    Cmds(Vector.empty[Cmd] :+ this)

  final def toEngineResult: Engine.Result =
    toCmds.toEngineResult

  final def files: List[Path] =
    this match {
      case Create(f, _) => f :: Nil
      case Update(f, _) => f :: Nil
      case Rename(f, g) => f :: g :: Nil
      case Delete(f)    => f :: Nil
    }
}
