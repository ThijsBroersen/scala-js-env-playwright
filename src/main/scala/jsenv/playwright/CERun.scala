package io.github.thijsbroersen.jsenv.playwright

import org.scalajs.jsenv.Input
import org.scalajs.jsenv.JSRun
import org.scalajs.jsenv.RunConfig

import cats.effect.unsafe.implicits.global
import cats.effect.IO

import scala.concurrent._

class CERun(
    override val playwrightJsEnv: PlaywrightJSEnv,
    override val input: Seq[Input],
    override val runConfig: RunConfig
) extends JSRun
    with Runner {
  scribe.debug(s"Creating CERun for ${playwrightJsEnv.capabilities.browserName}")

  lazy val future: Future[Unit] =
    jsRunPrg(isComEnabled = false).use(_ => IO.unit).unsafeToFuture()

  override protected def receivedMessage(msg: String): Unit = ()
}
