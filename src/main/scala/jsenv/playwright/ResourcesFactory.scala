package io.github.thijsbroersen.jsenv.playwright

import io.github.thijsbroersen.jsenv.playwright.PlaywrightJSEnv.Config

import com.microsoft.playwright.Page
import org.scalajs.jsenv.Input
import org.scalajs.jsenv.RunConfig

import cats.effect.IO
import cats.effect.Resource

import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

import java.util
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Consumer

object ResourcesFactory {
  def preparePageForJsRun(
      pageInstance: Page,
      materializerResource: Resource[IO, FileMaterializer],
      input: Seq[Input],
      enableCom: Boolean,
      env: Map[String, String]
  ): Resource[IO, Unit] =
    for {
      m <- materializerResource
      _ <- Resource.pure(
        PWLogger.debug(s"Page instance is ${pageInstance.hashCode()}")
      )
      _ <- Resource.pure {
        try {
          val setupJsScript = Input.Script(JSSetup.setupFile(enableCom, env))
          val fullInput = setupJsScript +: input
          val materialPage =
            m.materialize(
              "scalajsRun.html",
              CEUtils.htmlPage(fullInput, m)
            )
          pageInstance.navigate(materialPage.toString)
        } catch {
          // A LinkageError here (e.g. NoSuchMethodError on Input.Script.unapply) means
          // a different, binary-incompatible version of org.scala-js:scalajs-js-envs is
          // also on the classpath and is winning dependency resolution over the 1.6.0
          // this library is compiled against. scala.util.control.NonFatal treats
          // LinkageError as fatal, which makes cats-effect skip Resource finalizers
          // (browser/playwright/page never get closed). Rewrapping as a plain exception
          // here, before it reaches the IO runtime, lets the normal Resource release
          // path run and the JSRun fail with a clear cause instead of leaking the
          // browser process.
          case e: LinkageError =>
            throw new RuntimeException(
              "Failed to prepare the page for the run. This usually means a different, " +
                "binary-incompatible version of org.scala-js:scalajs-js-envs is on the " +
                "classpath alongside the 1.6.0 that scala-js-env-playwright was compiled " +
                "against (e.g. pulled in transitively by the Scala.js sbt plugin). Check " +
                "for a version conflict and align both to the same scalajs-js-envs version.",
              e
            )
        }
      }
    } yield ()

  private def fetchMessages(
      pageInstance: Page,
      intf: String
  ): java.util.Map[String, java.util.List[String]] = {
    val data =
      pageInstance
        .evaluate(s"$intf.fetch();")
        .asInstanceOf[java.util.Map[String, java.util.List[String]]]
    data
  }

  def processUntilStop(
      stopSignal: AtomicBoolean,
      pageInstance: Page,
      intf: String,
      sendQueue: ConcurrentLinkedQueue[String],
      outStream: OutputStreams.Streams,
      receivedMessage: String => Unit
  ): Resource[IO, Unit] =
    Resource.eval {
      for
        _ <- IO(PWLogger.debug(s"Started processUntilStop"))
        _ <- IO {
          sendAll(sendQueue, pageInstance, intf)
          val jsResponse = fetchMessages(pageInstance, intf)
          streamWriter(jsResponse, outStream, Some(receivedMessage))
        }.andWait(100.milliseconds).iterateUntil(_ => stopSignal.get())
        _ <- IO(PWLogger.debug(s"Stop processUntilStop"))
      yield ()
    }

  /**
   * Polls the page every 100 ms until the internal interface is available, the stop signal is
   * set, or `timeout` expires. Evaluation errors during page load count as "not ready yet".
   */
  def awaitConnection(
      pageInstance: Page,
      intf: String,
      stopSignal: AtomicBoolean,
      timeout: FiniteDuration = 30.seconds
  ): Resource[IO, Unit] =
    Resource.eval {
      val check: IO[Boolean] =
        IO.blocking(pageInstance.evaluate(s"!!$intf;").asInstanceOf[Boolean]).handleError { t =>
          PWLogger.debug(s"Connection check failed, retrying: $t")
          false
        }

      def poll: IO[Unit] =
        check.flatMap { up =>
          if (up || stopSignal.get()) IO.unit
          else IO.sleep(100.milliseconds) >> poll
        }

      poll.timeoutTo(
        timeout,
        IO.raiseError(
          new Exception(
            s"The page did not expose the Scala.js run interface within $timeout"
          )
        )
      )
    }

  def materializer(pwConfig: Config): Resource[IO, FileMaterializer] =
    Resource.make {
      IO.blocking(FileMaterializer(pwConfig.materialization)) // build
    } { fileMaterializer =>
      IO {
        PWLogger.debug("Closing the fileMaterializer")
        fileMaterializer.close()
      }.handleErrorWith { _ =>
        PWLogger.error("Error in closing the fileMaterializer")
        IO.unit
      } // release
    }

  /*
   * Creates resource for outputStream
   */
  def outputStream(
      runConfig: RunConfig
  ): Resource[IO, OutputStreams.Streams] =
    Resource.make {
      IO.blocking(OutputStreams.prepare(runConfig)) // build
    } { outStream =>
      IO {
        PWLogger.debug(s"Closing the stream ${outStream.hashCode()}")
        outStream.close()
      }.handleErrorWith { _ =>
        PWLogger.error(s"Error in closing the stream ${outStream.hashCode()})")
        IO.unit
      } // release
    }

  private def streamWriter(
      jsResponse: util.Map[String, util.List[String]],
      outStream: OutputStreams.Streams,
      onMessage: Option[String => Unit]
  ): Unit = {
    val data = jsResponse.get("consoleLog")
    val consoleError = jsResponse.get("consoleError")
    val error = jsResponse.get("errors")
    onMessage match {
      case Some(f) =>
        val msgs = jsResponse.get("msgs")
        msgs.forEach(consumer(f))
      case None => PWLogger.debug("No onMessage function")
    }
    data.forEach(outStream.out.println)
    consoleError.forEach(outStream.err.println)
    error.forEach(outStream.err.println)

    if (!error.isEmpty) {
      val errList = error.toArray(Array[String]()).toList
      throw new WindowOnErrorException(errList)
    }
  }

  @tailrec
  def sendAll(
      sendQueue: ConcurrentLinkedQueue[String],
      pageInstance: Page,
      intf: String
  ): Unit = {
    val msg = sendQueue.poll()
    if (msg != null) {
      PWLogger.debug(s"Sending message")
      val script = s"$intf.send(arguments[0]);"
      val wrapper = s"function(arg) { $script }"
      pageInstance.evaluate(s"$wrapper", msg)
      sendAll(sendQueue, pageInstance, intf)
    }
  }
  private def consumer[A](f: A => Unit): Consumer[A] = (v: A) => f(v)
}
