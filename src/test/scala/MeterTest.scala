import java.io.File
import com.github.gekomad.scalacompress.Compressors._
import org.scalatest.funsuite.AnyFunSuite

class MeterTest extends AnyFunSuite {

  ignore("meter") {

    val file = "blob.scala"
    val src  = getClass.getResource(file).getPath

    println(
      "|Compression method|Output file size|Compression ratio|Milliseconds to compress|MB per second compression|Milliseconds to decompress|MB per second decompression|"
    )

    {
      val tmpDir     = Util.createTmpDir(suiteName)
      val dest       = s"$tmpDir/a.ar"
      val srcDecr    = s"$tmpDir/adec"
      val statistics = arCompress(List(src), dest).get
      val deco       = arDecompress(dest, srcDecr).get
      println(Util.formatStatistics(statistics, deco.millSeconds))

    }

    {
      val tmpDir  = Util.createTmpDir(suiteName)
      val srcDecr = s"$tmpDir/dest"
      val dest    = s"$tmpDir"
      new File(srcDecr).mkdirs()
      val statistics = bzip2Compress(src, dest).get
      val deco       = bzip2Decompress(dest + s"/$file.bz2", srcDecr).get
      println(Util.formatStatistics(statistics, deco.millSeconds))
    }
    {
      val tmpDir  = Util.createTmpDir(suiteName)
      val dest    = s"$tmpDir"
      val srcDecr = s"$tmpDir/adec"
      new File(srcDecr).mkdirs()
      val statistics = deflateCompress(src, dest).get

      val deco = deflateDecompress(dest + s"/$file.deflate", srcDecr).get
      println(Util.formatStatistics(statistics, deco.millSeconds))
    }
    {
      val tmpDir  = Util.createTmpDir(suiteName)
      val dest    = s"$tmpDir"
      val srcDecr = s"$tmpDir/adec"
      new File(srcDecr).mkdirs()
      val statistics = gzCompress(src, dest).get

      val deco = gzDecompress(dest + s"/$file.gz", srcDecr).get
      println(Util.formatStatistics(statistics, deco.millSeconds))
    }
    {
      val tmpDir  = Util.createTmpDir(suiteName)
      val srcDecr = s"$tmpDir/a2"
      val dest    = s"$tmpDir"
      new File(srcDecr).mkdirs()
      val statistics = lz4Compress(src, dest).get

      val deco = lz4Decompress(dest + s"/$file.lz4", srcDecr).get
      println(Util.formatStatistics(statistics, deco.millSeconds))
    }
    {
      val tmpDir  = Util.createTmpDir(suiteName)
      val srcDecr = s"$tmpDir/a2"
      val dest    = s"$tmpDir"
      new File(srcDecr).mkdirs()
      val statistics = lzmaCompress(src, dest).get

      val deco = lzmaDecompress(dest + s"/$file.lzma", srcDecr).get
      println(Util.formatStatistics(statistics, deco.millSeconds))
    }

    {
      val tmpDir = Util.createTmpDir(suiteName)

      val dest    = s"$tmpDir/tar1.cpio"
      val srcDecr = s"$tmpDir/tar1.dec"
      new File(srcDecr).mkdirs()
      val statistics = cpioCompress(List(src), dest).get

      val deco = cpioDecompress(dest, srcDecr).get
      println(Util.formatStatistics(statistics, deco.millSeconds))
    }

    {
      val tmpDir = Util.createTmpDir(suiteName)

      val srcDecr = s"$tmpDir/adec"
      val dest    = s"$tmpDir"
      new File(srcDecr).mkdirs()
      val statistics = snappyCompress(src, dest).get

      val deco = snappyDecompress(dest + s"/$file.sz", srcDecr).get
      println(Util.formatStatistics(statistics, deco.millSeconds))
    }
    {
      val tmpDir = Util.createTmpDir(suiteName)

      val dest    = s"$tmpDir/tar1.tar"
      val srcDecr = s"$tmpDir/tar1.dec"
      new File(srcDecr).mkdirs()
      val statistics = tarCompress(List(src), dest).get

      val deco = tarDecompress(dest, srcDecr).get
      println(Util.formatStatistics(statistics, deco.millSeconds))
    }
    {
      val tmpDir = Util.createTmpDir(suiteName)

      val srcDecr = s"$tmpDir/adec"
      val dest    = s"$tmpDir"
      new File(srcDecr).mkdirs()
      val statistics = xzCompress(src, dest).get

      val deco = xzDecompress(dest + s"/$file.xz", srcDecr).get
      println(Util.formatStatistics(statistics, deco.millSeconds))
    }

    {
      val tmpDir = Util.createTmpDir(suiteName)
      val dest   = s"$tmpDir/dest"
      new File(dest).mkdirs()
      val statistics = zipCompress(List(src), dest).get

      val deco = zipDecompress(dest + s"/$file.zip", s"$tmpDir/zipdir").get
      println(Util.formatStatistics(statistics, deco.millSeconds))
    }

    {
      val tmpDir = Util.createTmpDir(suiteName)

      val srcDecr = s"$tmpDir/adec"
      val dest    = s"$tmpDir"
      new File(srcDecr).mkdirs()
      val statistics = zStandardCompress(src, dest).get

      val deco = zStandardDecompress(dest + s"/$file.zstd", srcDecr).get
      println(Util.formatStatistics(statistics, deco.millSeconds))
    }

    {
      val tmpDir = Util.createTmpDir(suiteName)

      val srcDecr = s"$tmpDir/out"
      val dest    = s"$tmpDir/a.7z"
      new File(srcDecr).mkdirs()
      val statistics = sevenZipCompress(List(src), dest).get

      val deco = sevenZipDecompress(dest, srcDecr).get
      println(Util.formatStatistics(statistics, deco.millSeconds))
    }

  }
}
