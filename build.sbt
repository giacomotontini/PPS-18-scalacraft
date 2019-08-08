val projectName = "scalacraft"
val projectVersion = "0.1-SNAPSHOT"
val projectOrganization = "io.scalacraft"
val projectScalaVersion = "2.12.8"
val scalaTestVersion = "3.0.8"
val akkaVersion = "2.5.23"

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
    coverageEnabled := true,
    parallelExecution := false,
      resolvers += "jitpack" at "https://jitpack.io"
  )

lazy val dependencies = Seq(
  "org.scala-lang" % "scala-reflect" % projectScalaVersion,
  "org.scalactic" %% "scalactic" % scalaTestVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
  "com.github.Querz" % "NBT" % "4.1",
  "io.netty" % "netty-all" % "4.1.38.Final",
  "org.apache.logging.log4j" %% "log4j-api-scala" % "11.0",
  "org.apache.logging.log4j" % "log4j-api" % "2.12.0",
  "org.apache.logging.log4j" % "log4j-core" % "2.12.0",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion
)
