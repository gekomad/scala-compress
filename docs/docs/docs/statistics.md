---
layout: docs
title: Statistics
permalink: docs/
---

Compression/Decompression methods return Statistics

```scala
case class CompressionStats(
  method: String,           // Compression method
  fileIn: List[String],     // Files and folders to compress
  fileOut: String,          // Compressed file
  sizeIn: Long,             // Total fileIn size
  sizeOut: Long,            // Total fileOut size
  compressionRatio: Float,  // Uncompress file size/compress file size
  millSeconds: Long,        // Time to compress
  mbPerSecond: Float        // Compression speed in MB per second
)
```

```scala
case class DecompressionStats(
  method: String,           // Compression method
  fileIn: String,           // Files and folders to compress
  fileOut: List[String],    // Decompressed files and folders
  sizeIn: Long,             // Total fileIn size
  sizeOut: Long,            // Total fileOut size
  compressionRatio: Float,  // Uncompress file size/compress file size
  millSeconds: Long,        // Time to decompress
  mbPerSecond: Float        // Compression speed in MB per second
)
```