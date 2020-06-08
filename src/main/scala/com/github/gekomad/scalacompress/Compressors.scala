package com.github.gekomad.scalacompress

import java.io.{BufferedInputStream, ByteArrayOutputStream, InputStream, _}
import java.nio.file.{Files, Paths, StandardCopyOption}
import java.util
import java.util.zip.{GZIPInputStream, GZIPOutputStream, ZipEntry, ZipFile}

import com.github.gekomad.scalacompress.Compressors.checkExtAndDestPath
import com.github.gekomad.scalacompress.Util._
import net.jpountz.lz4.{LZ4FrameInputStream, LZ4FrameOutputStream}
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.commons.compress.compressors.gzip.{GzipCompressorInputStream, GzipCompressorOutputStream}
import org.apache.commons.compress.compressors.snappy.{
  FramedSnappyCompressorInputStream,
  FramedSnappyCompressorOutputStream
}
import org.apache.commons.compress.utils.IOUtils

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
  * Compressors
  *
  * @author Giuseppe Cannella
  * @since 0.0.1
  */
object Compressors {

  private[scalacompress] case class CompressionMethod(name: String, ext: String, factory: Option[String])
  private[scalacompress] val DEFLATE = CompressionMethod("Deflate", ".deflate", Some(CompressorStreamFactory.DEFLATE))
  private[scalacompress] val BZ2     = CompressionMethod("Bz2", ".bz2", Some(CompressorStreamFactory.BZIP2))
  private[scalacompress] val GZ      = CompressionMethod("GZ", ".gz", Some(CompressorStreamFactory.GZIP))
  private[scalacompress] val PACK    = CompressionMethod("Pack", ".pack", Some(CompressorStreamFactory.PACK200))
  private[scalacompress] val XZ      = CompressionMethod("XZ", ".xz", Some(CompressorStreamFactory.XZ))
  private[scalacompress] val ZSTANDARD =
    CompressionMethod("Zstandard", ".zstd", Some(CompressorStreamFactory.ZSTANDARD))
  private[scalacompress] val LZMA   = CompressionMethod("Lzma", ".lzma", Some(CompressorStreamFactory.LZMA))
  private[scalacompress] val TAR    = CompressionMethod("Tar", ".tar", None)
  private[scalacompress] val ZIP    = CompressionMethod("Zip", ".zip", None)
  private[scalacompress] val LZ4    = CompressionMethod("Lz4", ".lz4", None)
  private[scalacompress] val SNAPPY = CompressionMethod("Snappy", ".sz", None)
  private[scalacompress] val SEVEN7 = CompressionMethod("7z", ".7z", None)
  private[scalacompress] val AR     = CompressionMethod("Ar", ".ar", None)
  private[scalacompress] val CPIO   = CompressionMethod("Cpio", ".cpio", None)

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def lzmaDecompress(src: String, dest: String): Try[DecompressionStats] =
    decompress2(src, dest, LZMA)

  /**
    *
    * @param src file to compress
    * @param dest Destination folder
    * @return Statistics on compression procedure
    */
  def lzmaCompress(src: String, dest: String): Try[CompressionStats] =
    compress2(src, dest, LZMA)

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def lz4Decompress(src: String, dest: String): Try[DecompressionStats] = {
    decompress(src, dest, LZ4)((b: BufferedInputStream, i: String) => {
      autoClose(new LZ4FrameInputStream(b))(zOut => writeStreamToFile(zOut, i))
    })
  }

  /**
    *
    * @param src file to compress
    * @param dest Destination folder
    * @return Statistics on compression procedure
    */
  def lz4Compress(src: String, dest: String): Try[CompressionStats] =
    compress(src, dest, LZ4)((b: BufferedOutputStream, i: InputStream) => {
      autoClose(new LZ4FrameOutputStream(b))(zOut => writeBuffer(i, zOut))
    })

  // Tar
  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def tarDecompress(src: String, dest: String): Try[DecompressionStats] = Tar.tarDecompress(src, dest)

