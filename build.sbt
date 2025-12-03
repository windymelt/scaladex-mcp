lazy val root = (project in file(".")).settings(
  name := "scaladex-mcp",
  version := "0.0.1",
  scalaVersion := "3.7.0",
  libraryDependencies ++= Seq(
    "com.indoorvivants" %% "mcp-quick" % "0.1.3",
    "com.softwaremill.sttp.client4" %% "core" % "4.0.13",
  ),
)
