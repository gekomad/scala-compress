import java.io.File
import com.github.gekomad.scalacompress.Compressors._
import org.scalatest.funsuite.AnyFunSuite
import scala.util.{Failure, Success}

class Pack200Test extends AnyFunSuite {

  test("pack2000 compress") {
    val tmpDir  = Util.createTmpDir(suiteName)
    val src     = getClass.getResource("example.jar").getPath
    val srcDecr = s"$tmpDir/adec"
    val dest    = s"$tmpDir"
    new File(srcDecr).mkdirs()
    pack200Compress(src, dest) match {
      case Failure(e) => assert(false, e)
      case Success(statistics) =>
        println("-----------\n" + Util.toString(statistics) + "-----------\n")
        pack200Decompress(dest + "/example.jar.pack", srcDecr) match {
          case Failure(e) => assert(false, e)
          case Success(statistics) =>
            println("-----------\n" + Util.toString(statistics) + "-----------\n")
            assert(new File(src).length > new File(srcDecr + "/example.jar").length)
        }
    }
  }

}
