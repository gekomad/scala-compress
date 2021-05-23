package com.github.gekomad.scalacompress

import java.io.{BufferedInputStream, ByteArrayOutputStream, InputStream, _}
import java.nio.file.{Files, Paths}
import java.util
import java.util.zip.{GZIPInputStream, GZIPOutputStream, ZipEntry, ZipFile}

import com.github.gekomad.scalacompress.Compressors.StreamableCompressor.{
  BZ2,
  DEFLATE,
  GZ,
  LZ4,
  LZMA,
  PACK200,
  SNAPPY,
  StreamableCompressor,
  XZ,
  ZSTANDARD
}
import com.github.gekomad.scalacompress.Util._
import net.jpountz.lz4.{LZ4FrameInputStream, LZ4FrameOutputStream}
import org.apache.commons.compress.archivers.ar.{ArArchiveEntry, ArArchiveInputStream, ArArchiveOutputStream}
import org.apache.commons.compress.archivers.cpio.{CpioArchiveEntry, CpioArchiveInputStream, CpioArchiveOutputStream}
import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveInputStream, TarArchiveOutputStream}
import org.apache.commons.compress.archivers.{ArchiveEntry, ArchiveInputStream, ArchiveOutputStream}
import org.apache.commons.compress.compressors.bzip2.{BZip2CompressorInputStream, BZip2CompressorOutputStream}
import org.apache.commons.compress.compressors.deflate.{DeflateCompressorInputStream, DeflateCompressorOutputStream}
import org.apache.commons.compress.compressors.{CompressorInputStream, CompressorOutputStream, CompressorStreamFactory}
import org.apache.commons.compress.compressors.gzip.{GzipCompressorInputStream, GzipCompressorOutputStream}
import org.apache.commons.compress.compressors.lz4.{FramedLZ4CompressorInputStream, FramedLZ4CompressorOutputStream}
import org.apache.commons.compress.compressors.lzma.{LZMACompressorInputStream, LZMACompressorOutputStream}
import org.apache.commons.compress.compressors.pack200.{Pack200CompressorInputStream, Pack200CompressorOutputStream}
import org.apache.commons.compress.compressors.snappy.{
  FramedSnappyCompressorInputStream,
  FramedSnappyCompressorOutputStream
}
import org.apache.commons.compress.compressors.xz.{XZCompressorInputStream, XZCompressorOutputStream}
import org.apache.commons.compress.compressors.zstandard.{ZstdCompressorInputStream, ZstdCompressorOutputStream}
import org.apache.commons.compress.utils.IOUtils
import scala.util.{Failure, Success, Try}

/**
  * Compressors
  *
  * @author Giuseppe Cannella
  * @since 0.0.1
  */
object Compressors {

  object StreamableCompressor extends Enumeration {
    type StreamableCompressor = Value
    val DEFLATE, BZ2, GZ, PACK200, XZ, ZSTANDARD, LZMA, LZ4, SNAPPY = Value
  }

  private[scalacompress] case class CompressionMethod(name: String, ext: String, factory: Option[String])
  private[scalacompress] val deflateMethod =
    CompressionMethod("Deflate", ".deflate", Some(CompressorStreamFactory.DEFLATE))
  private[scalacompress] val bz2Method   = CompressionMethod("Bz2", ".bz2", Some(CompressorStreamFactory.BZIP2))
  private[scalacompress] val gzMethod    = CompressionMethod("GZ", ".gz", Some(CompressorStreamFactory.GZIP))
  private[scalacompress] val parckMethod = CompressionMethod("Pack", ".pack", Some(CompressorStreamFactory.PACK200))
  private[scalacompress] val XZMethod    = CompressionMethod("XZ", ".xz", Some(CompressorStreamFactory.XZ))
  private[scalacompress] val zstandardMethod =
    CompressionMethod("Zstandard", ".zst", Some(CompressorStreamFactory.ZSTANDARD))
  private[scalacompress] val lzmaMethod     = CompressionMethod("Lzma", ".lzma", Some(CompressorStreamFactory.LZMA))
  private[scalacompress] val tarMethod      = CompressionMethod("Tar", ".tar", None)
  private[scalacompress] val zipMethod      = CompressionMethod("Zip", ".zip", None)
  private[scalacompress] val lz4Method      = CompressionMethod("Lz4", ".lz4", None)
  private[scalacompress] val snappyMethod   = CompressionMethod("Snappy", ".sz", None)
  private[scalacompress] val sevenZipMethod = CompressionMethod("7z", ".7z", None)
  private[scalacompress] val arMethod       = CompressionMethod("Ar", ".ar", None)
  private[scalacompress] val cpioMethod     = CompressionMethod("Cpio", ".cpio", None)

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def lzmaDecompress(src: String, dest: String): Try[DecompressionStats] =
    decompress2(src, dest, lzmaMethod)

