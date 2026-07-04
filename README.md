[![Scala CI](https://github.com/thijsbroersen/scala-js-env-playwright/actions/workflows/ci.yml/badge.svg)](https://github.com/thijsbroersen/scala-js-env-playwright/actions/workflows/ci.yml)
# scala-js-env-playwright
A JavaScript environment for Scala.js (a JSEnv) running playwright. It is only build for Scala 3 (supported by Mill and SBT 2.x)
## Requirements
Playwright needs certain system dependencies [read the docs](https://playwright.dev/docs/browsers#install-system-dependencies)

TLDR -> `npx playwright install-deps`

## Usage SBT
Add the following line to your `project/plugins.sbt` 
```scala
// For Scala.js 1.x
libraryDependencies += "io.github.thijsbroersen" %% "scala-js-env-playwright" % "0.2.2"
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
* WebKit support exists but its driver process has been observed to hang indefinitely
  on some Linux setups (e.g. Ubuntu). The WebKit test suite is skipped by default; set
  `PLAYWRIGHT_WEBKIT_TESTS=1` to run it in an environment known to work (e.g. macOS).
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
* This project is compiled with scala 3.7.2
### sbt versions
* This library can be used with sbt 1.x or 2.x
### Playwright versions
* This library can be used with playwright version 1.61.0 `"com.microsoft.playwright" % "playwright" % "1.61.0"`
### JDK versions
* This library is tested on JDK17

## Default configuration
```scala
jsEnv := new jsenv.playwright.PlaywrightJSEnv.chrome(
  headless = true,
  showLogs = false,
)
```

## Launch options

The launch options are used to enable/disable browser features. They can be overridden via the launchOptions parameter.

When not passing launchOptions, default launch options are as follows:

### Chrome/chromium
```scala
jsEnv := new jsenv.playwright.PlaywrightJSEnv.chrome(
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
jsEnv := new jsenv.playwright.PlaywrightJSEnv.firefox(
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
jsEnv := new jsenv.playwright.PlaywrightJSEnv.webkit(
  launchOptions = Nil
)
```

## KeepAlive configuration 
It is work in progress.
As a workaround introducing delay in the test cases may help to keep the browser alive. 

## Debugging
debug parameter can be passed to the PlaywrightJSEnv constructor to enable debugging. It will also display the version of the browser which is used.
```scala
Test / jsEnv := new PlaywrightJSEnv.chrome(
      headless = true,
      showLogs = true,
      debug = true
    )
```
