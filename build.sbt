name      := "scala-restructure"
startYear := Some(2021)

ThisBuild / organization := "com.github.japgolly.scala-restructure"
ThisBuild / homepage     := Some(url("https://github.com/japgolly/scala-restructure"))
ThisBuild / licenses     := ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0")) :: Nil
ThisBuild / shellPrompt  := ((s: State) => Project.extract(s).currentRef.project + "> ")

val root = Build.root
val core = Build.core
val cli  = Build.cli
