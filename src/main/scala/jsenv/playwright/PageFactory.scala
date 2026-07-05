package io.github.thijsbroersen.jsenv.playwright

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright

import cats.effect.IO
import cats.effect.Resource

import scala.jdk.CollectionConverters._

object PageFactory {
  private def pageBuilder(browser: Browser): Resource[IO, Page] =
    Resource.make(IO {
      val pg = browser.newContext().newPage()
      PWLogger.debug(s"Creating page ${pg.hashCode()} ")
      pg
    })(page =>
      IO {
        page.close()
      })

  private def browserBuilder(
      playwright: Playwright,
      capabilities: PlaywrightJSEnv.Capabilities
  ): Resource[IO, Browser] =
    Resource.make(IO {

      val launchOptions = new BrowserType.LaunchOptions()
        .setHeadless(capabilities.headless)
        .setArgs(capabilities.launchOptions.asJava)

      val browserType: BrowserType = capabilities match
        case capabilities: PlaywrightJSEnv.ChromeOptions =>
          playwright.chromium()
        case capabilities: PlaywrightJSEnv.FirefoxOptions =>
          launchOptions.setFirefoxUserPrefs(
            capabilities
              .firefoxUserPrefs
              // .view
              // .mapValues(_.asInstanceOf[java.lang.Object])
              // .toMap
              .asJava)
          playwright.firefox()
        case capabilities: PlaywrightJSEnv.WebkitOptions =>
          playwright.webkit()

      val browser = browserType.launch(launchOptions)
      PWLogger.info(
        s"Creating browser ${browser.browserType().name()} version ${browser.version()} with ${browser.hashCode()}"
      )
      browser
    })(browser =>
      IO {
        PWLogger.debug(s"Closing browser with ${browser.hashCode()}")
        browser.close()
      })

  private def playWrightBuilder: Resource[IO, Playwright] =
    Resource.make(IO {
      PWLogger.debug(s"Creating playwright")
      Playwright.create()
    })(pw =>
      IO {
        PWLogger.debug("Closing playwright")
        pw.close()
      })

  def createPage(
      capabilities: PlaywrightJSEnv.Capabilities
  ): Resource[IO, Page] =
    for {
      playwright <- playWrightBuilder
      browser <- browserBuilder(
        playwright,
        capabilities
      )
      page <- pageBuilder(browser)
    } yield page

}
