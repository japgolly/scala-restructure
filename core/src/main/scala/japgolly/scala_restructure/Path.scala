package japgolly.scala_restructure

final case class Path(dir: String, file: String)

object Path {

  private val dirAndFile = "^(.*)/([^/]+)$".r

  def apply(path: String): Path = {
    var p = path.replace('\\', '/').replace("/./", "/")
    while (p.startsWith("./"))
      p = p.drop(2)
    p match {
      case dirAndFile(dir, file) => Path(dir, file)
      case _                     => Path("", p.stripPrefix("/"))
    }
  }
}