  /**
    *
    * @param src File and folder to compress
    * @param dest Destination folder
    * @return Statistics on compression procedure
    */
  def tarCompress(src: List[String], dest: String): Try[CompressionStats] =
    fileAccess(src) match {
      case Some(e) => Failure(new Exception(e.mkString(",")))
      case None =>
        Try(src.flatMap(z => getListOfFiles(new File(z)).get)).flatMap(ll => Tar.tarCompress(ll, dest))
    }

//zip
  /**
    *
    * @param src File and folder to compress
    * @param dest Destination folder
    * @return Statistics on compression procedure
    */
  def zipCompress(src: List[String], dest: String): Try[CompressionStats] =
    fileAccess(src) match {
      case Some(e) => Failure(new Exception(e.mkString(",")))
      case None =>
        val a: Try[String] = new File(dest) match {
          case f if isWritableDirectory(dest) =>
            if (src.size == 1)
              Success(s"$dest/${new File(src.head).getName}${ZIP.ext}")
            else
              Success(s"$dest/${f.getName}${ZIP.ext}")
          case f if isWritableDirectory(f.getParent) =>
            Success(f.getAbsolutePath)
          case _ => Failure(new Exception(s"file error $dest"))
        }
        a.flatMap { fileOut =>
          val start = System.currentTimeMillis()
          val aa    = Try(src.flatMap(z => getListOfFiles(new File(z)).get))
          val b     = aa.flatMap(ll => Zip.zipCompress(ll, fileOut))
          b.flatMap(_ => CompressionStats(ZIP.name, src, fileOut, System.currentTimeMillis() - start))
        }
    }

  /**
    *
    * @param src String to compress
    * @param charSetName
    * @return compressed Array[Byte]
    */
  def zipString(src: String, charSetName: String = "UTF-8"): Try[Array[Byte]] = Try {
    autoClose(new ByteArrayOutputStream) { arrayOutputStream =>
      autoClose(new GZIPOutputStream(arrayOutputStream)) { outputStream =>
        outputStream.write(src.getBytes(charSetName))
      }
      arrayOutputStream.toByteArray
    }
  }

  /**
    *
    * @param src Compressed Array[Byte]
    * @param bufferSize Optional buffer size
    * @return Uncompressed Array[Byte]
    */
  def unzipString(src: Array[Byte], bufferSize: Int = 256): Try[Array[Byte]] = Try {
    val readBuffer = new Array[Byte](bufferSize)
    autoClose(new GZIPInputStream(new ByteArrayInputStream(src))) { inputStream =>
      val read = inputStream.read(readBuffer, 0, readBuffer.length)
      util.Arrays.copyOf(readBuffer, read)
    }
  }

  /**
    *
    * @param src Compressed file
    * @return ZipEntry List
    */
  def zipEntries(src: String): Try[List[ZipEntry]] = Try {
    autoClose(new ZipFile(src)) { x =>
      x.entries().asIterator().asScala.toList
    }
  }

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def zipDecompress(src: String, dest: String): Try[DecompressionStats] = {
    val start = System.currentTimeMillis()
    Try {
      autoClose(new ZipFile(src)) { zipFile =>
        val filesOut = zipFile.entries().asIterator().asScala.map { f =>
          val entry                = zipFile.getEntry(f.getName)
          val content: InputStream = zipFile.getInputStream(entry)
          val fileOut              = dest + "/" + f.getName
          new File(fileOut).getParentFile.mkdirs()
          Files.copy(content, Paths.get(fileOut), StandardCopyOption.REPLACE_EXISTING)
          fileOut
        }
        filesOut.toList
      }
    }.flatMap(filesOut => DecompressionStats(ZIP.name, src, filesOut, System.currentTimeMillis() - start))
  }

  /**
    *
    * @param src Compressed file
    * @param entryName The entry name to decompress
    * @return Uncompressed Array[Byte]
    */
  def zipDecompressEntry(src: String, entryName: String): Try[Array[Byte]] =
    Zip.zipDecompressEntry(src, entryName)