  /**
    *
    * @param src file to compress
    * @param dest Destination folder
    * @return Statistics on compression procedure
    */
  def lzmaCompress(src: String, dest: String): Try[CompressionStats] =
    compress2(src, dest, lzmaMethod)

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def lz4Decompress(src: String, dest: String): Try[DecompressionStats] = {
    decompress(src, dest, lz4Method)((b: BufferedInputStream, i: String) => {
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
    compress(src, dest, lz4Method)((b: BufferedOutputStream, i: InputStream) => {
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
    decompress3(Compressors.tarMethod.name, src, dest, entries)(
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
          .flatMap(ll => compress3(Compressors.tarMethod.name, ll, dest)(f)(g))
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
    autoClose(new ZipFile(src))(a => Util.javaIteratorToList(a.entries().asIterator()))
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
        .flatMap(filesOut => DecompressionStats(sevenZipMethod.name, src, filesOut, System.currentTimeMillis() - start))
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
    decompress3(Compressors.arMethod.name, src, dest, entries)(
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
    decompress3(Compressors.cpioMethod.name, src, dest, entries)(
      (src: String) => new CpioArchiveInputStream(Files.newInputStream(Paths.get(src)))
    )

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def deflateDecompress(src: String, dest: String): Try[DecompressionStats] =
    decompress2(src, dest, deflateMethod)

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def bzip2Decompress(src: String, dest: String): Try[DecompressionStats] =
    decompress2(src, dest, bz2Method)

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def gzDecompress(src: String, dest: String): Try[DecompressionStats] = decompress2(src, dest, gzMethod)

  /**
    * {{{
    * val in: InputStream     = ???
    * val out: OutputStream   = ???
    * val compress: Try[Unit] = compressStream(StreamableCompressor.GZ, in, out)
    * //    val compress: Try[Unit] = compressStream(StreamableCompressor.DEFLATE, in, out)
    * //    val compress: Try[Unit] = compressStream(StreamableCompressor.BZ2, in, out)
    * //    val compress: Try[Unit] = compressStream(StreamableCompressor.PACK200, in, out)
    * //    val compress: Try[Unit] = compressStream(StreamableCompressor.XZ, in, out)
    * //    val compress: Try[Unit] = compressStream(StreamableCompressor.ZSTANDARD, in, out)
    * //    val compress: Try[Unit] = compressStream(StreamableCompressor.LZMA, in, out)
    * //    val compress: Try[Unit] = compressStream(StreamableCompressor.LZ4, in, out)
    * //    val compress: Try[Unit] = compressStream(StreamableCompressor.SNAPPY, in, out)
    * }}}
    *
    * @param compressorName
    * @param in
    * @param out
    * @return
    */
  def compressStream(compressorName: StreamableCompressor, in: InputStream, out: OutputStream): Try[Unit] = Try {
    val zStream: CompressorOutputStream = compressorName match {
      case DEFLATE   => new DeflateCompressorOutputStream(out)
      case BZ2       => new BZip2CompressorOutputStream(out)
      case GZ        => new GzipCompressorOutputStream(out)
      case PACK200   => new Pack200CompressorOutputStream(out)
      case XZ        => new XZCompressorOutputStream(out)
      case ZSTANDARD => new ZstdCompressorOutputStream(out)
      case LZMA      => new LZMACompressorOutputStream(out)
      case LZ4       => new FramedLZ4CompressorOutputStream(out)
      case SNAPPY    => new FramedSnappyCompressorOutputStream(out)
    }
    Util.autoClose(zStream)(zStream => IOUtils.copy(in, zStream))
    ()
  }

  /**
    * {{{
    * val in: InputStream = ???
    * val out: OutputStream = ???
    * val decompress: Try[Unit] = decompressStream(StreamableCompressor.GZ,in, out)
    * //    val decompress: Try[Unit] = decompressStream(StreamableCompressor.DEFLATE,in, out)
    * //    val decompress: Try[Unit] = decompressStream(StreamableCompressor.BZ2,in, out)
    * //    val decompress: Try[Unit] = decompressStream(StreamableCompressor.PACK200,in, out)
    * //    val decompress: Try[Unit] = decompressStream(StreamableCompressor.XZ,in, out)
    * //    val decompress: Try[Unit] = decompressStream(StreamableCompressor.ZSTANDARD,in, out)
    * //    val decompress: Try[Unit] = decompressStream(StreamableCompressor.LZMA,in, out)
    * //    val decompress: Try[Unit] = decompressStream(StreamableCompressor.LZ4,in, out)
    * //    val decompress: Try[Unit] = decompressStream(StreamableCompressor.SNAPPY,in, out)
    * }}}
    *
    * @param compressorName
    * @param in
    * @param out
    * @return
    */
  def decompressStream(compressorName: StreamableCompressor, in: InputStream, out: OutputStream): Try[Unit] = Try {
    val zStream: CompressorInputStream = compressorName match {
      case DEFLATE   => new DeflateCompressorInputStream(in)
      case BZ2       => new BZip2CompressorInputStream(in)
      case GZ        => new GzipCompressorInputStream(in)
      case PACK200   => new Pack200CompressorInputStream(in)
      case XZ        => new XZCompressorInputStream(in)
      case ZSTANDARD => new ZstdCompressorInputStream(in)
      case LZMA      => new LZMACompressorInputStream(in)
      case LZ4       => new FramedLZ4CompressorInputStream(in)
      case SNAPPY    => new FramedSnappyCompressorInputStream(in)
    }
    Util.autoClose(zStream)(zStream => IOUtils.copy(zStream, out))
    ()
  }

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def gzCompress(src: String, dest: String): Try[CompressionStats] =
    compress2(src, dest, gzMethod)

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def xzDecompress(src: String, dest: String): Try[DecompressionStats] =
    decompress2(src, dest, XZMethod)

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
    decompress(src, dest, snappyMethod)((b: BufferedInputStream, i: String) => {
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
    ()
  }

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def pack200Decompress(src: String, dest: String): Try[DecompressionStats] =
    decompress2(src, dest, parckMethod)

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def zStandardDecompress(src: String, dest: String): Try[DecompressionStats] =
    decompress2(src, dest, zstandardMethod)

  /**
    *
    * @param src File to decompress
    * @param dest Destination folder
    * @return Statistics on decompression procedure
    */
  def deflateCompress(src: String, dest: String): Try[CompressionStats] =
    compress2(src, dest, deflateMethod)

  /**
    *
    * @param src File to compress
    * @param dest Destination folder
    * @return Statistics on compression procedure
    */
  def bzip2Compress(src: String, dest: String): Try[CompressionStats] =
    compress2(src, dest, bz2Method)

  /**
    *
    * @param src File to compress
    * @param dest Destination folder
    * @return Statistics on compression procedure
    */
  def xzCompress(src: String, dest: String): Try[CompressionStats] = compress2(src, dest, XZMethod)

  /**
    *
    * @param src File to compress
    * @param dest Destination folder
    * @return Statistics on compression procedure
    */
  def snappyCompress(src: String, dest: String): Try[CompressionStats] =
    compress(src, dest, snappyMethod)((b: BufferedOutputStream, i: InputStream) => {
      autoClose(new FramedSnappyCompressorOutputStream(b))(zOut => IOUtils.copy(i, zOut))
    })

  /**
    *
    * @param src File to compress
    * @param dest Destination folder
    * @return Statistics on compression procedure
    */
  def zStandardCompress(src: String, dest: String): Try[CompressionStats] =
    compress2(src, dest, zstandardMethod)

  /**
    *
    * @param src File to compress
    * @param dest Destination folder
    * @return Statistics on compression procedure
    */
  def pack200Compress(src: String, dest: String): Try[CompressionStats] =
    compress2(src, dest, parckMethod)

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

          compress3(Compressors.arMethod.name, ll, dest)(f)(g)
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
          compress3(Compressors.cpioMethod.name, ll, dest)(f)(g)
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
    * Compressing stream example
    *
    * {{{
    * val zStream     = StreamCompress(StreamableCompressor.GZ, new FileOutputStream("/tmp/compressedFile"))
    * //  val zStream     = StreamCompress(StreamableCompressor.DEFLATE, new FileOutputStream("/tmp/compressedFile"))
    * //  val zStream     = StreamCompress(StreamableCompressor.BZ2, new FileOutputStream("/tmp/compressedFile"))
    * //  val zStream     = StreamCompress(StreamableCompressor.PACK200, new FileOutputStream("/tmp/compressedFile"))
    * //  val zStream     = StreamCompress(StreamableCompressor.XZ, new FileOutputStream("/tmp/compressedFile"))
    * //  val zStream     = StreamCompress(StreamableCompressor.ZSTANDARD, new FileOutputStream("/tmp/compressedFile"))
    * //  val zStream     = StreamCompress(StreamableCompressor.LZMA, new FileOutputStream("/tmp/compressedFile"))
    * //  val zStream     = StreamCompress(StreamableCompressor.LZ4, new FileOutputStream("/tmp/compressedFile"))
    * //  val zStream     = StreamCompress(StreamableCompressor.SNAPPY, new FileOutputStream("/tmp/compressedFile"))
    * val foo: Array[Byte] = "foo".getBytes(StandardCharsets.UTF_8)
    * val bar: Array[Byte] = "bar".getBytes(StandardCharsets.UTF_8)
    *
    * val c1: Try[Unit] = zStream.compressBuffer(foo)
    * val c2: Try[Unit] = zStream.compressBuffer(bar)
    * val cl: Try[Unit] = zStream.close()
    *
    * }}}
    *
    * @param compressorName
    * @param out
    */
  import StreamableCompressor._
  case class StreamCompress(compressorName: StreamableCompressor, out: OutputStream) {
    private val zStream: CompressorOutputStream = compressorName match {
      case DEFLATE   => new DeflateCompressorOutputStream(out)
      case BZ2       => new BZip2CompressorOutputStream(out)
      case GZ        => new GzipCompressorOutputStream(out)
      case PACK200   => new Pack200CompressorOutputStream(out)
      case XZ        => new XZCompressorOutputStream(out)
      case ZSTANDARD => new ZstdCompressorOutputStream(out)
      case LZMA      => new LZMACompressorOutputStream(out)
      case LZ4       => new FramedLZ4CompressorOutputStream(out)
      case SNAPPY    => new FramedSnappyCompressorOutputStream(out)
    }

    def compressBuffer(buffer: Array[Byte]): Try[Unit] = Try(zStream.write(buffer, 0, buffer.length))

    def close(): Try[Unit] = Try(zStream.close())
  }

  /**
    * Decompressing stream example
    *
    *{{{
    * val zStream     = StreamDecompress(StreamableCompressor.GZ, new FileInputStream("/tmp/compressedFile"))
    * //  val zStream     = StreamDecompress(StreamableCompressor.DEFLATE, new FileInputStream("/tmp/compressedFile"))
    * //  val zStream     = StreamDecompress(StreamableCompressor.BZ2, new FileInputStream("/tmp/compressedFile"))
    * //  val zStream     = StreamDecompress(StreamableCompressor.PACK200, new FileInputStream("/tmp/compressedFile"))
    * //  val zStream     = StreamDecompress(StreamableCompressor.XZ, new FileInputStream("/tmp/compressedFile"))
    * //  val zStream     = StreamDecompress(StreamableCompressor.ZSTANDARD, new FileInputStream("/tmp/compressedFile"))
    * //  val zStream     = StreamDecompress(StreamableCompressor.LZMA, new FileInputStream("/tmp/compressedFile"))
    * //  val zStream     = StreamDecompress(StreamableCompressor.LZ4, new FileInputStream("/tmp/compressedFile"))
    * //  val zStream     = StreamDecompress(StreamableCompressor.Snappy1, new FileInputStream("/tmp/compressedFile"))
    * val buffer       = new Array[Byte](2)
    * val decompressed = new StringBuilder
    *
    * @tailrec
    * def readBuffer(): Unit = {
    *   zStream.readInBuffer(buffer) match {
    *     case Failure(exception) => exception.printStackTrace
    *     case Success(bytesRead) =>
    *       if (bytesRead != -1) {
    *         decompressed.append(new String(buffer, StandardCharsets.UTF_8))
    *         readBuffer()
    *       } else {
    *         println
    *         zStream.close()
    *       }
    *   }
    * }
    * readBuffer()
    * val cl: Try[Unit] = zStream.close()
    * assert(decompressed.toString == "foobar")
    *
    * }}}
    *
    * @param in
    */
  case class StreamDecompress(compressorName: StreamableCompressor, in: InputStream) {
    private val zStream: CompressorInputStream = compressorName match {
      case DEFLATE   => new DeflateCompressorInputStream(in)
      case BZ2       => new BZip2CompressorInputStream(in)
      case GZ        => new GzipCompressorInputStream(in)
      case PACK200   => new Pack200CompressorInputStream(in)
      case XZ        => new XZCompressorInputStream(in)
      case ZSTANDARD => new ZstdCompressorInputStream(in)
      case LZMA      => new LZMACompressorInputStream(in)
      case LZ4       => new FramedLZ4CompressorInputStream(in)
      case SNAPPY    => new FramedSnappyCompressorInputStream(in)
    }

    def readInBuffer(buffer: Array[Byte]): Try[Int] =
      Try(zStream.read(buffer)) match {
        case Success(read)      => Success(read)
        case Failure(exception) => Failure(exception)
      }
    def close(): Try[Unit] = Try(zStream.close())
  }

}
