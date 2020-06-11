import java.io.FileInputStream
import java.nio.charset.StandardCharsets

import org.scalatest.funsuite.AnyFunSuite

import scala.annotation.tailrec
import scala.util.{Failure, Success}

class StreamTest extends AnyFunSuite {

  test("stream") {
    import java.io.FileOutputStream

    import com.github.gekomad.scalacompress.Compressors._
    import com.github.gekomad.scalacompress.Util.SEP

    val tmpDir = Util.createTmpDir(suiteName)

    val zFile = s"$tmpDir${SEP}file"

    for (method <- StreamableCompressor.values if method != StreamableCompressor.PACK200) {

      {
        val zStream          = StreamCompress(method, new FileOutputStream(zFile))
        val foo: Array[Byte] = "foo".getBytes(StandardCharsets.UTF_8)
        val bar: Array[Byte] = "bar".getBytes(StandardCharsets.UTF_8)

        zStream.compressBuffer(foo)
        zStream.compressBuffer(bar)

        zStream.close()
      }

      {
        val zStream      = StreamDecompress(method, new FileInputStream(zFile))
        val buffer       = new Array[Byte](2)
        val decompressed = new StringBuilder

        @tailrec
        def readBuffer(): Unit = {
          zStream.readInBuffer(buffer) match {
            case Failure(exception) => exception.printStackTrace()
            case Success(bytesRead) =>
              if (bytesRead != -1) {
                decompressed.append(new String(buffer, StandardCharsets.UTF_8))
                readBuffer()
              } else {
                println
                zStream.close()
              }
          }
        }
        readBuffer()
        assert(decompressed.toString == "foobar")
      }
    }
  }

}
