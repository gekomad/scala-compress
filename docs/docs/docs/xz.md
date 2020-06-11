---
layout: docs
title: XZ
permalink: docs/xz/
---

## XZ

|Compression ratio|Speed|Files type|Options|
|--|--|--|--|
|Very good|Bad|Single file||

Compress

```scala
val fileToCompress: String = "/foo/a.txt"
val destinationFolder: String = "/bar/"
val compress: Try[CompressionStats] = xzCompress(fileToCompress, destinationFolder)
```
Decompress
```scala
val compressedFile: String = "/foo/a.txt.xz"
val destinationFolder: String = "/bar/"
val decompress: Try[DecompressionStats] = xzDecompress(compressedFile, destinationFolder)
```

To compress a XZ stream view  [streams](https://gekomad.github.io/scala-compress/docs/streams/)