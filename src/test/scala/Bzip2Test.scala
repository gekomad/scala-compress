import java.io.File
import org.scalatest.funsuite.AnyFunSuite
import scala.util.{Failure, Success}
import com.github.gekomad.scalacompress.Compressors._

class Bzip2Test extends AnyFunSuite {

  test("bzip2") {
    val tmpDir  = Util.createTmpDir(suiteName)
    val file    = "aa.txt"
    val src     = getClass.getResource(file).getPath
    val srcDecr = s"$tmpDir/dest"
    val dest    = s"$tmpDir"
    new File(srcDecr).mkdirs()
    bzip2Compress(src, dest) match {
      case Failure(e) => assert(false, e)
      case Success(statistics) =>
        println("-----------\n" + Util.toString(statistics) + "-----------\n")
        bzip2Decompress(dest + s"/$file.bz2", srcDecr) match {
          case Failure(e) => assert(false, e)
          case Success(statistics) =>
            println("-----------\n" + Util.toString(statistics) + "-----------\n")
            assert(scala.io.Source.fromFile(src).mkString == scala.io.Source.fromFile(srcDecr + s"/$file").mkString)
        }
    }
  }
}
