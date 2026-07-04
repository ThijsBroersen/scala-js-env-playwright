package io.github.thijsbroersen.jsenv.playwright

import org.scalajs.jsenv.test.JSEnvSuiteConfig

/**
 * Shared suite config declaring the capabilities this env actually supports: scripts and ES
 * modules, but no CommonJS modules (browsers have no `require`).
 */
object PlaywrightSuiteConfig {
  def apply(env: PlaywrightJSEnv): JSEnvSuiteConfig =
    JSEnvSuiteConfig(env).withSupportsCommonJSModules(false)
}
