package com.github.gekomad.scalacompress

import java.io._
import java.nio.file.{Files, Paths}
import org.apache.commons.compress.archivers.ar.{ArArchiveInputStream, ArArchiveOutputStream}
import scala.util.Try
import org.apache.commons.compress.archivers.ar.ArArchiveEntry

private[scalacompress] object Ar {

  def arDecompress(src: String, dest: String): Try[DecompressionStats] = {
    val start = System.currentTimeMillis()
    for {
      filesOut <- Try {
        Util.autoClose(new ArArchiveInputStream(Files.newInputStream(Paths.get(src))))(in => Util.write(in, dest))
      }
      stat <- DecompressionStats(Compressors.AR.name, src, filesOut, System.currentTimeMillis() - start)
    } yield stat
  }

  def arCompress(src: List[(File, Int)], outDir: String): Try[CompressionStats] = {
    val start = System.currentTimeMillis()
    Try {
      val ll = src.map(d => (d._1, d._1.getName))
      Util
        .autoClose(new FileOutputStream(new File(outDir))) { out =>
          Util.autoClose(new ArArchiveOutputStream(out)) { arOutput =>
            ll.foreach { file =>
              val entry = new ArArchiveEntry(file._1, file._2)
              arOutput.putArchiveEntry(entry)
              Util.autoClose(new FileInputStream(file._1)) { ii =>
                Util.writeBuffer(ii, arOutput)
                arOutput.closeArchiveEntry()
              }
            }
          }
          outDir
        }
    }.flatMap(
      outDir =>
        CompressionStats(Compressors.AR.name, src.map(_._1.getAbsolutePath), outDir, System.currentTimeMillis() - start)
    )
  }
}
