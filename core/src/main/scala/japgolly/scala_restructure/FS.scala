package japgolly.scala_restructure

final case class FS(asMap: Map[Path, String]) {

  def apply(cmd: Cmd): FS =
    cmd match {

      case Cmd.Write(file, content) =>
        FS(asMap.updated(file, content))

      case Cmd.Rename(from, to) =>
        val content = asMap(from)
        FS(asMap.updated(to, content) - from)

      case Cmd.Delete(file) =>
        FS(asMap - file)
    }

  def apply(cmds: Cmds): FS =
    cmds.asVector.foldLeft(this)(_(_))
}

object FS {
  def empty: FS =
    apply(Map.empty)

  def of(fs: (Path, String)*): FS =
    apply(fs.toMap)
}