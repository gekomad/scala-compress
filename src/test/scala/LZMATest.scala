import java.io.File
import com.github.gekomad.scalacompress.Compressors._
import org.scalatest.funsuite.AnyFunSuite
import scala.util.{Failure, Success}

class LZMATest extends AnyFunSuite {

  test("lzma compress") {
    val tmpDir  = Util.createTmpDir(suiteName)
    val file    = "aa.txt"
    val src     = getClass.getResource(file).getPath
    val srcDecr = s"$tmpDir/a2"
    val dest    = s"$tmpDir"
    new File(srcDecr).mkdirs()
    lzmaCompress(src, dest) match {
      case Failure(e) => assert(false, e)
      case Success(statistics) =>
        println("-----------\n" + Util.toString(statistics) + "-----------\n")
        lzmaDecompress(dest + s"/$file.lzma", srcDecr) match {
          case Failure(e) => assert(false, e)
          case Success(statistics) =>
            println("-----------\n" + Util.toString(statistics) + "-----------\n")
            assert(scala.io.Source.fromFile(src).mkString == scala.io.Source.fromFile(srcDecr + s"/$file").mkString)
        }
    }
  }

}
