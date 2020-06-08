import java.io.File

import org.scalatest.funsuite.AnyFunSuite

import scala.util.{Failure, Success}
import com.github.gekomad.scalacompress.Compressors._

class SevenZipTest extends AnyFunSuite {

  test("7zip folder") {
    val tmpDir = Util.createTmpDir(suiteName)

    val dest    = s"$tmpDir/seven7.7z"
    val src     = getClass.getResource("a").getPath
    val srcDecr = s"$tmpDir/a_dec"
    new File(srcDecr).mkdirs()
    assert(sevenZipCompress(List(src), dest).isSuccess)
    assert(sevenZipDecompress(dest, srcDecr).isSuccess)
    val l  = Util.getListOfFiles(src).get
    val l2 = Util.getListOfFiles(srcDecr).get
    assert(l.size == l2.size)
    (l zip l2) foreach { r =>
      val (f1, f2) = r
      assert(f1.getName == f2.getName)
      assert(scala.io.Source.fromFile(f1).mkString == scala.io.Source.fromFile(f2).mkString)
    }

  }

  test("7zip single file") {
    val tmpDir  = Util.createTmpDir(suiteName)
    val file    = "aa.txt"
    val src     = getClass.getResource(file).getPath
    val srcDecr = s"$tmpDir/out"
    new File(srcDecr).mkdirs()
    val dest = s"$tmpDir/a.7z"

    sevenZipCompress(List(src), dest) match {
      case Failure(value) => assert(false, value)
      case Success(statistics) =>
        println("-----------\n" + Util.toString(statistics) + "-----------\n")
        sevenZipDecompress(dest, srcDecr) match {
          case Failure(exception) => assert(false, exception)
          case Success(statistics) =>
            println("-----------\n" + Util.toString(statistics) + "-----------\n")
            assert(scala.io.Source.fromFile(src).mkString == scala.io.Source.fromFile(srcDecr + s"/$file").mkString)
        }
    }
  }

  test("7zip duplicate file") {
    val tmpDir = Util.createTmpDir(suiteName)

    val files = List("aa.txt", "aa.txt", "a/b/b.txt")
    val src   = files.map(i => getClass.getResource(i).getPath)

    val dest = s"$tmpDir/a.7z"

    sevenZipCompress(src, dest) match {
      case Failure(exception) if exception.getMessage == "Duplicate files: aa.txt" => assert(true)
      case _                                                                       => assert(false)
    }
  }

  test("7zip multiple file and folder") {
    val tmpDir = Util.createTmpDir(suiteName)

    val files = List("aa.txt", "a", "a/b/b.txt")
    val src   = files.map(i => getClass.getResource(i).getPath)

    val srcDecr = s"$tmpDir/out"
    new File(srcDecr).mkdirs()
    val dest = s"$tmpDir/a.7z"

    sevenZipCompress(src, dest) match {
      case Failure(e) => assert(false, e)
      case Success(statistics) =>
        println("-----------\n" + Util.toString(statistics) + "-----------\n")
        sevenZipDecompress(dest, srcDecr) match {
          case Failure(exception) => assert(false, exception)
          case _ =>
            val src1 = Set("aa.txt", "a/b/b2.txt", "a/file1", "b.txt", "a/aa.txt", "a/b/b.txt", "a/b/c/c.txt")
            val dest1 =
              Util.getListOfFiles(srcDecr).get.map(_.getAbsolutePath.substring(tmpDir.length + 1 + "/out".length)).toSet
            assert(src1 == dest1)
        }
    }
  }
}
