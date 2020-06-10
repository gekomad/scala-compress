import java.io.{File, FileInputStream}
import java.nio.charset.StandardCharsets
import org.scalatest.funsuite.AnyFunSuite
import scala.util.{Failure, Success}
import com.github.gekomad.scalacompress.Compressors._
import scala.annotation.tailrec

import com.github.gekomad.scalacompress.Util.SEP
class GZTest extends AnyFunSuite {

  test("gz compress buffer") {
    import java.io.FileOutputStream

      val tmpDir  = Util.createTmpDir(suiteName)
      val file    ="aa.txt"

    val src     = getClass.getResource(file).getPath

    val dest    = s"$tmpDir"

      assert(gzCompress(src, dest).isSuccess)

    val gzFile =dest + s"${SEP}$file.gz"

    {
      val gzStream         = GzCompressBuffer(new FileOutputStream(gzFile))
      val foo: Array[Byte] = "foo".getBytes(StandardCharsets.UTF_8)
      val bar: Array[Byte] = "bar".getBytes(StandardCharsets.UTF_8)

      gzStream.compressBuffer(foo)
      gzStream.compressBuffer(bar)

      gzStream.close()
    }

    {
      val gzStream     = GzDecompressInBuffer(new FileInputStream(gzFile))
      val buffer       = new Array[Byte](2)
      val decompressed = new StringBuilder

      @tailrec
      def readBuffer(): Unit = {
        gzStream.readInBuffer(buffer) match {
          case Failure(exception) => exception.printStackTrace()
          case Success(bytesRead) =>
            if (bytesRead != -1) {
              decompressed.append(new String(buffer, StandardCharsets.UTF_8))
              readBuffer()
            } else {
              println
              gzStream.close()
            }
        }
      }
      readBuffer()
      assert(decompressed.toString == "foobar")
    }
  }

  test("gz compress file") {
    val tmpDir  = Util.createTmpDir(suiteName)
    val file    = "aa.txt"
    val src     = getClass.getResource(file).getPath
    val srcDecr = s"$tmpDir/dest/"
    val dest    = s"$tmpDir"
    new File(srcDecr).mkdirs()
    gzCompress(src, dest) match {
      case Failure(e) => assert(false, e)
      case Success(statistics) =>
        println("-----------\n" + Util.toString(statistics) + "-----------\n")
        gzDecompress(dest + s"/$file.gz", srcDecr) match {
          case Failure(e) => assert(false, e)
          case Success(statistics) =>
            println("-----------\n" + Util.toString(statistics) + "-----------\n")
            assert(scala.io.Source.fromFile(src).mkString == scala.io.Source.fromFile(srcDecr + s"/$file").mkString)
        }
    }
  }
}
