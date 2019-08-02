val projectName = "scalacraft"
val projectVersion = "0.1-SNAPSHOT"
val projectOrganization = "io.scalacraft"
val projectScalaVersion = "2.12.8"
val scalaTestVersion = "3.0.8"

lazy val root = Project(
  id = projectName,
  base = file(".")
) .enablePlugins(AssemblyPlugin)
  .settings(
    assemblyJarName in assembly := s"${name.value}-${version.value}.jar",
    libraryDependencies ++= dependencies,
    mainClass in assembly := Some("io.scalacraft.EntryPoint"),
    name := projectName,
    scalaVersion := projectScalaVersion,
    version := projectVersion,
    organization := projectOrganization,
    coverageEnabled := true
  )

lazy val dependencies = Seq(
  "org.scala-lang" % "scala-reflect" % projectScalaVersion,
  "org.scalactic" %% "scalactic" % scalaTestVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
)
