import java.io.{File, FileInputStream}

import org.scalatest.funsuite.AnyFunSuite
import com.github.gekomad.scalacompress.Compressors._

class Stream2Test extends AnyFunSuite {

  test("stream2") {
    import java.io.FileOutputStream

    val tmpDir = Util.createTmpDir(suiteName)

    val file    = "aa.txt"
    val src     = new File(getClass.getResource(file).getPath).getAbsolutePath
    val srcDecr = s"$tmpDir/a2"
    new File(srcDecr).mkdirs()
    val dest = s"$tmpDir/compressed"
    for (method <- StreamableCompressor.values if method != StreamableCompressor.PACK200) {

      assert(compressStream(method, new FileInputStream(src), new FileOutputStream(dest)).isSuccess)

      assert(decompressStream(method, new FileInputStream(dest), new FileOutputStream(srcDecr + s"/$file")).isSuccess)
      assert(scala.io.Source.fromFile(src).mkString == scala.io.Source.fromFile(srcDecr + s"/$file").mkString)
    }
  }

}
