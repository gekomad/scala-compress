package com.github.gekomad.scalacompress

import java.io._
import java.util
import java.util.zip.ZipFile
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.utils.IOUtils
import scala.util.{Failure, Try}

private[scalacompress] object Zip {

  def zipCompress(in: List[(File, Int)], out: String): Try[Unit] = {
    val ll = in.map(d => (d._1, d._1.getAbsolutePath.substring(d._2 + 1)))
    Util.duplicate(ll.map(_._2)) match {
      case x :: xs =>
        Failure(new Exception("Duplicate files: " + (x :: xs).mkString(",")))
      case Nil =>
        Util.autoClose(new FileOutputStream(out)) { o =>
          Util.autoClose(
            new ArchiveStreamFactory()
              .createArchiveOutputStream(org.apache.commons.compress.archivers.ArchiveStreamFactory.ZIP, o)
          ) { archive =>
            Try(ll.foreach { file =>
              archive.putArchiveEntry(new ZipArchiveEntry(file._1, file._2))
              Util.autoClose(new FileInputStream(file._1)) { x =>
                IOUtils.copy(x, archive)
                archive.closeArchiveEntry()
              }
            })
          }
        }
    }
  }

  def zipDecompressEntry(zipFileName: String, entryName: String, bufferSize: Int = 4096): Try[Array[Byte]] = Try {
    Util.autoClose(new ZipFile(zipFileName)) { zipFile =>
      val entry                    = zipFile.getEntry(entryName)
      val inputStream: InputStream = zipFile.getInputStream(entry)
      val readBuffer               = new Array[Byte](bufferSize)
      val read                     = inputStream.read(readBuffer, 0, readBuffer.length)
      inputStream.close()
      util.Arrays.copyOf(readBuffer, read)
    }
  }
}
