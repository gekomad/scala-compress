import com.github.gekomad.scalacompress.Compressors._
import org.scalatest.funsuite.AnyFunSuite
import scala.util.{Failure, Success}

class TarTest extends AnyFunSuite {

  test("tar folder") {
    val tmpDir = Util.createTmpDir(suiteName)

    val dest    = s"$tmpDir/tar2.tar"
    val src     = getClass.getResource("a").getPath
    val srcDecr = s"$tmpDir/tar1.dec"
    tarCompress(List(src), dest) match {
      case Failure(e) => assert(false, e)
      case Success(statistics) =>
        println("-----------\n" + Util.toString(statistics) + "-----------\n")
    }
    assert(tarDecompress(dest, srcDecr).isSuccess)

    val l  = Util.getListOfFiles(src).get
    val l2 = Util.getListOfFiles(srcDecr).get
    assert(l.size == l2.size)
    (l zip l2) foreach { r =>
      val (f1, f2) = r
      assert(f1.getName == f2.getName)
      assert(scala.io.Source.fromFile(f1).mkString == scala.io.Source.fromFile(f2).mkString)
    }

  }

  test("tar duplicate file") {
    val tmpDir = Util.createTmpDir(suiteName)

    val src = List(
      getClass.getResource("aa.txt").getPath,
      getClass.getResource("b").getPath,
      getClass.getResource("a/aa.txt").getPath,
      getClass.getResource("a/b/b.txt").getPath
    )

    val dest = s"$tmpDir/tar1.tar"

    tarCompress(src, dest) match {
      case Failure(exception) if exception.getMessage == "Duplicate files: aa.txt" => assert(true)
      case _                                                                       => assert(false)
    }

  }

  test("tar multiple file") {
    val tmpDir = Util.createTmpDir(suiteName)

    val src = List(
      getClass.getResource("b").getPath,
      getClass.getResource("a/aa.txt").getPath,
      getClass.getResource("a/b/b.txt").getPath
    )

    val dest    = s"$tmpDir/tar1.tar"
    val srcDecr = s"$tmpDir/tar1.dec"

    assert(tarCompress(src, dest).isSuccess)

    assert(tarDecompress(dest, srcDecr).isSuccess)

    val l1 = Util.getListOfFiles(srcDecr).get

    val root = Util.commonPath(l1)
    val l    = l1.map(f => f.getAbsolutePath.substring(root.length + 1))

    assert(l.toSet == Set("aa.txt", "b.txt", "b/bb.txt"))
  }

  test("tar single file") {
    val tmpDir  = Util.createTmpDir(suiteName)
    val file    = "aa.txt"
    val src     = getClass.getResource(file).getPath
    val dest    = s"$tmpDir/tar1.tar"
    val srcDecr = s"$tmpDir/tar1.dec"

    tarCompress(List(src), dest) match {
      case Failure(value) => assert(false, value)
      case Success(statistics) =>
        println("-----------\n" + Util.toString(statistics) + "-----------\n")
    }

    tarDecompress(dest, srcDecr) match {
      case Failure(value) => assert(false, value)
      case Success(statistics) =>
        println("-----------\n" + Util.toString(statistics) + "-----------\n")
    }

    val l1   = Util.getListOfFiles(srcDecr).get
    val root = Util.commonPath(l1)
    val l    = l1.map(f => f.getAbsolutePath.substring(root.length + 1))

    assert(l.toSet == Set(file))

  }
}
