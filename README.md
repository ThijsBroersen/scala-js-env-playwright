[![CI (main)](https://github.com/ThijsBroersen/scala-js-env-playwright/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/ThijsBroersen/scala-js-env-playwright/actions/workflows/ci.yml?query=branch%3Amain)
[![CI (latest tag)](https://github.com/ThijsBroersen/scala-js-env-playwright/actions/workflows/ci.yml/badge.svg?branch=v0.3.2)](https://github.com/ThijsBroersen/scala-js-env-playwright/actions/workflows/ci.yml?query=branch%3Av0.3.2)
[![Latest tag](https://img.shields.io/github/v/tag/ThijsBroersen/scala-js-env-playwright?sort=semver&label=latest%20tag)](https://github.com/ThijsBroersen/scala-js-env-playwright/releases/latest)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.thijsbroersen/scala-js-env-playwright_3?label=release)](https://central.sonatype.com/artifact/io.github.thijsbroersen/scala-js-env-playwright_3)
[![Maven Snapshots](https://img.shields.io/badge/Maven%20Snapshots-central--snapshots-blue)](https://central.sonatype.com/repository/maven-snapshots/io/github/thijsbroersen/scala-js-env-playwright_3/)

The **CI (latest tag)** badge tracks the newest release tag (`v0.3.2` today); bump `?branch=v…` in that badge URL when you tag a new release.

# scala-js-env-playwright
A JavaScript environment for Scala.js (a JSEnv) running playwright. It is only build for Scala 3 (supported by Mill and SBT 2.x)
## Requirements
Playwright needs certain system dependencies [read the docs](https://playwright.dev/docs/browsers#install-system-dependencies)

TLDR -> `npx playwright install-deps`

## Snapshot releases (pre-release testing)

Every **push to `main`** runs tests and, when they pass, publishes a **signed** `-SNAPSHOT` to [Maven Central Snapshots](https://central.sonatype.com/repository/maven-snapshots/) (via [sbt-ci-release](https://github.com/sbt/sbt-ci-release) + sbt-dynver). The exact version is logged in the CI job **Show release version** (format `0.3.2+<commits>-<hash>-SNAPSHOT`).

In a consuming Scala.js project, add the snapshots resolver and pin the snapshot version from CI:

```scala
// project/plugins.sbt or build.sbt (where you declare the jsEnv dependency)
ThisBuild / resolvers += Resolver.sonatypeCentralSnapshots

libraryDependencies += "io.github.thijsbroersen" %% "scala-js-env-playwright" % "0.3.2+7-xxxxxxxx-SNAPSHOT"
```

Replace the version string with the one from the latest green **Publish to Maven Central** run on `main`. Stable releases use normal version numbers (for example `0.4.0`) from git tags `v*`.

## Usage SBT
Add the following line to your `project/plugins.sbt` 
```scala
// For Scala.js 1.x
libraryDependencies += "io.github.thijsbroersen" %% "scala-js-env-playwright" % "0.3.2"
```
Add the following line to your `build.sbt` 
```scala
Test / jsEnv := new PlaywrightJSEnv.chrome(
      headless = true
    )
```
## Usage Mill (not yet part of Mill, tested in locally build Mill version)
```scala
override def jsEnvConfig = JsEnvConfig.Playwright.chrome(
  headless = true
)
```
## Avoid trouble
* This is a very early version. It may not work for all projects. It is tested on chrome/chromium and firefox.
* **WebKit** is experimental. On Linux, the Playwright driver or com-channel tests often fail or hang; Ubuntu CI therefore skips the WebKit suite. Set `PLAYWRIGHT_WEBKIT_TESTS=1` to run it locally. A **macOS** workflow (`.github/workflows/webkit-macos.yml`) runs the WebKit suite on a schedule and on relevant PRs.
* It works only with Scala.js 1.x
* Supported inputs are scripts (`ModuleKind.NoModule`) and ES modules (`ModuleKind.ESModule`).
  CommonJS modules are not supported: browsers have no `require`/`module.exports`.
* Make sure the project is set up to use ModuleKind.ESModule in the Scala.js project.
  * SBT 2.x
  ```scala
    // For Scala.js 1.x
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) }
    ```
  * Mill
  ```scala
    override def moduleKind  = ModuleKind.ESModule
  ```
* Some projects which may need to use both Selenium and Playwright. 
If it runs into google exception, add the following line to your `plugins.sbt` 
```scala
libraryDependencies += "com.google.guava" % "guava" % "33.0.0-jre"
```

## Supported browsers
* chrome
* chromium (same as chrome)
* firefox
* webkit (experimental, see "Avoid trouble" above)

## Environment variables
Environment variables passed to a run via `RunConfig.env` are exposed to the JavaScript
code as `process.env.<NAME>`, mirroring Node.js.

## Compatibility notes
### Scala versions
* This library can be used with any scala version 3.x
* This project is compiled with Scala 3.10.0
### sbt versions
* This library can be used with sbt 1.x or 2.x
### Playwright versions
* This library is built against Playwright 1.63.0 (`"com.microsoft.playwright" % "playwright" % "1.63.0"`). Other 1.x versions may work if kept aligned with your environment.
### JDK versions
* This library is tested on JDK17

## Default configuration
```scala
jsEnv := new io.github.thijsbroersen.jsenv.playwright.PlaywrightJSEnv.chrome(
  headless = true,
  showLogs = false,
)
```

## Launch options

The launch options are used to enable/disable browser features. They can be overridden via the launchOptions parameter.

When not passing launchOptions, default launch options are as follows:

### Chrome/chromium
```scala
jsEnv := new io.github.thijsbroersen.jsenv.playwright.PlaywrightJSEnv.chrome(
  launchOptions = List(
      "--disable-extensions", 
      "--disable-web-security", 
      "--allow-running-insecure-content", 
      "--disable-site-isolation-trials", 
      "--allow-file-access-from-files", 
      "--disable-gpu"
    )
)
```

### Firefox
```scala
jsEnv := new io.github.thijsbroersen.jsenv.playwright.PlaywrightJSEnv.firefox(
  firefoxUserPrefs = Map(
      "security.mixed_content.block_active_content" -> false,
      "security.mixed_content.upgrade_display_content" -> false,
      "security.file_uri.strict_origin_policy" -> false
    )
)
```

### Webkit
WebKit does not accept Chromium-style launch flags (it refuses to start when given any),
so its default launch options are empty:
```scala
jsEnv := new io.github.thijsbroersen.jsenv.playwright.PlaywrightJSEnv.webkit(
  launchOptions = Nil
)
```

## Browser lifecycle (KeepAlive)

Each `JSRun` normally launches its own Playwright driver and browser and tears them down when the run finishes (`future` completes) or when you call `close()` on the run.

To **reuse the same browser process** across multiple runs on one env instance (faster sbt `Test` sessions), enable reuse on the env config and shut down explicitly when finished:

```scala
lazy val env = PlaywrightJSEnv(
  PlaywrightJSEnv.ChromeOptions(),
  PlaywrightJSEnv.Config().withReuseBrowserBetweenRuns(true)
)

Test / jsEnv := env

// after your test session (e.g. in test cleanup):
env.shutdownSharedBrowser()
```

Notes:

- Reuse keeps the browser **process** alive; each run still gets a **fresh browser context** for isolation.
- Do **not** combine reuse with `Test / parallelExecution := true` on the same env instance (runs may overlap).
- `RunConfig` has no keep-alive flag; lifecycle is controlled by the run `future`, `JSRun.close()`, and optionally reuse + `shutdownSharedBrowser()`.

## Debugging
debug parameter can be passed to the PlaywrightJSEnv constructor to enable debugging. It will also display the version of the browser which is used.
```scala
Test / jsEnv := new PlaywrightJSEnv.chrome(
      headless = true,
      showLogs = true,
      debug = true
    )
```
