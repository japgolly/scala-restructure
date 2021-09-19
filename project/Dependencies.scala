import sbt._

object Dependencies {

  object Ver {

    // Externally observable
    def scala     = "2.13.6"
    def scalameta = "4.4.28"
    def scopt     = "4.0.1"
    def osLib     = "0.7.8"

    // Internal
    def microlibs = "3.0.1"
    def utest     = "0.7.10"
  }

  object Dep {
    val microlibsStdlibExt  = Def.setting("com.github.japgolly.microlibs" %% "stdlib-ext"  % Ver.microlibs)
    val microlibsTestUtil   = Def.setting("com.github.japgolly.microlibs" %% "test-util"   % Ver.microlibs)
    val osLib               = Def.setting("com.lihaoyi"                   %% "os-lib"      % Ver.osLib)
    val scalameta           = Def.setting("org.scalameta"                 %% "scalameta"   % Ver.scalameta)
    val scopt               = Def.setting("com.github.scopt"              %% "scopt"       % Ver.scopt)
    val utest               = Def.setting("com.lihaoyi"                   %% "utest"       % Ver.utest)
  }
}
