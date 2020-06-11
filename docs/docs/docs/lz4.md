---
layout: docs
title: LZ4
permalink: docs/lz4/
---

## LZ4

|Compression ratio|Speed|Files type|Options|
|--|--|--|--|
|Medium|Very good|Single file||

Compress

```scala
val fileToCompress: String = "/foo/a.txt"
val destinationFolder: String = "/bar/"
val compress: Try[CompressionStats] = lz4Compress(fileToCompress, destinationFolder)
```
Decompress
```scala
val compressedFile: String = "/foo/a.txt.lz4"
val destinationFolder: String =  "/bar/"
val decompress: Try[DecompressionStats] = lz4Decompress(compressedFile, destinationFolder)
```

To compress a LZ4 stream view  [streams](https://gekomad.github.io/scala-compress/docs/streams/)