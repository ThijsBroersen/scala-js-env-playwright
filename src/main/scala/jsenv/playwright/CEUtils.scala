package io.github.thijsbroersen.jsenv.playwright

import org.scalajs.jsenv.Input
import org.scalajs.jsenv.UnsupportedInputException
import scribe.format.classNameSimple
import scribe.format.dateFull
import scribe.format.level
import scribe.format.mdc
import scribe.format.messages
import scribe.format.methodName
import scribe.format.threadName
import scribe.format.FormatterInterpolator

import java.nio.file.Path

object CEUtils {
  def validateInput(input: Seq[Input]): Unit =
    if (input.exists(!isSupportedInput(_)))
      throw new UnsupportedInputException(input)

  private def isSupportedInput(input: Input): Boolean =
    input match {
      // CommonJS modules cannot run in a browser page: `require` and
      // `module.exports` do not exist there.
      case _: Input.Script | _: Input.ESModule => true
      case _ => false
    }

  def htmlPage(
      fullInput: Seq[Input],
      materializer: FileMaterializer
  ): String = {
    validateInput(fullInput)
    val tags = fullInput.map {
      case Input.Script(path) => makeTag(path, "text/javascript", materializer)
      case Input.ESModule(path) => makeTag(path, "module", materializer)
      case _ => throw new UnsupportedInputException(fullInput)
    }

    s"""<html>
       |  <meta charset="UTF-8">
       |  <body>
       |    ${tags.mkString("\n    ")}
       |  </body>
       |</html>
    """.stripMargin
  }

  private def makeTag(
      path: Path,
      tpe: String,
      materializer: FileMaterializer
  ): String = {
    val url = materializer.materialize(path)
    s"<script defer type='$tpe' src='${htmlEscape(url.toString)}'></script>"
  }

  private def htmlEscape(s: String): String =
    s.flatMap {
      case '&' => "&amp;"
      case '<' => "&lt;"
      case '>' => "&gt;"
      case '\'' => "&#39;"
      case '"' => "&quot;"
      case c => c.toString
    }

  def setupLogger(showLogs: Boolean, debug: Boolean): Unit = {
    val formatter =
      formatter"$dateFull [$threadName] $classNameSimple $level $methodName - $messages$mdc"
    val minimumLevel =
      if (debug) scribe.Level.Trace
      else if (showLogs) scribe.Level.Info
      else scribe.Level.Error
    // Configure only this library's logger; the scribe root logger (and thus any
    // logging of the surrounding application/build) is left untouched.
    scribe
      .Logger(PWLogger.name)
      .orphan()
      .clearHandlers()
      .withHandler(formatter = formatter, minimumLevel = Some(minimumLevel))
      .replace()
    ()
  }

}
