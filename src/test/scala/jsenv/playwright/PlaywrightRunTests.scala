package io.github.thijsbroersen.jsenv.playwright

import org.junit.Assert._
import org.junit.Test
import org.scalajs.jsenv._
import org.scalajs.jsenv.test.kit.TestKit

import scala.concurrent.duration.DurationInt
import scala.concurrent.Await

/**
 * Tests for behavior the official JSEnvSuite does not cover: env-var injection, stderr routing,
 * uncaught async errors and close semantics.
 */
class PlaywrightRunTests {
  private val kit = new TestKit(PlaywrightJSEnv.chrome(), 30.seconds)

  @Test
  def envVarsExposedViaProcessEnv(): Unit = {
    val config = RunConfig().withEnv(Map("PLAYWRIGHT_TEST_VAR" -> "hello env"))
    kit.withRun("console.log(process.env.PLAYWRIGHT_TEST_VAR);", config) {
      _.expectOut("hello env\n").closeRun()
    }
  }

  @Test
  def envVarsWithSpecialCharacters(): Unit = {
    val tricky = "quote\" backslash\\ tab\t end"
    val config = RunConfig().withEnv(Map("TRICKY" -> tricky))
    kit.withRun("console.log(process.env.TRICKY);", config) {
      _.expectOut(tricky + "\n").closeRun()
    }
  }

  @Test
  def consoleErrorGoesToStderr(): Unit =
    kit.withRun("""console.error("to stderr"); console.log("to stdout");""") {
      _.expectOut("to stdout\n").expectErr("to stderr\n").closeRun()
    }

  @Test
  def uncaughtAsyncErrorFailsRun(): Unit =
    kit.withRun("setTimeout(function() { throw new Error('boom'); }, 0);") {
      _.fails()
    }

  @Test
  def closeReturnsPromptly(): Unit = {
    // JSRun.close() must be async: request the stop and return, without waiting
    // for the browser to shut down.
    val env = PlaywrightJSEnv.chrome()
    val run = env.start(Nil, RunConfig())
    val start = System.nanoTime()
    run.close()
    val elapsedMillis = (System.nanoTime() - start) / 1000000L
    assertTrue(s"close() blocked for ${elapsedMillis}ms", elapsedMillis < 1000)
    // The run must still terminate and release its resources.
    Await.ready(run.future, 30.seconds)
    ()
  }
}