  /**
    *
    * @param src Compressed file
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def sevenZipDecompress(src: String, dest: String): Try[DecompressionStats] =
    checkSrcPath(List(src), dest) {
      val start = System.currentTimeMillis()
      SevenZip
        .sevenZipDecompress(src, dest)
        .flatMap(filesOut => DecompressionStats(SEVEN7.name, src, filesOut, System.currentTimeMillis() - start))
    }.flatten

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def arDecompress(src: String, dest: String): Try[DecompressionStats] = Ar.arDecompress(src, dest)

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def cpioDecompress(src: String, dest: String): Try[DecompressionStats] = Cpio.cpioDecompress(src, dest)

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def deflateDecompress(src: String, dest: String): Try[DecompressionStats] =
    decompress2(src, dest, DEFLATE)

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def bzip2Decompress(src: String, dest: String): Try[DecompressionStats] =
    decompress2(src, dest, BZ2)

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def gzDecompress(src: String, dest: String): Try[DecompressionStats] =
    decompress2(src, dest, GZ)

  def gzCompressStream(in: InputStream, out: OutputStream): Try[Unit] = Try {
    Util.autoClose(new GzipCompressorOutputStream(out))(zStream => writeBuffer(in, zStream))
  }

  def gzDecompressStream(in: InputStream, out: OutputStream): Try[Unit] = Try {
    Util.autoClose(new GzipCompressorInputStream(in))(zStream => writeBuffer(zStream, out))
  }

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def gzCompress(src: String, dest: String): Try[CompressionStats] =
    compress2(src, dest, GZ)

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def xzDecompress(src: String, dest: String): Try[DecompressionStats] =
    decompress2(src, dest, XZ)

  private def checkExtAndDestPath(src: String, dest: String, ext: String)(f: => Unit): Try[Unit] =
    if (!isWritableDirectory(dest)) Failure(new Exception(s"$dest is not a writable directory"))
    else {
      if (!src.toLowerCase.endsWith(ext))
        Failure(new Exception(s"$src: unknown suffix"))
      else Try(f)
    }

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def snappyDecompress(src: String, dest: String): Try[DecompressionStats] = {
    decompress(src, dest, SNAPPY)((b: BufferedInputStream, i: String) => {
      autoClose(new FramedSnappyCompressorInputStream(b))(zOut => writeStreamToFile(zOut, i))
    })
  }

  private def checkSrcPath[A](src: List[String], dest: String)(f: => A): Try[A] =
    fileAccess(src) match {
      case Some(e) => Failure(new Exception(e.mkString(",")))
      case None =>
        if (!isWritableDirectory(dest)) Failure(new Exception(s"$dest is not a writable directory"))
        else Try(f)
    }

  def writeStreamToFile(in: InputStream, dest: String): Try[Unit] = Try {
    autoClose(Files.newOutputStream(Paths.get(dest)))(out => writeBuffer(in, out))
  }

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def pack200Decompress(src: String, dest: String): Try[DecompressionStats] =
    decompress2(src, dest, PACK)

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def zStandardDecompress(src: String, dest: String): Try[DecompressionStats] =
    decompress2(src, dest, ZSTANDARD)

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def deflateCompress(src: String, dest: String): Try[CompressionStats] =
    compress2(src, dest, DEFLATE)

  /**
    *
    * @param src File to compress
    * @param dest Destination folder
    * @return Statistics on compression procedure
    */
  def bzip2Compress(src: String, dest: String): Try[CompressionStats] =
    compress2(src, dest, BZ2)

  /**
    *
    * @param src File to compress
    * @param dest Destination folder
    * @return Statistics on compression procedure
    */
  def xzCompress(src: String, dest: String): Try[CompressionStats] = compress2(src, dest, XZ)

  /**
    *
    * @param src File to compress
    * @param dest Destination folder
    * @return Statistics on compression procedure
    */
  def snappyCompress(src: String, dest: String): Try[CompressionStats] =
    compress(src, dest, SNAPPY)((b: BufferedOutputStream, i: InputStream) => {
      autoClose(new FramedSnappyCompressorOutputStream(b))(zOut => writeBuffer(i, zOut))
    })

  /**
    *
    * @param src File to compress
    * @param dest Destination folder
    * @return Statistics on compression procedure
    */
  def zStandardCompress(src: String, dest: String): Try[CompressionStats] =
    compress2(src, dest, ZSTANDARD)

  /**
    *
    * @param src File to compress
    * @param dest Destination folder
    * @return Statistics on compression procedure
    */
  def pack200Compress(src: String, dest: String): Try[CompressionStats] =
    compress2(src, dest, PACK)

  /**
    *
    * @param src File and folder to compress
    * @param dest Destination folder
    * @return Statistics on compression procedure
    */
  def sevenZipCompress(src: List[String], dest: String): Try[CompressionStats] =
    Try(src.flatMap(z => getListOfFiles(new File(z)).get)).flatMap { ll =>
      SevenZip.sevenZipCompress(ll, dest)
    }

  /**
    *
    * @param src File and folder to compress
    * @param dest Destination folder
    * @return Statistics on compression procedure
    */
  def arCompress(src: List[String], dest: String): Try[CompressionStats] =
    fileAccess(src) match {
      case Some(e) => Failure(new Exception(e.mkString(",")))
      case None =>
        Try(src.flatMap(z => getListOfFiles(new File(z)).get)).flatMap { ll =>
          Ar.arCompress(ll, dest)
        }
    }

  /**
    *
    * @param src File and folder to compress
    * @param dest Destination folder
    * @return Statistics on compression procedure
    */
  def cpioCompress(src: List[String], dest: String): Try[CompressionStats] =
    fileAccess(src) match {
      case Some(e) => Failure(new Exception(e.mkString(",")))
      case None =>
        Try(src.flatMap(z => getListOfFiles(new File(z)).get)).flatMap { ll =>
          Cpio.cpioCompress(ll, dest)
        }
    }

