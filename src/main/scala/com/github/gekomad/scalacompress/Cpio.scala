package com.github.gekomad.scalacompress

import java.io._
import java.nio.file.{Files, Paths}
import org.apache.commons.compress.archivers.cpio.{CpioArchiveEntry, CpioArchiveInputStream, CpioArchiveOutputStream}
import scala.util.Try

private[scalacompress] object Cpio {
  def cpioDecompress(src: String, dest: String): Try[DecompressionStats] = {
    val start = System.currentTimeMillis()
    for {
      fileOut <- Try(
        Util.autoClose(new CpioArchiveInputStream(Files.newInputStream(Paths.get(src))))(in => Util.write(in, dest))
      )
      stat <- DecompressionStats(Compressors.CPIO.name, src, fileOut, System.currentTimeMillis() - start)
    } yield stat
  }

  def cpioCompress(src: List[(File, Int)], outDir: String): Try[CompressionStats] = {
    val start = System.currentTimeMillis()
    Try {
      val ll = src.map(d => (d._1, d._1.getName))
      Util
        .autoClose(new FileOutputStream(new File(outDir))) { out =>
          Util.autoClose(new CpioArchiveOutputStream(out)) { arOutput =>
            ll.foreach { file =>
              val entry = new CpioArchiveEntry(file._1, file._2)
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
        CompressionStats(
          Compressors.CPIO.name,
          src.map(_._1.getAbsolutePath),
          outDir,
          System.currentTimeMillis() - start
      )
    )
  }
}
