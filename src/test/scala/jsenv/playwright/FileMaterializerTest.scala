package io.github.thijsbroersen.jsenv.playwright

import com.google.common.jimfs.Jimfs
import io.github.thijsbroersen.jsenv.playwright.PlaywrightJSEnv.Config.Materialization
import org.junit.Assert._
import org.junit.Test

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Browser-free unit tests for the file materializers.
 */
class FileMaterializerTest {

  private def readBack(url: java.net.URL): String =
    new String(Files.readAllBytes(Paths.get(url.toURI)), StandardCharsets.UTF_8)

  @Test
  def tempMaterializerReturnsDirectUrlForRealFiles(): Unit = {
    val file = Files.createTempFile("mat-test", ".js")
    try {
      Files.write(file, "console.log(1);".getBytes(StandardCharsets.UTF_8))
      val m = FileMaterializer(Materialization.Temp)
      try {
        val url = m.materialize(file)
        assertEquals("file", url.getProtocol)
        assertEquals("console.log(1);", readBack(url))
      } finally m.close()
    } finally Files.delete(file)
  }

  @Test
  def tempMaterializerCopiesVirtualFiles(): Unit = {
    val virtual = Jimfs.newFileSystem().getPath("virtual.js")
    Files.write(virtual, "console.log(2);".getBytes(StandardCharsets.UTF_8))

    val m = FileMaterializer(Materialization.Temp)
    val url = m.materialize(virtual)
    val materialized = Paths.get(url.toURI)
    try {
      assertEquals("file", url.getProtocol)
      assertNotEquals(virtual.toString, materialized.toString)
      assertEquals("console.log(2);", readBack(url))
    } finally {
      m.close()
      assertFalse("close() must delete materialized temp files", Files.exists(materialized))
    }
  }

  @Test
  def tempMaterializerWritesContent(): Unit = {
    val m = FileMaterializer(Materialization.Temp)
    val url = m.materialize("page.html", "<html></html>")
    val materialized = Paths.get(url.toURI)
    try assertEquals("<html></html>", readBack(url).trim)
    finally {
      m.close()
      assertFalse("close() must delete materialized temp files", Files.exists(materialized))
    }
  }

  @Test
  def serverMaterializerCreatesContentDirAndWebRootUrls(): Unit = {
    val contentDir = Files.createTempDirectory("mat-server-test").resolve("static")
    assertFalse(Files.exists(contentDir))

    val webRoot = new URI("http://localhost:8080/assets/").toURL
    val m = FileMaterializer(Materialization.Server(contentDir, webRoot))
    try {
      assertTrue("contentDir must be created", Files.isDirectory(contentDir))

      val url = m.materialize("app.js", "console.log(3);")
      assertTrue(
        s"URL $url must be under the webRoot",
        url.toString.startsWith("http://localhost:8080/assets/")
      )

      val fileName = url.toString.stripPrefix("http://localhost:8080/assets/")
      val onDisk: Path = contentDir.resolve(fileName)
      assertTrue("materialized file must be inside contentDir", Files.isRegularFile(onDisk))
    } finally m.close()
  }

  @Test(expected = classOf[IllegalArgumentException])
  def serverMaterializationRequiresTrailingSlash(): Unit = {
    Materialization.Server(
      Paths.get("."),
      new URI("http://localhost:8080/assets").toURL
    )
    ()
  }
}
