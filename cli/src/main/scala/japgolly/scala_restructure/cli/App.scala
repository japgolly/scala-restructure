package japgolly.scala_restructure.cli

import japgolly.scala_restructure._

object App {
  sealed abstract class ExitCode(final val value: Int, final val fatal: Boolean)
  object ExitCode {
    case object NoDirectories  extends ExitCode(2, false)
    case object FailedToDelete extends ExitCode(3, false)
    case object FileExists     extends ExitCode(4, false)
    case object FileNotFound   extends ExitCode(5, false)
    case object IoError        extends ExitCode(6, false)
    case object FailedToParse  extends ExitCode(7, false)
    case object FailedToApply  extends ExitCode(8, false)
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
    val engine: Engine =
      opts.engine()

    for (glob <- opts.dirGlobs) {
      val dirs = DirResolver(glob)
      if (dirs.isEmpty)
        error(NoDirectories)("No directories found.")

      for (dir <- dirs)
        runOnTree(engine, dir)
    }
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

    def badPath(p: Path) =
      s"$RED$root/${p.fullPath}$RESET"

    // Collect files
    debug(s"Scanning $root ...")
    val fs = loadAllScalaFilesFromDisk(root)
    debug(s"  Found ${fs.size} files")
    if (fs.isEmpty)
      return

    // Scan and process files in memory
    val engineResult = engine.scanFS(fs)
    if (engineResult.errors.nonEmpty) {
      val errMsgs =
        engineResult.errors
          .iterator
          .map(e => s"Failed to parse ${badPath(e.file)}: ${e.msg}")
          .toArray
          .sortInPlace()
      error(FailedToParse)(errMsgs.mkString("\n"))
    }

    // Perform fixes in memory first
    val fs2: FS =
      fs(engineResult.cmds) match {
        case Right(fs2) =>
          debug("  Operations applied successfully in-memory")
          fs2

        case Left(e) =>
          import FS.CmdApFailure._
          def cantRename(c: Cmd.Rename) = {
            import c._
            val desc: String =
              from.unify(to) match {
                case None    => s"${badPath(from)} => ${badPath(to)}"
                case Some(u) => root.toString + "/" + u.desc(s"$RESET{$RED", s"$RESET => $RED", s"$RESET}")
              }
            s"Can't rename $desc"
          }
          val errMsgs =
            e.iterator
              .map {
                case CreateFailedFileExists    (f, _) => s"Can't create ${badPath(f)} - already exists"
                case DeleteFailedFileNotFound  (f)    => s"Can't delete ${badPath(f)} - file not found"
                case RenameFailedSourceNotFound(_, c) => s"${cantRename(c)} - source not found"
                case RenameFailedTargetExists  (_, c) => s"${cantRename(c)} - target already exists"
                case UpdateFailedFileNotFound  (f, _) => s"Can't update ${badPath(f)} - file not found"
              }
              .toArray
              .sortInPlace()
              .mapInPlace("Failed to apply fix. " + _)
          error(FailedToApply)(errMsgs.mkString("\n"))
          return
      }

    // Go through the commands
    implicit val pwd = Path.Pwd(root)
    val cmds = engineResult.cmds.asVector
    val commonRoot = (fs ++ fs2).commonRoot
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
