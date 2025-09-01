package io.github.thijsbroersen.jsenv.playwright

import io.github.thijsbroersen.jsenv.playwright.PageFactory._

import org.scalajs.jsenv.Input
import org.scalajs.jsenv.RunConfig

import cats.effect.IO
import cats.effect.Resource

import scala.concurrent.duration.DurationInt

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentLinkedQueue

trait Runner {
  def playwrightJsEnv: PlaywrightJSEnv
  def input: Seq[Input]
  def runConfig: RunConfig

  // enableCom is false for CERun and true for CEComRun
  // protected val enableCom = false
  protected val intf = "this.scalajsPlayWrightInternalInterface"
  protected val sendQueue = new ConcurrentLinkedQueue[String]
  // receivedMessage is called only from JSComRun. Hence its implementation is empty in CERun
  protected def receivedMessage(msg: String): Unit
  private val isClosed = new AtomicBoolean(false)
  private val wantToClose = new AtomicBoolean(false)
  // List of programs
  // 1. isInterfaceUp()
  // Create PW resource if not created. Create browser,context and page
  // 2. Sleep
  // 3. wantClose
  // 4. sendAll()
  // 5. fetchAndProcess()
  // 6. Close driver
  // 7. Close streams
  // 8. Close materializer
  // Flow
  // if interface is down and dont want to close wait for 100 milliseconds
  // interface is up and dont want to close sendAll(), fetchAndProcess() Sleep for 100 milliseconds
  // If want to close then close driver, streams, materializer
  // After future is completed close driver, streams, materializer

  private[playwright] def jsRunPrg(
      isComEnabled: Boolean = false
  ): Resource[IO, Unit] = for {
    _ <- Resource.make(IO.unit)(_ =>
      IO {
        isClosed.set(true)
      })
    _ <- Resource.pure(
      scribe.info(
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
      isComEnabled
    )
    connectionReady <- ResourcesFactory.isConnectionUp(pageInstance, intf)
    _ <-
      if (!connectionReady) Resource.sleep[IO](100.milliseconds)
      else Resource.unit[IO]
    _ <-
      if (!connectionReady) ResourcesFactory.isConnectionUp(pageInstance, intf)
      else Resource.unit[IO]
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
   * Stops the run and releases all the resources.
   *
   * This <strong>must</strong> be called to ensure the run's resources are released.
   *
   * Whether or not this makes the run fail or not is up to the implementation. However, in the
   * following cases, calling [[close]] may not fail the run: <ul> <li>[[jsRunPrg]] is already
   * completed when [[close]] is called. <li>This is a [[CERun]] and the event loop inside the
   * VM is empty. </ul>
   *
   * Idempotent, async, nothrow.
   */

  def close(): Unit = {
    scribe.debug(s"Received stopSignal")
    val now = System.currentTimeMillis()
    val deadline = now + 5000
    wantToClose.set(true)
    while (!isClosed.get() && System.currentTimeMillis() < deadline)
      Thread.sleep(10)
    scribe.debug(s"jsRunPrg is closed")
  }

  def getCaller: String = {
    val stackTraceElements = Thread.currentThread().getStackTrace
    if (stackTraceElements.length > 5) {
      val callerElement = stackTraceElements(5)
      s"Caller class: ${callerElement.getClassName}, method: ${callerElement.getMethodName}"
    } else
      "Could not determine caller."
  }

  def logStackTrace(): Unit =
    try
      throw new Exception("Logging stack trace")
    catch {
      case e: Exception => e.printStackTrace()
    }

}

//private class WindowOnErrorException(errs: List[String])
//  extends Exception(s"JS error: $errs")
