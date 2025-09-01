package io.github.thijsbroersen.jsenv.playwright

import org.junit.runner.RunWith
import org.scalajs.jsenv.test._

@RunWith(classOf[JSEnvSuiteRunner])
class PlaywrightJSEnvSuiteFirefox
    extends JSEnvSuite(
      JSEnvSuiteConfig(PlaywrightJSEnv.firefox(debug = false, headless = true))
    )
