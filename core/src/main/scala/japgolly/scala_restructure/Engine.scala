package japgolly.scala_restructure

import scala.meta._
import scala.meta.parsers.Parsed

trait Engine { self =>

  def scanFile(file: Path, content: String): Engine.Result

  final def scanFS(fs: FS): Engine.Result =
    fs.asMap.foldLeft(Engine.Result.empty) { case (r, (path, content)) =>
      r ++ scanFile(path, content)
    }

  final def apply(res: Engine.Result, fs: FS): Engine.ApResult =
    if (res.errors.nonEmpty)
      Left(Left(res.errors))
    else
      fs(res.cmds) match {
        case Right(fs2) => Right(fs2)
        case Left(e)    => Left(Right(e))
      }

  final def &(next: Engine): Engine =
    new Engine {
      override def scanFile(file: Path, content: String): Engine.Result =
        self.scanFile(file, content) ++ next.scanFile(file, content)
    }
}

object Engine {

  trait Simple extends Engine {
    protected def process(file: Path, src: Source): Engine.Result

    override final def scanFile(file: Path, content: String): Engine.Result =
      content.parse[Source] match {
        case e: Parsed.Error =>
          Engine.Error(file, e.toString).toResult

        case p: Parsed.Success[Source] =>
          process(file, p.tree)
      }
  }

  final case class Error(file: Path, msg: String) {
    def toResult: Result =
      Result(Cmds.empty, Set.empty[Error] + this)
  }

  final case class Result(cmds: Cmds, errors: Set[Error]) {
    def ++(r: Result): Result =
      Result(cmds ++ r.cmds, errors ++ r.errors)
  }

  object Result {
    val empty: Result =
      apply(Cmds.empty, Set.empty)
  }

  type ApError = Either[Set[Engine.Error], Set[FS.CmdApFailure]]
  type ApResult = Either[ApError, FS]
}
