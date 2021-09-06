ThisBuild / scalacOptions              += "-P:semanticdb:synthetics:on"
ThisBuild / scalacOptions              += "-Yrangepos"
ThisBuild / semanticdbEnabled          := true
ThisBuild / scalafixScalaBinaryVersion := "2.13"
ThisBuild / semanticdbVersion          := Dependencies.Ver.scalameta

ThisBuild / scalafixDependencies ++= Seq(
  "com.github.liancheng" %% "organize-imports" % "0.5.0",
)
