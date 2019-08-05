// Deploy fat JARs. Restart processes.
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.9")

// Builds and pushes Docker images of the project.
// addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.5.0")

// Integrates the scoverage code coverage library
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")
