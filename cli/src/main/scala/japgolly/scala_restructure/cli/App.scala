package japgolly.scala_restructure.cli

import japgolly.scala_restructure._
import java.nio.file.{Path => JPath, _}
import scala.collection.immutable.ArraySeq

object App {
  sealed abstract class ExitCode(final val value: Int, final val fatal: Boolean)
  object ExitCode {
    case object NoDirectories  extends ExitCode(2, true)
    case object FailedToDelete extends ExitCode(3, false)
    case object FileExists     extends ExitCode(4, false)
    case object FileNotFound   extends ExitCode(5, false)
    case object IoError        extends ExitCode(6, false)
  }
}

final class App(opts: Options) {
  import App.ExitCode
  import App.ExitCode._
  import Cmd._
  import Console._
  import Path.Pwd

  private def debug(msg: => Any): Unit =
    if (opts.verbose)
      println(msg)

  private def log(msg: => Any): Unit =
    println(msg)

  private def error(exitCode: ExitCode)(msg: Any): Unit = {
    System.err.println(msg)
    if (exitCode.fatal || !opts.ignoreErrors)
      System.exit(exitCode.value)
  }

  private def catchErrors[A](exitCode: ExitCode)(a: => A): Option[A] =
    try
      Some(a)
    catch {
      case t: Throwable =>
        error(exitCode)(t.getMessage)
        None
    }

  def run(): Unit = {
    val dirs = collectDirs()
    if (dirs.isEmpty)
      error(NoDirectories)("No directories found.")

    // TODO:
    val engine: Engine =
      AlignFileToTypes & AlignDirectoriesToPackages

    for (d <- dirs)
      runOnTree(engine, d)
  }

  /** Find all relevant Scala source directories. */
  private def collectDirs(): ArraySeq[os.Path] = {
    val dirMatchesGlobs: JPath => Boolean = {
      val FS = FileSystems.getDefault()
      val matchers = opts.dirGlobs.map { g =>
        try
          FS.getPathMatcher("glob:" + g)
        catch {
          case e: Throwable =>
            throw new IllegalArgumentException("Invalid glob: " + g, e) // TODO: Handle nicely
        }
      }
      p => matchers.exists(_.matches(p))
    }

    val dirs =
      os.walk.attrs(
          path = os.pwd,
          skip = (_, s) => !s.isDir,
          followLinks = true,
          includeTarget = false,
        )
        .iterator
        .map { case (p, _) => p }
        .filterNot(_.toString contains "/target/scala-")
        .filter(p => dirMatchesGlobs(p.toNIO))
        .toArray
        .sortInPlace()

    ArraySeq.unsafeWrapArray(dirs.array)
  }

  private def loadAllScalaFilesFromDisk(root: os.Path): FS = {
    val fsAsMap =
      os.walk.attrs(root, followLinks = true, includeTarget = false)
        .iterator
        .filter(_._2.isFile)
        .filter(_._1.ext == "scala")
        .map { case (p, _) => p }
        .foldLeft(Map.empty[Path, String]) { (fs, p) =>
          val d = p.relativeTo(root)
          val f = Path(d.segments.dropRight(1).mkString("/"), d.last)
          val s = os.read(p)
          fs.updated(f, s)
        }

    FS(fsAsMap)
  }

  private def runOnTree(engine: Engine, root: os.Path): Unit = {

    // Collect files
    debug(s"Scanning $root ...")
    val fs = loadAllScalaFilesFromDisk(root)
    debug(s"  Found ${fs.size} files")
    if (fs.isEmpty)
      return

    // Perform fixes in memory
    val engineResult = engine.scanFS(fs)
    val commonRoot: String =
      engine(engineResult, fs) match {
        case Right(fs2) =>
          debug("  Opperations applied successfully in-memory")
          (fs ++ fs2).commonRoot

        case Left(e) =>
          // TODO: HANDLE ERRORS!
          println("  FAILED: " + e)
          return
      }

    // Go through the commands
    implicit val pwd = Path.Pwd(root)
    val cmds = engineResult.cmds.asVector
    debug(s"  Generated ${cmds.length} changes to apply")
    val count = cmds.length
    for (i <- cmds.indices) {
      val cmd = cmds(i)
      def desc = formatCmd(commonRoot, cmd)
      debug(s"    - [${i + 1}/$count] $desc")

      // Perform real changes
      if (!opts.dryRun)
        reallyInterpretCmd(cmd)
    }

    // Done
    val affectedFiles = engineResult.cmds.fileIterator().toSet.size
    if (affectedFiles > 0)
      log(s"Modified $affectedFiles files in $root")
  }

  private def formatCmd(commonRoot: String, cmd: Cmd): String = {
    val colourFile = YELLOW

    def file(p: Path): String =
      if (commonRoot.isEmpty)
        colourFile + p.fullPath + RESET
      else if (p.dir == commonRoot.dropRight(1))
        commonRoot + colourFile + p.file + RESET
      else
        commonRoot + colourFile + p.fullPath.drop(commonRoot.length) + RESET

    cmd match {
      case Create(f, _) => s"${GREEN}Create$RESET ${file(f)}"
      case Update(f, _) => s"${CYAN}Update$RESET ${file(f)}"
      case Delete(f)    => s"${RED}Delete$RESET ${file(f)}"

      case Rename(from, to) =>
        val desc: String =
          from.unify(to) match {
            case None    => s"${file(from)} => ${file(to)}"
            case Some(u) => u.desc(s"$RESET{$colourFile", s"$RESET => $colourFile", s"$RESET}")
          }
        s"${BLUE}Rename$RESET $desc"
    }
  }

  private def reallyInterpretCmd(cmd: Cmd)(implicit pwd: Pwd): Unit =
    cmd match {

      case Create(file, content) =>
        if (file.exists())
          error(FileExists)(s"File already exists: ${file.fullPath}")
        else
          catchErrors(IoError) {
            os.write(file.toOsPath, content, createFolders = true)
          }

      case Update(file, content) =>
        if (file.exists())
          catchErrors(IoError) {
            os.write.over(file.toOsPath, content, createFolders = true)
          }
        else
          error(FileNotFound)(s"File not found: ${file.fullPath}")

      case Rename(from, to) =>
        if (!from.exists())
          error(FileNotFound)(s"File not found: ${from.fullPath}")
        else if (to.exists())
          error(FileExists)(s"File already exists: ${to.fullPath}")
        else
          catchErrors(IoError) {
            os.move(from.toOsPath, to.toOsPath, createFolders = true)
          }

      case Delete(f) =>
        val deleted = catchErrors(FailedToDelete)(f.toFile().delete())
        if (deleted.contains(false))
          error(FailedToDelete)(s"Failed to delete: ${f.fullPath}")
    }
}
