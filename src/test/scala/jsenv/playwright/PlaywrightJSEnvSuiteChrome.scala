package io.github.thijsbroersen.jsenv.playwright

import org.junit.runner.RunWith
import org.scalajs.jsenv.test._

@RunWith(classOf[JSEnvSuiteRunner])
class PlaywrightJSEnvSuiteChrome
    extends JSEnvSuite(
      JSEnvSuiteConfig(PlaywrightJSEnv.chrome(debug = false, headless = true))
    )
