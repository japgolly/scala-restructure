package japgolly.scala_restructure.cli

import japgolly.scala_restructure.{Engine, EngineDef}
import scopt._

final case class Options(
    dirGlobs    : Seq[String]    = Seq.empty,
    dryRun      : Boolean        = false,
    engineDefs  : Set[EngineDef] = Set.empty,
    ignoreErrors: Boolean        = false,
    verbose     : Boolean        = false,
) {

  def engine(): Engine =
    engineDefs.iterator.map(_.engine).reduce(_ & _)

  def withDefaults: Options = {
    import Options.Default
    var o = this

    if (dirGlobs.isEmpty) o = o.copy(dirGlobs = Default.dirGlobs)
    if (engineDefs.isEmpty) o = o.copy(engineDefs = Default.engineDefs)

    o
  }
}

object Options {

  object Default {

    def dirGlobs = Seq[String](
      "**/src/*/scala*",
    ).sorted

    def engineDefs: Set[EngineDef] =
      EngineDef.enabled
  }

  def fromArgs(args: Array[String]): Option[Options] =
    Parser.parse(args, Options())

  object Parser extends OptionParser[Options](BuildInfo.name) {
    head(BuildInfo.displayName, "v" + BuildInfo.version)
    head()

    arg[String]("<dir | glob of dirs>...")
      .unbounded()
      .optional()
      .action((i, o) => o.copy(dirGlobs = o.dirGlobs :+ i))
      .text(s"Source directory roots. Default: ${Default.dirGlobs.mkString(", ")}")

    for (e <- EngineDef.enabled) {
      def add(short: Char, full: String, desc: String): Unit =
        opt[Unit](short, full).action_(o => o.copy(engineDefs = o.engineDefs + e)).text(desc + " (on by default)")

      import EngineDef._
      e match {
        case AlignDirectoriesToPackages => add('d', "align-dirs", "Move files into directories that match their packages (i.e. Java-style)")
        case AlignFileToTypes           => add('f', "align-files", "Split and move files so that their filename matches the top-level type (i.e. Java-style)")
      }
    }

    opt[Unit]('i', "ignore-errors").action_(_.copy(ignoreErrors = true)).text("Ignore (ignorable) errors")
    opt[Unit]('n', "dry-run"      ).action_(_.copy(dryRun       = true)).text("Don't actually modify the file system")
    opt[Unit]('v', "verbose"      ).action_(_.copy(verbose      = true)).text("Print more information. Useful for debugging and seeing what's going on under the hood.")

    help('h', "help").text("Prints this usage text")
  }

  private implicit class ExtScoptOptionDef[A, C](private val self: OptionDef[A, C]) extends AnyVal {
      def action_(f: C => C) = self.action((_, c) => f(c))
  }
}
