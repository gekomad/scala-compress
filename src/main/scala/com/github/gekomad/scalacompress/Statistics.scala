package com.github.gekomad.scalacompress

import com.github.gekomad.scalacompress.Util._

import scala.util.Try

/**
  * Statistics on compression/decompression procedure
  *
  * @author Giuseppe Cannella
  * @since 0.0.1
  */
/**
  *
  * @param method Compression method
  * @param fileIn Files and folders to compress
  * @param fileOut Compressed file name
  * @param sizeIn Total fileIn size
  * @param sizeOut Total fileOut size
  * @param compressionRatio Uncompress size/compress size
  * @param millSeconds Time to compress
  * @param mbPerSecond Compression speed in MB per second
  */
case class CompressionStats(
  method: String,
  fileIn: List[String],
  fileOut: String,
  sizeIn: Long,
  sizeOut: Long,
  compressionRatio: Float,
  millSeconds: Long,
  mbPerSecond: Float
)

/**
  *
  * @param method Compression method
  * @param fileIn Compressed file name
  * @param fileOut Decompressed files and folders
  * @param sizeIn Total fileIn size
  * @param sizeOut Total fileOut size
  * @param compressionRatio Uncompress size/compress size
  * @param millSeconds Time to decompress
  * @param mbPerSecond Decompression speed in MB per second
  */
case class DecompressionStats(
  method: String,
  fileIn: String,
  fileOut: List[String],
  sizeIn: Long,
  sizeOut: Long,
  compressionRatio: Float,
  millSeconds: Long,
  mbPerSecond: Float
)

object CompressionStats {

  def apply(method: String, in: List[String], out: String, time: Long): Try[CompressionStats] =
    for {
      s1 <- Try(in.foldLeft(0L)(_ + getTotalSize(_).get))
      s2 <- getTotalSize(out)
    } yield
      new CompressionStats(
        method,
        in,
        out,
        s1,
        s2,
        compressionRatio = s1.toFloat / s2.toFloat,
        time,
        (s1.toFloat / time) / 1000
      )
}

object DecompressionStats {

  def apply(method: String, in: String, out: List[String], time: Long): Try[DecompressionStats] =
    for {
      s1 <- getTotalSize(in)
      s2 <- Try(out.foldLeft(0L)(_ + getTotalSize(_).get))
    } yield
      new DecompressionStats(
        method,
        in,
        out,
        s1,
        s2,
        compressionRatio = s1.toFloat / s2.toFloat,
        time,
        (s1.toFloat / time) / 1000
      )
}
