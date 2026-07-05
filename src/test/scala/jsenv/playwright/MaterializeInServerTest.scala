package io.github.thijsbroersen.jsenv.playwright

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.Test
import org.scalajs.jsenv.test.kit.TestKit

import scala.concurrent.duration.DurationInt

import java.net.InetSocketAddress
import java.nio.file.Files

/**
 * End-to-end test for `Config.withMaterializeInServer`: files served over http://.
 */
class MaterializeInServerTest {

  @Test
  def runsViaHttpServer(): Unit = {
    val contentDir = Files.createTempDirectory("pw-jsenv-server-test")
    val server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext(
      "/",
      (exchange: HttpExchange) => {
        val file = contentDir.resolve(exchange.getRequestURI.getPath.stripPrefix("/"))
        if (Files.isRegularFile(file)) {
          val bytes = Files.readAllBytes(file)
          val contentType =
            if (file.toString.endsWith(".html")) "text/html" else "text/javascript"
          exchange.getResponseHeaders.add("Content-Type", contentType)
          exchange.sendResponseHeaders(200, bytes.length.toLong)
          exchange.getResponseBody.write(bytes)
        } else
          exchange.sendResponseHeaders(404, -1L)
        exchange.close()
      }
    )
    server.start()
    try {
      val port = server.getAddress.getPort
      val env = PlaywrightJSEnv(
        PlaywrightJSEnv.ChromeOptions(),
        PlaywrightJSEnv
          .Config()
          .withMaterializeInServer(contentDir.toString, s"http://127.0.0.1:$port/")
      )
      val kit = new TestKit(env, 30.seconds)
      kit.withRun("""console.log("served over http");""") {
        _.expectOut("served over http\n").closeRun()
      }
    } finally server.stop(0)
  }
}
