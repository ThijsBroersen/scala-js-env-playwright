package io.github.thijsbroersen.jsenv.playwright

import com.microsoft.playwright.Page

import cats.effect.IO
import cats.effect.Resource

import java.util.concurrent.ConcurrentHashMap

object PageFactory {
  private val sessions = new ConcurrentHashMap[Integer, BrowserSession]()

  private def sessionFor(env: PlaywrightJSEnv): BrowserSession =
    sessions.computeIfAbsent(
      Integer.valueOf(System.identityHashCode(env)),
      (_: Integer) =>
        new BrowserSession(env.capabilities, env.config.reuseBrowserBetweenRuns)
    )

  def createPage(env: PlaywrightJSEnv): Resource[IO, Page] =
    sessionFor(env).pageResource

  /** Binary-compatible overload for callers that only pass capabilities. */
  def createPage(capabilities: PlaywrightJSEnv.Capabilities): Resource[IO, Page] =
    createPage(PlaywrightJSEnv(capabilities, PlaywrightJSEnv.Config()))

  def shutdownSharedBrowser(env: PlaywrightJSEnv): Unit =
    Option(sessions.remove(Integer.valueOf(System.identityHashCode(env))))
      .foreach(_.shutdown())
}
