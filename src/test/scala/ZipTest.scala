import java.io.File

import com.github.gekomad.scalacompress.Compressors._
import org.scalatest.funsuite.AnyFunSuite
import scala.util.{Failure, Success}
import com.github.gekomad.scalacompress.Util.SEP

class ZipTest extends AnyFunSuite {

  test("zip folder into destination folder") {
    val tmpDir = Util.createTmpDir(suiteName)

    val dest   = s"$tmpDir/k"
    val gzFile = s"$dest/a.zip"
    val src    = getClass.getResource("a").getPath
    new File(dest).mkdirs()
    zipCompress(List(src), dest) match {
      case Failure(e) => assert(false, e)
      case Success(statistics) =>
        println("-----------\n" + Util.toString(statistics) + "-----------\n")

    }
    val a1: Set[String] = zipEntries(gzFile) match {
      case Failure(exception) => throw new Exception(exception)
      case Success(value)     => value.map(a => a.getName).toSet
    }
    assert(
      a1 == Set(s"a/file1", s"a/empty/", s"a/aa.txt", s"a/b/b.txt", s"a/b/b2.txt", s"a/b/c/c.txt") ||
        a1 == Set(s"a/file1", s"a\\empty/", s"a/aa.txt", s"a/b/b.txt", s"a/b/b2.txt", s"a/b/c/c.txt")
    )

    val array: Array[Byte] = zipDecompressEntry(gzFile, "a/b/c/c.txt").get
    assert(new String(array, "UTF-8") == "c")
    assert(zipDecompress(gzFile, s"$tmpDir/zipdir").isSuccess)

    val l  = Util.getListOfFiles(src).get
    val l2 = Util.getListOfFiles(s"$tmpDir/zipdir").get
    assert(l.size == l2.size)
    l.filter(_.isFile) zip l2.filter(_.isFile) foreach { r =>
      val (f1, f2) = r
      assert(f1.getName == f2.getName)
      assert(scala.io.Source.fromFile(f1).mkString == scala.io.Source.fromFile(f2).mkString)
    }
  }

  test("zip folder into a zipped file") {
    val tmpDir = Util.createTmpDir(suiteName)

    val dest = s"$tmpDir/file.zip"

    val src = getClass.getResource("a").getPath

    assert(zipCompress(List(src), dest).isSuccess)
    val a1 = zipEntries(dest) match {
      case Failure(exception) => assert(false, exception)
      case Success(value)     => value.map(a => a.getName).toSet
    }
    assert(
      a1 == Set("a/file1", "a/empty/", "a/aa.txt", "a/b/b.txt", "a/b/b2.txt", "a/b/c/c.txt") ||
        a1 == Set("a/file1", "a\\empty/", "a/aa.txt", "a/b/b.txt", "a/b/b2.txt", "a/b/c/c.txt")
    )

    val array = zipDecompressEntry(dest, "a/b/c/c.txt").get
    assert(new String(array, "UTF-8") == "c")
    assert(zipDecompress(dest, s"$tmpDir/zipdir").isSuccess)

    val l  = Util.getListOfFiles(src).get
    val l2 = Util.getListOfFiles(s"$tmpDir/zipdir").get
    assert(l.size == l2.size)
    l.filter(_.isFile) zip l2.filter(_.isFile) foreach { r =>
      val (f1, f2) = r
      assert(f1.getName == f2.getName)
      assert(scala.io.Source.fromFile(f1).mkString == scala.io.Source.fromFile(f2).mkString)
    }
  }

  test("zip single file with destination file name") {
    val tmpDir = Util.createTmpDir(suiteName)

    val src  = getClass.getResource("a/aa.txt").getPath
    val dest = s"$tmpDir/b.zip"
    assert(zipCompress(List(src), dest).isSuccess)
    val a1 = zipEntries(dest) match {
      case Success(value) => value.map(a => a.getName)
      case Failure(e)     => throw new Exception(e)
    }
    assert(a1 == List("aa.txt"))
    assert(zipDecompress(dest, s"$tmpDir/zipdir").isSuccess)

    val l  = Util.getListOfFiles(src).get
    val l2 = Util.getListOfFiles(s"$tmpDir/zipdir").get
    assert(l.size == l2.size)
    l.filter(_.isFile) zip l2.filter(_.isFile) foreach { r =>
      val (f1, f2) = r
      assert(f1.getName == f2.getName)
      assert(scala.io.Source.fromFile(f1).mkString == scala.io.Source.fromFile(f2).mkString)
    }
  }

