package io.github.thijsbroersen.jsenv.playwright

import io.github.thijsbroersen.jsenv.playwright.PWEnv.Config
import io.github.thijsbroersen.jsenv.playwright.PageFactory._
import io.github.thijsbroersen.jsenv.playwright.ResourcesFactory._

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.BrowserType.LaunchOptions
import org.scalajs.jsenv.Input
import org.scalajs.jsenv.RunConfig

import cats.effect.IO
import cats.effect.Resource

import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentLinkedQueue

trait Runner {
  val browserName: String = "" // or provide actual values
  val headless: Boolean = false // or provide actual values
  val pwConfig: Config = Config() // or provide actual values
  val runConfig: RunConfig = RunConfig() // or provide actual values
  val input: Seq[Input] = Seq.empty // or provide actual values
  val launchOptions: List[String] = Nil
  val additionalLaunchOptions: List[String] = Nil

  // enableCom is false for CERun and true for CEComRun
  protected val enableCom = false
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

  def jsRunPrg(
      browserName: String,
      headless: Boolean,
      isComEnabled: Boolean,
      launchOptions: LaunchOptions
  ): Resource[IO, Unit] = for {
    _ <- Resource.make(IO.unit)(_ =>
      IO {
        isClosed.set(true)
      })
    _ <- Resource.pure(
      scribe.info(
        s"Begin Main with isComEnabled $isComEnabled " +
          s"and  browserName $browserName " +
          s"and headless is $headless "
      )
    )
    pageInstance <- createPage(
      browserName,
      headless,
      launchOptions
    )
    _ <- preparePageForJsRun(
      pageInstance,
      materializer(pwConfig),
      input,
      isComEnabled
    )
    connectionReady <- isConnectionUp(pageInstance, intf)
    _ <-
      if (!connectionReady) Resource.sleep[IO](100.milliseconds)
      else Resource.unit[IO]
    _ <-
      if (!connectionReady) isConnectionUp(pageInstance, intf)
      else Resource.unit[IO]
    out <- outputStream(runConfig)
    _ <- processUntilStop(
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

  protected lazy val pwLaunchOptions =
    browserName.toLowerCase() match {
      case "chromium" | "chrome" =>
        new BrowserType.LaunchOptions().setArgs(
          if (launchOptions.isEmpty)
            (PWEnv.chromeLaunchOptions ++ additionalLaunchOptions).asJava
          else (launchOptions ++ additionalLaunchOptions).asJava
        )
      case "firefox" =>
        import scala.jdk.CollectionConverters._
        new BrowserType.LaunchOptions()
          .setFirefoxUserPrefs(
            PWEnv
              .firefoxUserPrefs
              .view
              .mapValues(_.asInstanceOf[java.lang.Object])
              .toMap
              .asJava)
          .setArgs(
            if (launchOptions.isEmpty)
              (PWEnv.firefoxLaunchOptions ++ additionalLaunchOptions).asJava
            else (launchOptions ++ additionalLaunchOptions).asJava
          )
      case "webkit" =>
        new BrowserType.LaunchOptions().setArgs(
          if (launchOptions.isEmpty)
            (PWEnv.webkitLaunchOptions ++ additionalLaunchOptions).asJava
          else (launchOptions ++ additionalLaunchOptions).asJava
        )
      case _ => throw new IllegalArgumentException("Invalid browser type")
    }

}

//private class WindowOnErrorException(errs: List[String])
//  extends Exception(s"JS error: $errs")