  private[scalacompress] def decompress(src: String, dest: String, method: CompressionMethod)(
    ff: (BufferedInputStream, String) => Unit
  ): Try[DecompressionStats] = {
    val start   = System.currentTimeMillis()
    val fileOut = dest + "/" + new File(src).getName.dropRight(method.ext.length)
    checkExtAndDestPath(src, dest, method.ext) {
      autoClose(new BufferedInputStream(Files.newInputStream(Paths.get(src)))) { x =>
        ff(x, fileOut)
      }
    }.flatMap(_ => DecompressionStats(method.name, src, List(fileOut), System.currentTimeMillis() - start))
  }

  private[scalacompress] def decompress2(src: String, dest: String, method: CompressionMethod) = {
    val start   = System.currentTimeMillis()
    val fileOut = dest + "/" + new File(src).getName.dropRight(method.ext.length)
    checkExtAndDestPath(src, dest, method.ext) {
      autoClose(Files.newInputStream(new File(src).toPath)) { is =>
        autoClose(new CompressorStreamFactory().createCompressorInputStream(method.factory.get, is)) { in =>
          IOUtils.copy(in, Files.newOutputStream(new File(fileOut).toPath))
        }
      }
    }.flatMap(_ => DecompressionStats(method.name, src, List(fileOut), System.currentTimeMillis() - start))
  }

  private[scalacompress] def compress2(src: String, dest: String, method: CompressionMethod): Try[CompressionStats] = {
    val start = System.currentTimeMillis()
    checkSrcPath(List(src), dest) {
      val zFile = s"$dest/${new File(src).getName}${method.ext}"
      autoClose(Files.newOutputStream(new File(zFile).toPath)) { out =>
        autoClose(new CompressorStreamFactory().createCompressorOutputStream(method.factory.get, out)) { cos =>
          val in: InputStream = Files.newInputStream(new File(src).toPath)
          IOUtils.copy(in, cos)
          zFile
        }
      }
    }.flatMap(zFile => CompressionStats(method.name, List(src), zFile, System.currentTimeMillis() - start))
  }

  private[scalacompress] def compress(src: String, dest: String, method: CompressionMethod)(
    compressor: (BufferedOutputStream, InputStream) => Unit
  ): Try[CompressionStats] = {
    val start = System.currentTimeMillis()
    checkSrcPath(List(src), dest) {
      val zFile = s"$dest/${new File(src).getName}${method.ext}"
      autoClose(Files.newInputStream(Paths.get(src))) { in =>
        autoClose(Files.newOutputStream(Paths.get(zFile))) { fout =>
          autoClose(new BufferedOutputStream(fout))(out => compressor(out, in))
          zFile
        }
      }
    }.flatMap(zFile => CompressionStats(method.name, List(src), zFile, System.currentTimeMillis() - start))
  }

  /**
    *
    * {{{
    * val gzStream         = GzCompressBuffer(new FileOutputStream("/tmp/file.gz"))
    * val foo: Array[Byte] = "foo".getBytes(StandardCharsets.UTF_8)
    * val bar: Array[Byte] = "bar".getBytes(StandardCharsets.UTF_8)
    *
    * val c1: Try[Unit] = gzStream.compressBuffer(foo)
    * val c2: Try[Unit] = gzStream.compressBuffer(bar)
    * val cl: Try[Unit] = gzStream.close()
    * }}}
    *
    * @param out
    */
  case class GzCompressBuffer(out: OutputStream) {
    private val zStream = new GzipCompressorOutputStream(out)

    def compressBuffer(buffer: Array[Byte]): Try[Unit] = Try(zStream.write(buffer, 0, buffer.length))

    def close(): Try[Unit] = Try(zStream.close())
  }

  /**
    *{{{
    * val gzStream     = GzDecompressInBuffer(new FileInputStream("/tmp/file.gz"))
    * val buffer       = new Array[Byte](2)
    * val decompressed = new StringBuilder
    *
    * @tailrec
    * def readBuffer(): Unit = {
    *   gzStream.readInBuffer(buffer) match {
    *     case Failure(exception) => exception.printStackTrace
    *     case Success(bytesRead) =>
    *       if (bytesRead != -1) {
    *         decompressed.append(new String(buffer, StandardCharsets.UTF_8))
    *         readBuffer()
    *       } else {
    *         println
    *         gzStream.close()
    *       }
    *   }
    * }
    * readBuffer()
    * val cl: Try[Unit] = gzStream.close()
    * assert(decompressed.toString == "foobar")
    * }}}
    *
    * @param in
    */
  case class GzDecompressInBuffer(in: InputStream) {
    private val zStream = new GzipCompressorInputStream(in)
    def readInBuffer(buffer: Array[Byte]): Try[Int] =
      Try(zStream.read(buffer)) match {
        case Success(read)      => Success(read)
        case Failure(exception) => Failure(exception)
      }
    def close(): Try[Unit] = Try(zStream.close())
  }

}
