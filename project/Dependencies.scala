import sbt._

object Dependencies {

  object Ver {

    // Externally observable
    def scalameta = "4.4.27"
    def scala     = "2.13.6"

    // Internal
    def microlibs = "3.0.1"
    def utest     = "0.7.10"
  }

  object Dep {
    val scalameta           = Def.setting("org.scalameta"                 %% "scalameta"   % Ver.scalameta)
    val microlibsAdtMacros  = Def.setting("com.github.japgolly.microlibs" %% "adt-macros"  % Ver.microlibs)
    val microlibsMacroUtils = Def.setting("com.github.japgolly.microlibs" %% "macro-utils" % Ver.microlibs)
    val microlibsNonempty   = Def.setting("com.github.japgolly.microlibs" %% "nonempty"    % Ver.microlibs)
    val microlibsRecursion  = Def.setting("com.github.japgolly.microlibs" %% "recursion"   % Ver.microlibs)
    val microlibsScalazExt  = Def.setting("com.github.japgolly.microlibs" %% "scalaz-ext"  % Ver.microlibs)
    val microlibsStdlibExt  = Def.setting("com.github.japgolly.microlibs" %% "stdlib-ext"  % Ver.microlibs)
    val microlibsTestUtil   = Def.setting("com.github.japgolly.microlibs" %% "test-util"   % Ver.microlibs)
    val microlibsUtils      = Def.setting("com.github.japgolly.microlibs" %% "utils"       % Ver.microlibs)
    val utest               = Def.setting("com.lihaoyi"                   %% "utest"       % Ver.utest)
  }
}
