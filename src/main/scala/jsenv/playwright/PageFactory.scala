package io.github.thijsbroersen.jsenv.playwright

import com.microsoft.playwright.Page

import cats.effect.IO
import cats.effect.Resource

object PageFactory {
  def createPage(env: PlaywrightJSEnv): Resource[IO, Page] =
    env.browserSession.pageResource
}
