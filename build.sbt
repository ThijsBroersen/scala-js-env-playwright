import sbtrelease.ReleasePlugin.autoImport.*
import sbtrelease.ReleaseStateTransformations.*

scalacOptions ++= Seq("-java-output-version", "21", "-Wunused:all")

organization := "io.github.thijsbroersen"
organizationName := "ThijsBroersen"
scalaVersion := "3.8.4"
versionScheme := Some("early-semver")
licenses := Seq(
  "BSD-3-Clause" -> url("https://opensource.org/licenses/BSD-3-Clause")
)
scmInfo := Some(
  ScmInfo(
    url("https://github.com/thijsbroersen/scala-js-env-playwright"),
    "scm:git@github.com:thijsbroersen/scala-js-env-playwright.git"
  )
)
developers := List(
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

semanticdbEnabled := true

publishMavenStyle := true
pomIncludeRepository := { _ => false }
publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (version.value.endsWith("-SNAPSHOT"))
    Some("central-snapshots" at centralSnapshots)
  else
    localStaging.value
}

lazy val root = (project in file(".")).settings(
  name := "scala-js-env-playwright",
  libraryDependencies ++= Seq(
    "com.microsoft.playwright" % "playwright" % "1.61.0",
    ("org.scala-js" %% "scalajs-js-envs" % "1.6.0").cross(CrossVersion.for3Use2_13),
    ("org.scala-js" %% "scalajs-js-envs-test-kit" % "1.6.0" % Test).cross(CrossVersion.for3Use2_13),
    "com.google.jimfs" % "jimfs" % "1.3.1",
    "com.outr" %% "scribe" % "3.19.0",
    "org.typelevel" %% "cats-effect" % "3.7.0",
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
    releaseStepCommand("publishSigned"),
    releaseStepCommand("sonaRelease"),
    setNextVersion,
    commitNextVersion
  ),
  Test / parallelExecution := true,
  Test / publishArtifact := false,
  // WebKit's driver process hangs indefinitely on some Linux setups (observed on
  // Ubuntu); only run the WebKit suite when explicitly requested, e.g. in a CI job
  // where the environment is known to work.
  Test / testOptions += Tests.Filter { name =>
    !name.contains("SuiteWebKit") || sys.env.getOrElse("PLAYWRIGHT_WEBKIT_TESTS", "0") == "1"
  }
)
