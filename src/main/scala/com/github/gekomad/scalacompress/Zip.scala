package com.github.gekomad.scalacompress

import java.io.{InputStream, _}
import java.nio.file.{Files, Paths, StandardCopyOption}
import java.util
import java.util.zip.ZipFile
import com.github.gekomad.scalacompress.Compressors.zipMethod
import com.github.gekomad.scalacompress.Util._
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import scala.util.{Failure, Success, Try}

private[scalacompress] object Zip {

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

  def zipCompress(src: List[String], dest: String): Try[CompressionStats] =
    fileAccess(src) match {
      case Some(e) => Failure(new Exception(e.mkString(",")))
      case None =>
        val a: Try[String] = new File(dest) match {
          case f if isWritableDirectory(dest) =>
            if (src.size == 1)
              Success(s"$dest$SEP${new File(src.head).getName}${zipMethod.ext}")
            else
              Success(s"$dest$SEP${f.getName}${zipMethod.ext}")
          case f if isWritableDirectory(f.getParent) =>
            Success(f.getAbsolutePath)
          case _ => Failure(new Exception(s"file error $dest"))
        }
        a.flatMap { fileOut =>
          val start = System.currentTimeMillis()
          val b = Try(src.flatMap(z => getListOfFiles(new File(z)).get)).flatMap { ll =>
            def f(out: OutputStream) =
              new ArchiveStreamFactory()
                .createArchiveOutputStream(org.apache.commons.compress.archivers.ArchiveStreamFactory.ZIP, out)
            def g(f: File, s: String) = new ZipArchiveEntry(f, s)

            Compressors.compress3(Compressors.zipMethod.name.toString, ll, fileOut)(f)(g)
          }
          b.flatMap(_ => CompressionStats(zipMethod.name.toString, src, fileOut, System.currentTimeMillis() - start))
        }
    }

  def zipDecompress(src: String, dest: String): Try[DecompressionStats] = {
    val start = System.currentTimeMillis()
    Try {
      autoClose(new ZipFile(src)) { zipFile =>
        val filesOut = javaIteratorToList(zipFile.entries().asIterator()).map { f =>
          val entry                = zipFile.getEntry(f.getName)
          val content: InputStream = zipFile.getInputStream(entry)
          val fileOut              = s"$dest$SEP${f.getName}"
          val fo                   = new File(fileOut)
          Files.createDirectories(fo.getParentFile.toPath)
          if (entry.isDirectory) Files.createDirectories(fo.toPath)
          else Files.copy(content, Paths.get(fileOut), StandardCopyOption.REPLACE_EXISTING)
          fileOut
        }
        filesOut.toList
      }
    }.flatMap(
      filesOut => DecompressionStats(zipMethod.name.toString, src, filesOut, System.currentTimeMillis() - start)
    )
  }
}
