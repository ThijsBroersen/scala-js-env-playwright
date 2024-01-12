# scala-js-env-playwright
A JavaScript environment for Scala.js (a JSEnv) running playwright
## Usage
Add the following line to your `project/plugins.sbt` 
```scala
// For Scala.js 1.x
libraryDependencies += "github.gmkumar2005" %% "scala-js-env-playwright" % "0.1.0-SNAPSHOT"
```
Add the following line to your `build.sbt` 
```scala
Test / jsEnv := new PWEnv(
      browserName = "chrome",
      headless = true,
      showLogs = true
    )
```
## Avoid trouble
* This is a very early version. It is not yet published to maven central. You need to clone this repo and do a `sbt publishLocal` to use it.
* It works only with Scala.js 1.x
* Make sure the project is set up to use ModuleKind.ESModule in the Scala.js project.
  ```scala
    // For Scala.js 1.x
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) }
    ```

## References
* Sample project using this JSEnv: https://github.com/gmkumar2005/scalajs-sbt-vite-laminar-chartjs-example

## Todo 
* Add examples to demonstrate how to use LaunchOptions
* Add feature to keepAlive the browser
* Optimize to use a single browser instance for all tests by creating multiple tabs