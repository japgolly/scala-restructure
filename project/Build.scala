import sbt._
import sbt.Keys._
import sbtassembly.AssemblyPlugin.autoImport._
import sbtbuildinfo.BuildInfoPlugin
import sbtbuildinfo.BuildInfoPlugin.autoImport._
import Dependencies._
import Lib._

object Build {

  lazy val root = project
    .in(file("."))
    .configure(defaultSettings(tests = false))
    .aggregate(
      core,
      cli,
    )

  lazy val core = project
    .configure(defaultSettings(tests = true))
    .settings(
      libraryDependencies ++= Seq(
        Dep.microlibsStdlibExt.value,
        Dep.osLib.value,
        Dep.scalameta.value,
      ),
    )

  private val cliName = "scala_restructure"

  lazy val cli = project
    .enablePlugins(BuildInfoPlugin)
    .configure(defaultSettings(tests = false))
    .dependsOn(core)
    .settings(
      buildInfoKeys := Seq[BuildInfoKey](
        "name" -> cliName,
        "displayName" -> cliName,
        version,
      ),
      buildInfoPackage := "japgolly.scala_restructure.cli",
      libraryDependencies ++= Seq(
        Dep.scopt.value,
      ),
      assembly / logLevel := Level.Info,
      assembly / assemblyJarName := s"$cliName.jar",
    )

}
