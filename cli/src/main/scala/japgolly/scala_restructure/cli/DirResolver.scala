package japgolly.scala_restructure.cli

import java.nio.file.{Path => JPath, _}
import scala.collection.immutable.ArraySeq

object DirResolver {

  def apply(_dirOrGlob: String): ArraySeq[os.Path] = {
    val dirOrGlob = _dirOrGlob.replace('\\', '/')

    val parts: Vector[String] =
      if (dirOrGlob.startsWith("/"))
        // Absolute path provided
        dirOrGlob.split("/+").toVector.tail // .tail because head is ""
      else
        // Relative path provided
        os.RelPath(dirOrGlob).resolveFrom(os.pwd).segments.toVector

    parts.indexWhere(isGlob) match {
      case -1 =>
        // No glob specified
        val root = partsToAbsPath(parts)
        val paths =
          Options.Default.dirGlobs
            .iterator
            .flatMap(glob(root, _))
            .toArray
            .sortInPlaceBy(_.toString)
            .array
        if (paths.nonEmpty)
          ArraySeq.unsafeWrapArray(paths)
        else
          ArraySeq(root)

      case i =>
        // It's a glob! Watch out!
        val root = partsToAbsPath(parts.take(i - 1))
        val glob = parts.drop(i).mkString("/")
        this.glob(root, glob)
    }
  }

  private val isGlobChar: Char => Boolean = {
    case '*' | '?' => true
    case _         => false
  }

  private val isGlob: String => Boolean =
    _.exists(isGlobChar)

  private def partsToAbsPath(ps: Vector[String]): os.Path =
    os.Path(ps.mkString("/", "/", ""))

  private lazy val FS = FileSystems.getDefault()

  def glob(root: os.Path, glob: String): ArraySeq[os.Path] = {
    val matcher: JPath => Boolean =
      try {
        val m = FS.getPathMatcher(s"glob:$glob")
        m.matches
      } catch {
        case e: Throwable =>
          throw new IllegalArgumentException("Invalid glob: " + glob, e) // TODO: Handle nicely
      }

    val dirs =
      os.walk.attrs(
          path = root,
          skip = (_, s) => !s.isDir,
          followLinks = true,
          includeTarget = false,
        )
        .iterator
        .map { case (p, _) => p }
        .filterNot(p => blacklist(p.toString))
        .filter(_.toIO.isDirectory())
        .filter(p => matcher(p.toNIO))
        .toArray
        .sortInPlace()

    ArraySeq.unsafeWrapArray(dirs.array)
  }

  private val blacklistFragments = List[String](
    "/.bloop/",
    "/.bsp/",
    "/.idea/",
    "/.metals/",
    "/.vscode/",
    "/target/scala-",
  )

  private def blacklist(dir: String): Boolean =
    blacklistFragments.exists(dir.contains)
}