  test("zip single file with destination folder") {
    val tmpDir = Util.createTmpDir(suiteName)
    val file   = "aa.txt"
    val src    = getClass.getResource(file).getPath
    val dest   = s"$tmpDir/dest"
    new File(dest).mkdirs()
    zipCompress(List(src), dest) match {
      case Failure(e) => assert(false, e)
      case Success(statistics) =>
        println("-----------\n" + Util.toString(statistics) + "-----------\n")
    }
    val a1 = zipEntries(dest + s"/$file.zip") match {
      case Success(value) => value.map(_.getName)
      case Failure(e)     => throw new Exception(e)
    }
    assert(a1 == List(file))
    zipDecompress(dest + s"/$file.zip", s"$tmpDir/zipdir") match {
      case Failure(e) => assert(false, e)
      case Success(statistics) =>
        println("-----------\n" + Util.toString(statistics) + "-----------\n")
    }

    val l  = Util.getListOfFiles(src).get
    val l2 = Util.getListOfFiles(s"$tmpDir/zipdir").get
    assert(l.size == l2.size)
    l.filter(_.isFile) zip l2.filter(_.isFile) foreach { r =>
      val (f1, f2) = r
      assert(f1.getName == f2.getName)
      assert(scala.io.Source.fromFile(f1).mkString == scala.io.Source.fromFile(f2).mkString)
    }
  }

  test("zip duplicate file") {
    val tmpDir = Util.createTmpDir(suiteName)

    val src = List(
      getClass.getResource("aa.txt").getPath,
      getClass.getResource("aa.txt").getPath,
      getClass.getResource("a").getPath
    )

    val dest = s"$tmpDir/a.zip"
    zipCompress(src, dest) match {
      case Failure(exception) if exception.getMessage == "Duplicate files: aa.txt" => assert(true)
      case _                                                                       => assert(false)
    }
  }

  test("zip multiple file and folder with zip name") {
    val tmpDir = Util.createTmpDir(suiteName)

    val src = List(
      getClass.getResource("aa.txt").getPath,
      getClass.getResource("a").getPath,
      getClass.getResource("a/b/b.txt").getPath
    )

    val dest = s"$tmpDir/foo.zip"

    val a = zipCompress(src, dest)
    assert(a.isSuccess)
    a match {
      case Failure(e) => throw new Exception(e)
      case Success(statistics) =>
        println("-----------\n" + Util.toString(statistics) + "-----------\n")

    }
    val a1 = zipEntries(dest) match {
      case Success(value) => value.map(a => new File(a.getName).getPath).toSet
      case Failure(e)     => throw new Exception(e)
    }
    assert(
      a1 == Set("aa.txt", "a/empty", "b.txt", "a/file1", "a/aa.txt", "a/b/b.txt", "a/b/b2.txt", "a/b/c/c.txt") || a1 == Set(
        "aa.txt",
        "a\\empty",
        "b.txt",
        "a\\file1",
        "a\\aa.txt",
        "a\\b\\b.txt",
        "a\\b\\b2.txt",
        "a\\b\\c\\c.txt"
      )
    )
    assert(zipDecompress(dest, s"$tmpDir/zipdir").isSuccess)
  }

  test("zip multiple file and folder with folder name") {
    val tmpDir = Util.createTmpDir(suiteName)

    val src = List(
      getClass.getResource("aa.txt").getPath,
      getClass.getResource("a").getPath,
      getClass.getResource("a/b/b.txt").getPath
    )

    val dest = s"$tmpDir/xxx"
    new File(dest).mkdirs()
    zipCompress(src, dest) match {
      case Failure(e) => assert(false, e)
      case Success(statistics) =>
        println("-----------\n" + Util.toString(statistics) + "-----------\n")

    }
    val zippedFile = s"$tmpDir/xxx/xxx.zip"
    val a1 = zipEntries(zippedFile) match {
      case Success(value) => value.map(a => new File(a.getName).getPath).toSet
      case Failure(e)     => throw new Exception(e)
    }
    assert(
      a1 == Set(
        "aa.txt",
        s"a${SEP}empty",
        "b.txt",
        s"a${SEP}file1",
        s"a${SEP}aa.txt",
        s"a${SEP}b${SEP}b.txt",
        s"a${SEP}b${SEP}b2.txt",
        s"a${SEP}b${SEP}c${SEP}c.txt"
      )
    )
    assert(zipDecompress(zippedFile, s"$tmpDir/zipdir").isSuccess)
  }

  test("zip string") {
    val s = "fooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo"
    val a = for {
      c <- zipString(s)
      d <- unzipString(c)
    } yield (s, d, c)

    a match {
      case Failure(value) =>
        assert(false, value)
      case Success(a) =>
        assert(a._1 == new String(a._2, "UTF-8"))
        assert(a._3.length < s.length)
    }
  }
}
