import Dependencies.*

ThisBuild / scalaVersion := "3.9.0"
ThisBuild / organization := "com.atacamasoft.rsp"

lazy val root = (project in file("."))
  .settings(
    name := "rsp-backoffice-server-zio",
    libraryDependencies ++= Dependencies.all,
    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked"),
    Compile / run / fork := true,
    Compile / mainClass  := Some("com.atacamasoft.rsp.backoffice.server.Main")
  )
