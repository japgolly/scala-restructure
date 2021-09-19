package japgolly.scala_restructure

import java.io.File

final case class Path(dir: String, file: String) {
  import Path._

  def fullPath: String =
    if (dir.isEmpty)
      file
    else
      s"$dir/$file"

  def toFile()(implicit pwd: Pwd): File =
    toOsPath.wrapped.toFile()

  def toOsPath(implicit pwd: Pwd): os.Path =
    pwd / this

  def exists()(implicit pwd: Pwd): Boolean =
    toFile().exists()

  def unify(that: Path): Option[Unified] = {
    if (this == that)
      return None

    def split(p: Path): Array[String] = segments.split(p.fullPath)
    var s1 = split(this)
    var s2 = split(that)

    // Collect prefixes
    var prefix = ""
    while (s1.nonEmpty && s2.nonEmpty && s1.head == s2.head) {
      prefix += s1.head
      s1 = s1.tail
      s2 = s2.tail
    }

    // Collect suffixes
    var suffix = ""
    while (s1.nonEmpty && s2.nonEmpty && s1.last == s2.last) {
      suffix = s1.last + suffix
      s1 = s1.init
      s2 = s2.init
    }

    def moveIntoPrefix(tok: String): Unit =
      if (s1.nonEmpty && s2.nonEmpty && s1.head.startsWith(tok) && s2.head.startsWith(tok)) {
        s1(0) = s1(0).drop(tok.length)
        s2(0) = s2(0).drop(tok.length)
        prefix += tok
      }

    Option.when(prefix.nonEmpty || suffix.nonEmpty) {
      moveIntoPrefix("/")
      moveIntoPrefix(".")

      Unified(
        prefix = Option.when(prefix.nonEmpty)(prefix),
        mid1   = s1.mkString,
        mid2   = s2.mkString,
        suffix = Option.when(suffix.nonEmpty)(suffix),
      )
    }
  }
}

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

  private[Path] val headDir = "^([^/]+?)/(.+)$".r
  private[Path] val tailDir = "^([^/]+)/(.+)$".r
  private[Path] val segments = "(?=[/.])".r

  final case class Unified(prefix: Option[String], mid1: String, mid2: String, suffix: Option[String]) {
    assert(prefix.isDefined || suffix.isDefined)

    def desc(mid: (String, String) => String): String =
      prefix.getOrElse("") + mid(mid1, mid2) + suffix.getOrElse("")

    def desc(pre: String, mid: String, post: String): String =
      desc(pre + _ + mid + _ + post)

    def desc: String =
      desc("{", " => ", "}")
  }

  final case class Pwd(path: os.Path) {
    def /(p: Path): os.Path =
      path / os.RelPath(p.fullPath)
  }
}