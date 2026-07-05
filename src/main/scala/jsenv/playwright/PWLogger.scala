package io.github.thijsbroersen.jsenv.playwright

/**
 * Library-scoped logger. Configured by [[CEUtils.setupLogger]] and detached from the scribe
 * root logger, so user/application logging is never affected by this library.
 */
private[playwright] object PWLogger {
  private[playwright] val name = "io.github.thijsbroersen.jsenv.playwright"

  inline def trace(inline msg: String): Unit = scribe.Logger(name).trace(msg)
  inline def debug(inline msg: String): Unit = scribe.Logger(name).debug(msg)
  inline def info(inline msg: String): Unit = scribe.Logger(name).info(msg)
  inline def error(inline msg: String): Unit = scribe.Logger(name).error(msg)
}
