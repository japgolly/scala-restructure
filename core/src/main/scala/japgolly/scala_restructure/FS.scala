package japgolly.scala_restructure

final case class FS(asMap: Map[Path, String]) {
  import FS.CmdApFailure

  type ApplyResult = Either[Set[CmdApFailure], FS]

  def apply(cmd: Cmd): ApplyResult = {

    def check(checks: Option[CmdApFailure]*)(whenOk: => FS): ApplyResult = {
      val errors = checks.iterator.flatten.toSet
      if (errors.nonEmpty)
        Left(errors)
      else
        Right(whenOk)
    }

    cmd match {

      case Cmd.Create(file, content) =>
        check(
          validateDoesntExist(file),
        )(FS(asMap.updated(file, content)))

      case Cmd.Update(file, content) =>
        check(
          validateExists(file),
        )(FS(asMap.updated(file, content)))

      case Cmd.Rename(from, to) =>
        check(
          validateExists(from),
          validateDoesntExist(to),
        ) {
          val content = asMap(from)
          FS(asMap.updated(to, content) - from)
        }

      case Cmd.Delete(file) =>
        check(
          validateExists(file),
        )(FS(asMap - file))

    }
  }

  private def validateExists(file: Path): Option[CmdApFailure.FileNotFound] =
    Option.unless(asMap.contains(file))(CmdApFailure.FileNotFound(file))

  private def validateDoesntExist(file: Path): Option[CmdApFailure.FileAlreadyExists] =
    Option.when(asMap.contains(file))(CmdApFailure.FileAlreadyExists(file))

  def apply(cmds: Cmds): ApplyResult =
    cmds.asVector.foldLeft[ApplyResult](Right(this)) { (res, cmd) =>
      res.flatMap(_(cmd))
    }
}

object FS {
  def empty: FS =
    apply(Map.empty)

  def of(fs: (Path, String)*): FS =
    apply(fs.toMap)

  sealed trait CmdApFailure
  object CmdApFailure {
    final case class FileAlreadyExists(file: Path) extends CmdApFailure
    final case class FileNotFound     (file: Path) extends CmdApFailure
  }
}