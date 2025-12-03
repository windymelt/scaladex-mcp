lazy val root = (project in file(".")).settings(
  name := "scaladex-mcp",
  version := "0.0.1",
  scalaVersion := "3.7.4",
  libraryDependencies ++= Seq(
    "com.indoorvivants" %% "mcp-quick" % "0.1.3",
    "com.softwaremill.sttp.client4" %% "core" % "4.0.13",
  ),
)

inThisBuild(
  List(
    organization := "dev.capslock",
    homepage := Some(url("https://github.com/windymelt/scaladex-mcp")),
    licenses := List(
      "GPL-3.0-or-later" -> url(
        "https://spdx.org/licenses/GPL-3.0-or-later.html",
      ),
    ),
    developers := List(
      Developer(
        "windymelt",
        "Windymelt",
        "windymelt@capslock.dev",
        url("https://www.3qe.us"),
      ),
    ),
    versionScheme := Some("early-semver"),
  ),
)
