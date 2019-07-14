name := "scala-statecharts"
organization := "com.github.reugn"

scalaVersion := "2.12.8"
crossScalaVersions := Seq(scalaVersion.value, "2.13.0")

libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.8" % Test
)

scalacOptions := Seq(
    "-target:jvm-1.8",
    "-unchecked",
    "-deprecation",
    "-feature",
    "-encoding", "utf8",
)

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))