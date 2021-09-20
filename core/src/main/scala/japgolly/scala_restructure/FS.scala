package japgolly.scala_restructure

import scala.annotation.tailrec

final case class FS(asMap: Map[Path, String]) {
  import FS.{ApplyResult, CmdApFailure}

  @inline def isEmpty = asMap.isEmpty
  @inline def nonEmpty = asMap.nonEmpty
  @inline def size = asMap.size

  def exists(p: Path): Boolean =
    asMap.contains(p)

  def apply(cmds: Cmds): ApplyResult =
    cmds.asVector.foldLeft[ApplyResult](Right(this)) { (res, cmd) =>
      res.flatMap(_(cmd))
    }

  def apply(cmd: Cmd): ApplyResult = {
    import CmdApFailure._

    def check(checks: Option[CmdApFailure]*)(whenOk: => FS): ApplyResult = {
      val errors = checks.iterator.flatten.toSet
      if (errors.nonEmpty)
        Left(errors)
      else
        Right(whenOk)
    }

    cmd match {

      case c@ Cmd.Create(file, content) =>
        check(
          validateDoesntExist(CreateFailedFileExists(_, c), file),
        )(FS(asMap.updated(file, content)))

      case c@ Cmd.Update(file, content) =>
        check(
          validateExists(UpdateFailedFileNotFound(_, c), file),
        )(FS(asMap.updated(file, content)))

      case c@ Cmd.Rename(from, to) =>
        check(
          validateExists(RenameFailedSourceNotFound(_, c), from),
          validateDoesntExist(RenameFailedTargetExists(_, c), to),
        ) {
          val content = asMap(from)
          FS(asMap.updated(to, content) - from)
        }

      case Cmd.Delete(file) =>
        check(
          validateExists(DeleteFailedFileNotFound, file),
        )(FS(asMap - file))

    }
  }

  private def validateExists[C <: CmdApFailure](cmd: Path => C, p: Path): Option[C] =
    Option.when(!exists(p))(cmd(p))

  private def validateDoesntExist[C <: CmdApFailure](cmd: Path => C, p: Path): Option[C] =
    Option.when(exists(p))(cmd(p))

  lazy val commonRoot: String = {
    if (asMap.isEmpty)
      ""
    else {
      @tailrec
      def go(p: String, root: String): String =
        if (p.isEmpty)
          root
        else {
          val newSuffix: String =
            p.indexOf('/') match {
              case -1 => p
              case i  => p.take(i + 1)
            }
          val newRoot = root + newSuffix
          if (asMap.keysIterator.forall(_.dir.startsWith(newRoot)))
            go(p.drop(newSuffix.length), newRoot)
          else
            root
        }
      val suffix = go(asMap.keysIterator.next().dir, "")
      if (suffix.nonEmpty && suffix.last != '/')
        suffix + "/"
      else
        suffix
    }
  }

  def ++(fs2: FS): FS =
    FS(asMap ++ fs2.asMap)
}

object FS {
  type ApplyResult = Either[Set[CmdApFailure], FS]

  def empty: FS =
    apply(Map.empty)

  def of(fs: (Path, String)*): FS =
    apply(fs.toMap)

  sealed trait CmdApFailure
  object CmdApFailure {
    final case class CreateFailedFileExists    (file: Path, cmd: Cmd.Create) extends CmdApFailure
    final case class DeleteFailedFileNotFound  (file: Path)                  extends CmdApFailure
    final case class RenameFailedSourceNotFound(file: Path, cmd: Cmd.Rename) extends CmdApFailure
    final case class RenameFailedTargetExists  (file: Path, cmd: Cmd.Rename) extends CmdApFailure
    final case class UpdateFailedFileNotFound  (file: Path, cmd: Cmd.Update) extends CmdApFailure
  }
}