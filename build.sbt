val projectName         = "scalacraft"
val projectVersion      = "0.1-SNAPSHOT"
val projectOrganization = "io.scalacraft"
val projectScalaVersion = "2.12.8"

val scalaTestVersion    = "3.0.8"
val akkaVersion         = "2.5.24"
val circeVersion        = "0.11.1"
val scalaLoggingVersion = "3.9.2"
val nettyVersion        = "4.1.38.Final"
val logbackVersion      = "1.2.3"
val nbtVersion          = "4.1.2"

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
  "org.scala-lang"             %  "scala-reflect"   % projectScalaVersion,
  "org.scalactic"              %% "scalactic"       % scalaTestVersion,
  "org.scalatest"              %% "scalatest"       % scalaTestVersion     % "test",
  "com.typesafe.scala-logging" %% "scala-logging"   % scalaLoggingVersion,
  "it.eciavatta"               %  "NBT"             % nbtVersion,
  "io.netty"                   %  "netty-all"       % nettyVersion,
  "com.typesafe.akka"          %% "akka-actor"      % akkaVersion,
  "com.typesafe.akka"          %% "akka-slf4j"      % akkaVersion,
  "ch.qos.logback"             %  "logback-classic" % logbackVersion,
  "io.circe"                   %% "circe-core"      % circeVersion,
  "io.circe"                   %% "circe-generic"   % circeVersion,
  "io.circe"                   %% "circe-parser"    % circeVersion
)
