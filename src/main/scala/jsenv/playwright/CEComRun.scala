package io.github.thijsbroersen.jsenv.playwright

import org.scalajs.jsenv.Input
import org.scalajs.jsenv.JSComRun
import org.scalajs.jsenv.RunConfig

import cats.effect.unsafe.implicits.global
import cats.effect.IO

import scala.concurrent._

// browserName, headless, pwConfig, runConfig, input, onMessage
class CEComRun(
    override val playwrightJsEnv: PlaywrightJSEnv,
    override val input: Seq[Input],
    override val runConfig: RunConfig,
    onMessage: String => Unit
) extends JSComRun
    with Runner {
  scribe.debug(s"Creating CEComRun for ${playwrightJsEnv.capabilities.browserName}")
  // enableCom is false for CERun and true for CEComRun
  // send is called only from JSComRun
  override def send(msg: String): Unit = {
    sendQueue.offer(msg)
    ()
  }
  // receivedMessage is called only from JSComRun. Hence its implementation is empty in CERun
  override protected def receivedMessage(msg: String): Unit = onMessage(msg)

  lazy val future: Future[Unit] =
    jsRunPrg(isComEnabled = true).use_.unsafeToFuture()

}

private class WindowOnErrorException(errs: List[String]) extends Exception(s"JS error: $errs")
