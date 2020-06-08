import com.github.gekomad.scalacompress.Compressors._
import org.scalatest.funsuite.AnyFunSuite

import scala.util.{Failure, Success}

class CpioTest extends AnyFunSuite {

  test("cpio") {
    val tmpDir  = Util.createTmpDir(suiteName)
    val src     = getClass.getResource("a").getPath
    val srcDecr = s"$tmpDir/adec"
    val dest    = s"$tmpDir/a.cpio"
    cpioCompress(List(src), dest) match {
      case Failure(e)          => assert(false, e)
      case Success(statistics) => println("-----------\n" + Util.toString(statistics) + "-----------\n")
    }
    cpioDecompress(dest, srcDecr) match {
      case Failure(e)          => assert(false, e)
      case Success(statistics) => println("-----------\n" + Util.toString(statistics) + "-----------\n")
    }
    val l  = Util.getListOfFiles(src).get.map(_.getName).toSet
    val l2 = Util.getListOfFiles(srcDecr).get.map(_.getName).toSet
    assert(l == l2)
  }

}
