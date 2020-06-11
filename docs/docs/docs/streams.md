---
layout: docs
title: Streams
permalink: docs/streams/
---

## Streams

Compress stream
```scala
val zStream     = StreamCompress(StreamableCompressor.GZ, new FileOutputStream("/tmp/compressedFile"))
//  val zStream     = StreamCompress(StreamableCompressor.DEFLATE, new FileOutputStream("/tmp/compressedFile"))
//  val zStream     = StreamCompress(StreamableCompressor.BZ2, new FileOutputStream("/tmp/compressedFile"))    
//  val zStream     = StreamCompress(StreamableCompressor.PACK200, new FileOutputStream("/tmp/compressedFile"))
//  val zStream     = StreamCompress(StreamableCompressor.XZ, new FileOutputStream("/tmp/compressedFile"))
//  val zStream     = StreamCompress(StreamableCompressor.ZSTANDARD, new FileOutputStream("/tmp/compressedFile"))
//  val zStream     = StreamCompress(StreamableCompressor.LZMA, new FileOutputStream("/tmp/compressedFile"))
//  val zStream     = StreamCompress(StreamableCompressor.LZ4, new FileOutputStream("/tmp/compressedFile"))
//  val zStream     = StreamCompress(StreamableCompressor.SNAPPY, new FileOutputStream("/tmp/compressedFile"))
val foo: Array[Byte] = "foo".getBytes(StandardCharsets.UTF_8)
val bar: Array[Byte] = "bar".getBytes(StandardCharsets.UTF_8)

val c1: Try[Unit] = zStream.compressBuffer(foo)
val c2: Try[Unit] = zStream.compressBuffer(bar)
val cl: Try[Unit] = zStream.close()
```

Decompress stream
```scala
val zStream     = StreamDecompress(StreamableCompressor.GZ, new FileInputStream("/tmp/compressedFile"))
//  val zStream     = StreamDecompress(StreamableCompressor.DEFLATE, new FileInputStream("/tmp/compressedFile"))
//  val zStream     = StreamDecompress(StreamableCompressor.BZ2, new FileInputStream("/tmp/compressedFile"))    
//  val zStream     = StreamDecompress(StreamableCompressor.PACK200, new FileInputStream("/tmp/compressedFile"))
//  val zStream     = StreamDecompress(StreamableCompressor.XZ, new FileInputStream("/tmp/compressedFile"))
//  val zStream     = StreamDecompress(StreamableCompressor.ZSTANDARD, new FileInputStream("/tmp/compressedFile"))
//  val zStream     = StreamDecompress(StreamableCompressor.LZMA, new FileInputStream("/tmp/compressedFile"))
//  val zStream     = StreamDecompress(StreamableCompressor.LZ4, new FileInputStream("/tmp/compressedFile"))
//  val zStream     = StreamDecompress(StreamableCompressor.SNAPPY, new FileInputStream("/tmp/compressedFile"))
val buffer       = new Array[Byte](2)
val decompressed = new StringBuilder

@tailrec
def readBuffer(): Unit = {
  zStream.readInBuffer(buffer) match {
    case Failure(exception) => exception.printStackTrace
    case Success(bytesRead) =>
      if (bytesRead != -1) {
        decompressed.append(new String(buffer, StandardCharsets.UTF_8))
        readBuffer()
      } else {
        println
        zStream.close()
      }
  }
}
readBuffer()
val cl: Try[Unit] = zStream.close()
assert(decompressed.toString == "foobar")
```


Compress stream
```scala
val in: InputStream     = ???
val out: OutputStream   = ???
val compress: Try[Unit] = compressStream(StreamableCompressor.GZ, in, out)
//    val compress: Try[Unit] = compressStream(StreamableCompressor.DEFLATE, in, out)
//    val compress: Try[Unit] = compressStream(StreamableCompressor.BZ2, in, out)
//    val compress: Try[Unit] = compressStream(StreamableCompressor.PACK200, in, out)
//    val compress: Try[Unit] = compressStream(StreamableCompressor.XZ, in, out)
//    val compress: Try[Unit] = compressStream(StreamableCompressor.ZSTANDARD, in, out)
//    val compress: Try[Unit] = compressStream(StreamableCompressor.LZMA, in, out)
//    val compress: Try[Unit] = compressStream(StreamableCompressor.LZ4, in, out)
//    val compress: Try[Unit] = compressStream(StreamableCompressor.SNAPPY, in, out)
```

Decompress stream
```scala
val in: InputStream = ???
val out: OutputStream = ???
val decompress: Try[Unit] = decompressStream(StreamableCompressor.GZ,in, out)
//    val decompress: Try[Unit] = decompressStream(StreamableCompressor.DEFLATE,in, out)
//    val decompress: Try[Unit] = decompressStream(StreamableCompressor.BZ2,in, out)
//    val decompress: Try[Unit] = decompressStream(StreamableCompressor.PACK200,in, out)
//    val decompress: Try[Unit] = decompressStream(StreamableCompressor.XZ,in, out)
//    val decompress: Try[Unit] = decompressStream(StreamableCompressor.ZSTANDARD,in, out)
//    val decompress: Try[Unit] = decompressStream(StreamableCompressor.LZMA,in, out)
//    val decompress: Try[Unit] = decompressStream(StreamableCompressor.LZ4,in, out)
//    val decompress: Try[Unit] = decompressStream(StreamableCompressor.SNAPPY,in, out)
```
