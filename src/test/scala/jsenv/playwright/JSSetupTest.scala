package io.github.thijsbroersen.jsenv.playwright

import org.junit.Assert._
import org.junit.Test

import java.nio.charset.StandardCharsets
import java.nio.file.Files

/**
 * Browser-free unit tests for the generated setup script.
 */
class JSSetupTest {

  private def setupCode(enableCom: Boolean, env: Map[String, String] = Map.empty): String =
    new String(Files.readAllBytes(JSSetup.setupFile(enableCom, env)), StandardCharsets.UTF_8)

  @Test
  def comInterfaceGatedOnEnableCom(): Unit = {
    assertTrue(setupCode(enableCom = true).contains("if (true)"))
    assertTrue(setupCode(enableCom = false).contains("if (false)"))
  }

  @Test
  def envVarsAreEmittedAsProcessEnv(): Unit = {
    val code = setupCode(enableCom = false, env = Map("FOO" -> "bar"))
    assertTrue(code, code.contains("\"FOO\": \"bar\""))
    assertTrue(code, code.contains("this.process = proc"))
  }

  @Test
  def envValuesAreEscaped(): Unit = {
    val code = setupCode(enableCom = false, env = Map("K" -> "a\"b\\c\nd"))
    assertTrue(code, code.contains("\"a\\\"b\\\\c\\nd\""))
  }

  @Test
  def emptyEnvEmitsEmptyObject(): Unit = {
    val code = setupCode(enableCom = false)
    assertTrue(code, code.contains("var envVars = {};"))
  }
}
