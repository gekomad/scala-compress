package com.github.gekomad.scalacompress

import java.io._
import java.nio.file.{Files, Paths}
import com.github.gekomad.scalacompress.Util.autoClose
import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveInputStream}
import org.apache.commons.compress.archivers.{ArchiveOutputStream, ArchiveStreamFactory}
import org.apache.commons.compress.utils.IOUtils
import scala.util.{Failure, Try}

private[scalacompress] object Tar {

  def tarFile(file: (File, String), archive: ArchiveOutputStream): Try[Unit] = Try {
    archive.putArchiveEntry(new TarArchiveEntry(file._1, file._2))
    autoClose(new FileInputStream(file._1)) { x =>
      IOUtils.copy(x, archive)
      archive.closeArchiveEntry()
    }
  }

  def tarDecompress(src: String, dest: String): Try[DecompressionStats] = {
    val start = System.currentTimeMillis()
    for {
      filesOut <- Try {
        autoClose(new TarArchiveInputStream(Files.newInputStream(Paths.get(src))))(in => Util.write(in, dest))
      }
      stat <- DecompressionStats(Compressors.TAR.name, src, filesOut, System.currentTimeMillis() - start)
    } yield stat
  }

  def tarCompress(l: List[(File, Int)], out: String): Try[CompressionStats] = {
    val start = System.currentTimeMillis()

    {
      autoClose(new FileOutputStream(out)) { x =>
        autoClose(
          new ArchiveStreamFactory()
            .createArchiveOutputStream(org.apache.commons.compress.archivers.ArchiveStreamFactory.TAR, x)
        ) { archive =>
          val ll = l.map(d => (d._1, d._1.getAbsolutePath.substring(d._2 + 1)))
          Util.duplicate(ll.map(_._2)) match {
            case x :: xs =>
              Failure(new Exception("Duplicate files: " + (x :: xs).mkString(",")))
            case Nil =>
              Try {
                ll.foreach(file => tarFile(file, archive))
              }
          }
        }
      }
    }.flatMap(
      _ => CompressionStats(Compressors.TAR.name, l.map(_._1.getAbsolutePath), out, System.currentTimeMillis() - start)
    )
  }
}
