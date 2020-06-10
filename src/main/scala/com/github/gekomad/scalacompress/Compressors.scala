package com.github.gekomad.scalacompress

import java.io.{BufferedInputStream, ByteArrayOutputStream, InputStream, _}
import java.nio.file.{Files, Paths}
import java.util
import java.util.zip.{GZIPInputStream, GZIPOutputStream, ZipEntry, ZipFile}

import com.github.gekomad.scalacompress.Util._
import net.jpountz.lz4.{LZ4FrameInputStream, LZ4FrameOutputStream}
import org.apache.commons.compress.archivers.ar.{ArArchiveEntry, ArArchiveInputStream, ArArchiveOutputStream}
import org.apache.commons.compress.archivers.cpio.{CpioArchiveEntry, CpioArchiveInputStream, CpioArchiveOutputStream}
import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveInputStream, TarArchiveOutputStream}
import org.apache.commons.compress.archivers.{ArchiveEntry, ArchiveInputStream, ArchiveOutputStream}
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
  private[scalacompress] val DEFLATE   = CompressionMethod("Deflate", ".deflate", Some(CompressorStreamFactory.DEFLATE))
  private[scalacompress] val BZ2       = CompressionMethod("Bz2", ".bz2", Some(CompressorStreamFactory.BZIP2))
  private[scalacompress] val GZ        = CompressionMethod("GZ", ".gz", Some(CompressorStreamFactory.GZIP))
  private[scalacompress] val PACK      = CompressionMethod("Pack", ".pack", Some(CompressorStreamFactory.PACK200))
  private[scalacompress] val XZ        = CompressionMethod("XZ", ".xz", Some(CompressorStreamFactory.XZ))
  private[scalacompress] val ZSTANDARD = CompressionMethod("Zstandard", ".zst", Some(CompressorStreamFactory.ZSTANDARD))
  private[scalacompress] val LZMA      = CompressionMethod("Lzma", ".lzma", Some(CompressorStreamFactory.LZMA))
  private[scalacompress] val TAR       = CompressionMethod("Tar", ".tar", None)
  private[scalacompress] val ZIP       = CompressionMethod("Zip", ".zip", None)
  private[scalacompress] val LZ4       = CompressionMethod("Lz4", ".lz4", None)
  private[scalacompress] val SNAPPY    = CompressionMethod("Snappy", ".sz", None)
  private[scalacompress] val SEVEN7    = CompressionMethod("7z", ".7z", None)
  private[scalacompress] val AR        = CompressionMethod("Ar", ".ar", None)
  private[scalacompress] val CPIO      = CompressionMethod("Cpio", ".cpio", None)

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
      autoClose(new LZ4FrameOutputStream(b))(zOut => IOUtils.copy(i, zOut))
    })

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @param entries If != None extract only these entries
    * @return Statistics on decompression procedure
    */
  def tarDecompress(src: String, dest: String, entries: Option[List[String]] = None): Try[DecompressionStats] =
    decompress3(Compressors.TAR.name, src, dest, entries)(
      (src: String) => new TarArchiveInputStream(Files.newInputStream(Paths.get(src)))
    )

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
        def f(out: OutputStream) = {
          val ar = new TarArchiveOutputStream(out)
          ar.setLongFileMode(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_POSIX)
          ar
        }
        def g(f: File, s: String) = new TarArchiveEntry(f, s)

        Try(src.flatMap(z => getListOfFiles(new File(z)).get))
          .flatMap(ll => compress3(Compressors.TAR.name, ll, dest)(f)(g))
    }

  /**
    *
    * @param src File and folder to compress
    * @param dest Destination folder
    * @return Statistics on compression procedure
    */
  def zipCompress(src: List[String], dest: String): Try[CompressionStats] = Zip.zipCompress(src, dest)

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
    autoClose(new ZipFile(src))(_.entries().asIterator().asScala.toList)
  }

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def zipDecompress(src: String, dest: String): Try[DecompressionStats] = Zip.zipDecompress(src, dest)

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
  def sevenZipDecompress(src: String, dest: String, entries: Option[List[String]] = None): Try[DecompressionStats] =
    checkSrcPath(List(src), dest) {
      val start = System.currentTimeMillis()
      SevenZip
        .sevenZipDecompress(src, dest, entries)
        .flatMap(filesOut => DecompressionStats(SEVEN7.name, src, filesOut, System.currentTimeMillis() - start))
    }.flatten

  private def decompress3(name: String, src: String, dest: String, entries: Option[List[String]])(
    f: String => ArchiveInputStream
  ): Try[DecompressionStats] = {
    val start = System.currentTimeMillis()
    for {
      filesOut <- Try(Util.autoClose(f(src))(in => Util.extractEntries(in, dest, entries)))
      stat     <- DecompressionStats(name, src, filesOut, System.currentTimeMillis() - start)
    } yield stat
  }

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @param entries If != None extract only these entries
    * @return Statistics on decompression procedure
    */
  def arDecompress(src: String, dest: String, entries: Option[List[String]] = None): Try[DecompressionStats] =
    decompress3(Compressors.AR.name, src, dest, entries)(
      (src: String) => new ArArchiveInputStream(Files.newInputStream(Paths.get(src)))
    )

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @param entries If != None extract only these entries
    * @return Statistics on decompression procedure
    */
  def cpioDecompress(src: String, dest: String, entries: Option[List[String]] = None): Try[DecompressionStats] =
    decompress3(Compressors.CPIO.name, src, dest, entries)(
      (src: String) => new CpioArchiveInputStream(Files.newInputStream(Paths.get(src)))
    )

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
    Util.autoClose(new GzipCompressorOutputStream(out))(zStream => IOUtils.copy(in, zStream))
  }

  def gzDecompressStream(in: InputStream, out: OutputStream): Try[Unit] = Try {
    Util.autoClose(new GzipCompressorInputStream(in))(zStream => IOUtils.copy(zStream, out))
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
    autoClose(Files.newOutputStream(Paths.get(dest)))(out => IOUtils.copy(in, out))
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
      autoClose(new FramedSnappyCompressorOutputStream(b))(zOut => IOUtils.copy(i, zOut))
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
          def f(out: OutputStream)  = new ArArchiveOutputStream(out)
          def g(f: File, s: String) = new ArArchiveEntry(f, s)

          compress3(Compressors.AR.name, ll, dest)(f)(g)
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
          def f(out: OutputStream)  = new CpioArchiveOutputStream(out)
          def g(f: File, s: String) = new CpioArchiveEntry(f, s)
          compress3(Compressors.CPIO.name, ll, dest)(f)(g)
        }
    }

  private[scalacompress] def decompress(src: String, dest: String, method: CompressionMethod)(
    ff: (BufferedInputStream, String) => Unit
  ): Try[DecompressionStats] = {
    val start   = System.currentTimeMillis()
    val fileOut = dest + SEP + new File(src).getName.dropRight(method.ext.length)
    checkExtAndDestPath(src, dest, method.ext) {
      autoClose(new BufferedInputStream(Files.newInputStream(Paths.get(src)))) { x =>
        ff(x, fileOut)
      }
    }.flatMap(_ => DecompressionStats(method.name, src, List(fileOut), System.currentTimeMillis() - start))
  }

  private[scalacompress] def decompress2(src: String, dest: String, method: CompressionMethod) = {
    val start   = System.currentTimeMillis()
    val fileOut = dest + SEP + new File(src).getName.dropRight(method.ext.length)
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
      val zFile = s"$dest$SEP${new File(src).getName}${method.ext}"
      autoClose(Files.newOutputStream(new File(zFile).toPath)) { out =>
        autoClose(new CompressorStreamFactory().createCompressorOutputStream(method.factory.get, out)) { cos =>
          val in: InputStream = Files.newInputStream(new File(src).toPath)
          IOUtils.copy(in, cos)
          zFile
        }
      }
    }.flatMap(zFile => CompressionStats(method.name, List(src), zFile, System.currentTimeMillis() - start))
  }

  private[scalacompress] def compress3(name: String, src: List[(File, Int)], outDir: String)(
    ar: OutputStream => ArchiveOutputStream
  )(entryf: (File, String) => ArchiveEntry): Try[CompressionStats] = {
    val start = System.currentTimeMillis()
    Try {
      val ll = src.map(d => (d._1, d._1.getAbsolutePath.substring(d._2 + 1)))
      Util.duplicate(ll.map(_._2)) match {
        case x :: xs => throw new Exception("Duplicate files: " + (x :: xs).mkString(","))
        case _ =>
          Util
            .autoClose(new FileOutputStream(new File(outDir))) { out =>
              Util.autoClose {
                ar(out)
              } { arOutput =>
                ll.foreach { file =>
                  val entry = entryf(file._1, file._2)
                  arOutput.putArchiveEntry(entry)
                  if (!file._1.isDirectory)
                    Util.autoClose(new FileInputStream(file._1))(ii => IOUtils.copy(ii, arOutput))
                  arOutput.closeArchiveEntry()
                }
              }
              outDir
            }
      }
    }.flatMap(
      outDir => CompressionStats(name, src.map(_._1.getAbsolutePath), outDir, System.currentTimeMillis() - start)
    )
  }
  private[scalacompress] def compress(src: String, dest: String, method: CompressionMethod)(
    compressor: (BufferedOutputStream, InputStream) => Unit
  ): Try[CompressionStats] = {
    val start = System.currentTimeMillis()
    checkSrcPath(List(src), dest) {
      val zFile = s"$dest$SEP${new File(src).getName}${method.ext}"
      autoClose(Files.newInputStream(Paths.get(src))) { in =>
        autoClose(Files.newOutputStream(Paths.get(zFile))) { fout =>
          autoClose(new BufferedOutputStream(fout))(out => compressor(out, in))
          zFile
        }
      }
    }.flatMap(zFile => CompressionStats(method.name, List(src), zFile, System.currentTimeMillis() - start))
  }

  /**
    * Compress many Array[Byte] in a OutputStream
    *
    * {{{
    * import com.github.gekomad.scalacompress.Compressors._
    *
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
    * Decompress InputStream in Array[Byte]
    *
    *{{{
    * import com.github.gekomad.scalacompress.Compressors._
    *
    * val gzStream     = GzDecompressInBuffer(new FileInputStream("/tmp/file.gz"))
    * val buffer       = new Array[Byte](2) // in real world use a big buffer
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
