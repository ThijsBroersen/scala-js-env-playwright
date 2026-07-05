package io.github.thijsbroersen.jsenv.playwright

import io.github.thijsbroersen.jsenv.playwright.PlaywrightJSEnv.Config.Materialization
import org.junit.Assert._
import org.junit.Test
import org.scalajs.jsenv.Input
import org.scalajs.jsenv.UnsupportedInputException

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Browser-free unit tests for the generated HTML page.
 */
class HtmlPageTest {

  private def withMaterializer[A](f: FileMaterializer => A): A = {
    val m = FileMaterializer(Materialization.Temp)
    try f(m)
    finally m.close()
  }

  private def tmpScript(name: String): Path = {
    val dir = Files.createTempDirectory("html-page-test")
    val file = dir.resolve(name)
    Files.write(file, "console.log(1);".getBytes(StandardCharsets.UTF_8))
    file.toFile.deleteOnExit()
    file
  }

  @Test
  def scriptInputBecomesClassicScriptTag(): Unit = withMaterializer { m =>
    val page = CEUtils.htmlPage(Seq(Input.Script(tmpScript("a.js"))), m)
    assertTrue(page, page.contains("type='text/javascript'"))
  }

  @Test
  def esModuleInputBecomesModuleTag(): Unit = withMaterializer { m =>
    val page = CEUtils.htmlPage(Seq(Input.ESModule(tmpScript("b.js"))), m)
    assertTrue(page, page.contains("type='module'"))
  }

  @Test(expected = classOf[UnsupportedInputException])
  def commonJSModuleIsRejected(): Unit = withMaterializer { m =>
    CEUtils.htmlPage(Seq(Input.CommonJSModule(tmpScript("c.js"))), m)
    ()
  }

  @Test
  def urlsAreHtmlEscaped(): Unit = withMaterializer { m =>
    // An apostrophe in the path would break the single-quoted src attribute.
    val page = CEUtils.htmlPage(Seq(Input.Script(tmpScript("we'ird.js"))), m)
    assertTrue(page, page.contains("&#39;"))
    assertFalse(page, page.contains("we'ird"))
  }
}
