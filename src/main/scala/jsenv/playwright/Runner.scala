package io.github.thijsbroersen.jsenv.playwright

import io.github.thijsbroersen.jsenv.playwright.PageFactory._

import org.scalajs.jsenv.Input
import org.scalajs.jsenv.RunConfig

import cats.effect.IO
import cats.effect.Resource

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentLinkedQueue

trait Runner {
  def playwrightJsEnv: PlaywrightJSEnv
  def input: Seq[Input]
  def runConfig: RunConfig

  protected val intf = "this.scalajsPlayWrightInternalInterface"
  protected val sendQueue = new ConcurrentLinkedQueue[String]
  // receivedMessage is called only from JSComRun. Hence its implementation is empty in CERun
  protected def receivedMessage(msg: String): Unit
  private val wantToClose = new AtomicBoolean(false)

  private[playwright] def jsRunPrg(
      isComEnabled: Boolean = false
  ): Resource[IO, Unit] = for {
    _ <- Resource.pure(
      PWLogger.info(
        s"Begin Main with isComEnabled $isComEnabled " +
          s"and  browserName ${playwrightJsEnv.capabilities.browserName} " +
          s"and headless is ${playwrightJsEnv.capabilities.headless} "
      )
    )
    pageInstance <- createPage(
      playwrightJsEnv.capabilities
    )
    _ <- ResourcesFactory.preparePageForJsRun(
      pageInstance,
      ResourcesFactory.materializer(playwrightJsEnv.config),
      input,
      isComEnabled,
      runConfig.env
    )
    _ <- ResourcesFactory.awaitConnection(pageInstance, intf, wantToClose)
    out <- ResourcesFactory.outputStream(runConfig)
    _ <- ResourcesFactory.processUntilStop(
      wantToClose,
      pageInstance,
      intf,
      sendQueue,
      out,
      receivedMessage
    )
  } yield ()

  /**
   * Requests the run to stop and release all its resources.
   *
   * This <strong>must</strong> be called to ensure the run's resources are released.
   *
   * Idempotent, async, nothrow: returns immediately. Completion of the shutdown is observed
   * through the run's `future`.
   */
  def close(): Unit = {
    PWLogger.debug("Received stopSignal")
    wantToClose.set(true)
  }

}
