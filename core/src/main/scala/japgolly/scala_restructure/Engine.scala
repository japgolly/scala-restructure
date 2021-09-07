package japgolly.scala_restructure

import scala.meta._
import scala.meta.parsers.Parsed

trait Engine { self =>

  def singleFile(file: Path, content: String): Engine.Result

  final def apply(fs: FS): Engine.Result =
    fs.asMap.foldLeft(Engine.Result.empty) { case (r, (path, content)) =>
      r ++ singleFile(path, content)
    }

  final def &(next: Engine): Engine =
    new Engine {
      override def singleFile(file: Path, content: String): Engine.Result =
        self.singleFile(file, content) ++ next.singleFile(file, content)
    }
}

object Engine {

  trait Simple extends Engine {
    protected def process(file: Path, src: Source): Engine.Result

    override final def singleFile(file: Path, content: String): Engine.Result =
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
}
