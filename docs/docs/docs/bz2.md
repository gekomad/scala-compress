---
layout: docs
title: BZ2
permalink: docs/bz2/
---

## Bzip2

|Compression ratio|Speed|Files type|Options|
|--|--|--|--|
|Very good|Medium|Single file||

Compress

```scala
val fileToCompress: String = "/a/b/foo.txt"
val destinationFolder: String = "/a/b/"
val compress: Try[CompressionStats] = bzip2Compress(fileToCompress, destinationFolder)
```
Decompress
```scala
val compressedFile: String = "/a/a.bz2"
val destinationFolder: String = "/bar/"
val decompress: Try[DecompressionStats] = bzip2Decompress(compressedFile, destinationFolder)
```

To compress a BZ2 stream view [streams](https://gekomad.github.io/scala-compress/docs/streams/)