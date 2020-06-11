import java.io.File

import com.github.gekomad.scalacompress.Compressors._
import org.scalatest.funsuite.AnyFunSuite

import scala.util.{Failure, Success}
class GzipTest extends AnyFunSuite {

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
