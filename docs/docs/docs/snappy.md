---
layout: docs
title: Snappy
permalink: docs/snappy/
---

## Snappy

|Compression ratio|Speed|Files type|Options|
|--|--|--|--|
|Medium|Medium|Single file||

Compress

```scala
val fileToCompress: String = "/foo/a.txt"
val destinationFolder: String = "/bar/"
val compress: Try[CompressionStats] = snappyCompress(fileToCompress, destinationFolder)
```
Decompress
```scala
val compressedFile: String =  "/a/a.sz"
val destinationFolder: String = "/foo/"
val decompress: Try[DecompressionStats] = snappyDecompress(compressedFile, destinationFolder)
```

To compress a Snappy stream view  [streams](https://gekomad.github.io/scala-compress/docs/streams/)