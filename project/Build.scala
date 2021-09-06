import sbt._
import sbt.Keys._
import Dependencies._
import Lib._

object Build {

  lazy val root = project
    .in(file("."))
    .configure(defaultSettings(tests = false))
    .aggregate(
      core,
    )

  lazy val core = project
    .configure(defaultSettings(tests = true))
    .settings(
      libraryDependencies ++= Seq(
        Dep.scalameta.value,
      ),
    )

}
