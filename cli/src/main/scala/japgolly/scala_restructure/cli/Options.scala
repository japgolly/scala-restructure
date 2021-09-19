package japgolly.scala_restructure.cli

import scopt._

final case class Options(
    dirGlobs    : Seq[String] = Seq.empty,
    dryRun      : Boolean     = false,
    ignoreErrors: Boolean     = false,
    verbose     : Boolean     = false,
) {
  def withDefaults: Options = {
    import Options.Default
    var o = this
    if (dirGlobs.isEmpty) o = o.copy(dirGlobs = Default.dirGlobs)
    o
  }
}

object Options {

  private[Options] object Default {
    def dirGlobs = Seq[String](
      "**/src/*/scala*",
    ).sorted
  }

  def fromArgs(args: Array[String]): Option[Options] =
    Parser.parse(args, Options())

  object Parser extends OptionParser[Options](BuildInfo.name) {
    head(BuildInfo.displayName, "v" + BuildInfo.version)
    head()

    help('h', "help").text("Prints this usage text")

    opt[Unit]('i', "ignore-errors").action_(_.copy(ignoreErrors = true)).text("Ignore (ignorable) errors")
    opt[Unit]('n', "dry-run"      ).action_(_.copy(dryRun       = true)).text("Don't actually modify the file system")
    opt[Unit]('v', "verbose"      ).action_(_.copy(verbose      = true)).text("Print more information. Useful for debugging and seeing what's going on under the hood.")

    arg[String]("<dir>...")
      .unbounded()
      .optional()
      .action((i, o) => o.copy(dirGlobs = o.dirGlobs :+ i))
      .text(s"Source directory roots. Default: ${Default.dirGlobs.mkString(", ")}")

    // checkConfig { o =>
    //   import o._

    //   val result =
    //     if (List(parseDump, parseTrace, parseState, parseValue).count(identity) > 1)
    //       failure("Only one parse method can be specified.")
    //     else if (toDiffTrace & toFullTrace)
    //       failure("Did you want a diff trace or a full trace?")
    //     else
    //       success

    //   result.left.map(_ + "\n")
    // }
  }

  private implicit class ExtScoptOptionDef[A, C](private val self: OptionDef[A, C]) extends AnyVal {
      def action_(f: C => C) = self.action((_, c) => f(c))
  }
}
