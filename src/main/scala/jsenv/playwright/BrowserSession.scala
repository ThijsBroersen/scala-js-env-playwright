package io.github.thijsbroersen.jsenv.playwright

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright

import cats.effect.IO
import cats.effect.Resource

import scala.jdk.CollectionConverters._

/**
 * Owns Playwright browser lifecycle for a [[PlaywrightJSEnv]].
 *
 * By default each JS run launches and closes its own browser. With reuse enabled, the browser
 * (and Playwright driver) stay alive between runs until [[shutdown()]].
 */
private[playwright] final class BrowserSession(
    capabilities: PlaywrightJSEnv.Capabilities,
    reuseBrowserBetweenRuns: Boolean
) {
  private val lock = new Object()
  private var playwright: Playwright = null
  private var browser: Browser = null

  def pageResource: Resource[IO, Page] =
    if (reuseBrowserBetweenRuns) reusedPage else ephemeralPage

  /** Releases a reused browser and Playwright driver, if any. Idempotent. */
  def shutdown(): Unit = lock.synchronized {
    closeQuietly(browser)
    browser = null
    closeQuietly(playwright)
    playwright = null
  }

  private def ephemeralPage: Resource[IO, Page] =
    for {
      playwright <- playwrightResource
      browser <- browserResource(playwright)
      page <- pageResource(browser)
    } yield page

  private def reusedPage: Resource[IO, Page] =
    Resource.make {
      IO.blocking {
        lock.synchronized {
          ensureSharedBrowser()
          val context = browser.newContext()
          context.newPage()
        }
      }
    } { page =>
      IO.blocking {
        lock.synchronized {
          val context = page.context()
          closeQuietly(page)
          closeQuietly(context)
        }
      }
    }

  private def ensureSharedBrowser(): Unit = {
    if (browser != null && playwright != null) ()
    else {
      closeQuietly(browser)
      closeQuietly(playwright)
      playwright = CEUtils.createPlaywright()
      browser = launchBrowser(playwright)
    }
  }

  private def playwrightResource: Resource[IO, Playwright] =
    Resource.make(IO.blocking(CEUtils.createPlaywright()))(pw => IO.blocking(closeQuietly(pw)))

  private def browserResource(playwright: Playwright): Resource[IO, Browser] =
    Resource.make(IO.blocking(launchBrowser(playwright)))(b => IO.blocking(closeQuietly(b)))

  private def pageResource(browser: Browser): Resource[IO, Page] =
    Resource.make {
      IO.blocking {
        val page = browser.newContext().newPage()
        PWLogger.debug(s"Creating page ${page.hashCode()} ")
        page
      }
    } { page =>
      IO.blocking {
        val context = page.context()
        closeQuietly(page)
        closeQuietly(context)
      }
    }

  private def launchBrowser(playwright: Playwright): Browser = {
    val launchTimeoutMs =
      if (capabilities.browserName == "webkit") 120_000.0 else 60_000.0
    val launchOptions = new com.microsoft.playwright.BrowserType.LaunchOptions()
      .setHeadless(capabilities.headless)
      .setArgs(capabilities.launchOptions.asJava)
      .setTimeout(launchTimeoutMs)

    val browserType = capabilities match {
      case _: PlaywrightJSEnv.ChromeOptions =>
        playwright.chromium()
      case ff: PlaywrightJSEnv.FirefoxOptions =>
        launchOptions.setFirefoxUserPrefs(ff.firefoxUserPrefs.asJava)
        playwright.firefox()
      case _: PlaywrightJSEnv.WebkitOptions =>
        playwright.webkit()
    }

    val launched = browserType.launch(launchOptions)
    PWLogger.info(
      s"Creating browser ${launched.browserType().name()} version ${launched.version()} " +
        s"with ${launched.hashCode()}"
    )
    launched
  }

  private def closeQuietly(autoCloseable: AutoCloseable): Unit =
    if (autoCloseable != null) {
      try autoCloseable.close()
      catch { case _: Exception => () }
    }

  private def closeQuietly(context: BrowserContext): Unit =
    if (context != null) {
      try context.close()
      catch { case _: Exception => () }
    }
}
