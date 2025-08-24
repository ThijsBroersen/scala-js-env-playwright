import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations.*
import sbtrelease.ReleaseStateTransformations.{
  checkSnapshotDependencies,
  inquireVersions,
  runClean
}
import xerial.sbt.Sonatype.*

ThisBuild / sonatypeCredentialHost := sonatypeCentralHost
ThisBuild / organization := "io.github.thijsbroersen"
ThisBuild / organizationName := "ThijsBroersen"
ThisBuild / scalaVersion := "3.7.2"
ThisBuild / sonatypeProfileName := "io.github.thijsbroersen"
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / licenses := Seq(
  "BSD-3-Clause" -> url("https://opensource.org/licenses/BSD-3-Clause")
)
ThisBuild / sonatypeProjectHosting := Some(
  GitHubHosting("thijsbroersen", "scala-js-env-playwright", "thijsbroersen@gmail.com")
)
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/thijsbroersen/scala-js-env-playwright"),
    "scm:git@github.com:thijsbroersen/scala-js-env-playwright.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "gmkumar2005",
    name = "Kiran Kumar",
    email = "info@akkagrpc.com",
    url = url("https://www.akkagrpc.com")
  ),
  Developer(
    id = "thijsbroersen",
    name = "Thijs Broersen",
    email = "thijsbroersen@gmail.com",
    url = url("https://thijsbroersen.nl")
  )
)
ThisBuild / tlCiHeaderCheck := false
ThisBuild / tlCiMimaBinaryIssueCheck := false
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("17"))

lazy val root = (project in file(".")).settings(
  name := "scala-js-env-playwright",
  libraryDependencies ++= Seq(
    "com.microsoft.playwright" % "playwright" % "1.54.0",
    ("org.scala-js" %% "scalajs-js-envs" % "1.4.0").cross(CrossVersion.for3Use2_13),
    ("org.scala-js" %% "scalajs-js-envs-test-kit" % "1.4.0" % Test)
      .cross(CrossVersion.for3Use2_13),
    "com.google.jimfs" % "jimfs" % "1.3.1",
    "com.outr" %% "scribe" % "3.17.0",
    "org.typelevel" %% "cats-effect" % "3.6.3",
    "com.novocode" % "junit-interface" % "0.11" % Test
  ),
  javacOptions += "-nowarn",
  javacOptions -= "-Werror",
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    ReleaseStep(action = st => Command.process("publishSigned", st, _ => ())),
    setNextVersion,
    commitNextVersion
  ),
  publishMavenStyle := true,
  Test / parallelExecution := true,
  Test / publishArtifact := false
)
