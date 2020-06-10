import java.io.File
import com.github.gekomad.scalacompress.Compressors._
import com.github.gekomad.scalacompress.Util.SEP
import org.scalatest.funsuite.AnyFunSuite
import scala.util.{Failure, Success}

class LZ4Test extends AnyFunSuite {

  test("lz4") {
    val tmpDir  = Util.createTmpDir(suiteName)
    val file    = "aa.txt"
    val src     = new File( getClass.getResource(file).getPath).getAbsolutePath
    val srcDecr = s"$tmpDir/a2"
    val dest    = s"$tmpDir"
    new File(srcDecr).mkdirs()
    lz4Compress(src, dest) match {
      case Failure(e) => assert(false, e)
      case Success(statistics) =>
        println("-----------\n" + Util.toString(statistics) + "-----------\n")
        lz4Decompress(dest + s"$SEP$file.lz4", srcDecr) match {
          case Failure(e) => assert(false, e)
          case Success(statistics) =>
            println("-----------\n" + Util.toString(statistics) + "-----------\n")
            assert(scala.io.Source.fromFile(src).mkString == scala.io.Source.fromFile(srcDecr + s"/$file").mkString)
        }
    }
  }

}
