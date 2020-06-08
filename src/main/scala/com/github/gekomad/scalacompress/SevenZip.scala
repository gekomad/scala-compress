package com.github.gekomad.scalacompress

import java.io._
import java.nio.file.{Files, Paths}
import com.github.gekomad.scalacompress.Util.autoClose
import org.apache.commons.compress.archivers.sevenz.{SevenZFile, SevenZOutputFile}
import scala.annotation.tailrec
import scala.util.{Failure, Try}

private[scalacompress] object SevenZip {

  def sevenZipDecompress(compressed: String, decompressed: String, bufferSize: Int = 4096): Try[List[String]] = Try {

    def inout(in: SevenZFile, out: OutputStream): Unit = {
      val buffer = new Array[Byte](bufferSize)
      @tailrec
      def go(): Unit = {
        val c = in.read(buffer)
        if (c > 0) {
          out.write(buffer, 0, c)
          go()
        }
      }
      go()
    }

    @tailrec
    def write(sevenZFile: SevenZFile, outFiles: List[String]): List[String] = {
      val entry = sevenZFile.getNextEntry
      if (entry != null) {
        val fileOut  = s"$decompressed/${entry.getName}"
        val confFile = new File(fileOut)
        confFile.getParentFile.mkdirs()
        autoClose(Files.newOutputStream(Paths.get(fileOut)))(out => inout(sevenZFile, out))
        write(sevenZFile, fileOut :: outFiles)
      } else outFiles
    }
    autoClose(new SevenZFile(new File(compressed)))(sevenZFile => write(sevenZFile, Nil))
  }

  def sevenZipCompress(
    fileToArchive: List[(File, Int)],
    fileOut2: String,
    bufferSize: Int = 4096
  ): Try[CompressionStats] = {
    def write(ii: FileInputStream, out: SevenZOutputFile): Unit = {
      val chunk = new Array[Byte](bufferSize)
      @tailrec
      def go(): Unit = {
        val contentOfEntry = ii.read(chunk)
        if (contentOfEntry != -1) {
          out.write(chunk, 0, contentOfEntry)
          go()
        } else ()
      }
      go()
    }

    val ll    = fileToArchive.map(d => (d._1, d._1.getAbsolutePath.substring(d._2 + 1)))
    val start = System.currentTimeMillis()
    Util.duplicate(ll.map(_._2)) match {
      case x :: xs => Failure(new Exception("Duplicate files: " + (x :: xs).mkString(",")))
      case Nil =>
        Try {
          autoClose(new SevenZOutputFile(new File(fileOut2))) { x =>
            autoClose(x) { sevenZOutput =>
              ll.foreach { fileToCompress =>
                val entry = sevenZOutput.createArchiveEntry(fileToCompress._1, fileToCompress._2)
                sevenZOutput.putArchiveEntry(entry)
                autoClose(new FileInputStream(fileToCompress._1)) { ii =>
                  write(ii, sevenZOutput)
                  sevenZOutput.closeArchiveEntry()
                }
              }
            }
          }
        }.flatMap(
          _ =>
            CompressionStats(
              Compressors.SEVEN7.name,
              fileToArchive.map(_._1.getAbsolutePath),
              fileOut2,
              System.currentTimeMillis() - start
          )
        )
    }
  }
}
