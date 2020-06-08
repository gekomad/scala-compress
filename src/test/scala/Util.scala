import java.io.File
import java.util.UUID

import com.github.gekomad.scalacompress.CompressionStats
import com.github.gekomad.scalacompress.DecompressionStats
import com.github.gekomad.scalacompress.Util.getParent

import scala.annotation.tailrec
import scala.util.Try

object Util {

  def formatStatistics(s: CompressionStats, millDecompress: Long): String = {
    val method       = s.method
    val fileSize     = s.sizeOut
    val ratio        = s.sizeIn.toDouble / s.sizeOut.toDouble
    val millCompress = Math.round(s.millSeconds.toFloat)
    val mbComprSec   = Math.round(s.sizeIn * 0.001 / s.millSeconds)
    val mbDecomprSec = Math.round(s.sizeIn * 0.001 / millDecompress)
    f"""|$method|$fileSize|$ratio%1.2f|$millCompress|$mbComprSec|$millDecompress|$mbDecomprSec|"""
  }

  def toString(s: CompressionStats): String =
    s"""Compression method: ${s.method}
       |Input file: ${s.fileIn.mkString(",")}
       |Input file size: ${s.sizeIn}
       |Output file: ${s.fileOut}
       |Output file size: ${s.sizeOut}
       |Compression ratio: ${s.compressionRatio}
       |Milliseconds: ${s.millSeconds}
       |MB per second: ${s.mbPerSecond}
       |""".stripMargin

  def toString(s: DecompressionStats): String =
    s"""Decompression method: ${s.method}
       |Input file: ${s.fileIn}
       |Input file size: ${s.sizeIn}
       |Output file: ${s.fileOut}
       |Output file size: ${s.sizeOut}
       |Compression ratio: ${s.compressionRatio}
       |Milliseconds: ${s.millSeconds}
       |MB per second: ${s.mbPerSecond}
       |""".stripMargin

  def getListOfFiles(dir: String): Try[List[File]] =
    com.github.gekomad.scalacompress.Util.getListOfFiles(new File(dir)).map(_.map(_._1))

  def commonPath(ll: List[File]): String = {

    @tailrec
    def _commonPath(l: List[File], parent: String): String = {
      val c = l.forall(x => x.getAbsolutePath.startsWith(parent))
      if (!c)
        _commonPath(l, getParent(new File(parent)))
      else
        parent
    }
    _commonPath(ll, getParent(ll.head))
  }

  def createTmpDir(name: String): String = {
    val tmp = System.getProperty("java.io.tmpdir")
    java.nio.file.Files
      .createDirectories(new File(s"$tmp/scala-compress/$name-${UUID.randomUUID().toString}").toPath)
      .toString
  }
}
