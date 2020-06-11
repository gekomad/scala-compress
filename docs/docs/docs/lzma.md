---
layout: docs
title: LZMA
permalink: docs/lzma/
---

## Lzma

|Compression ratio|Speed|Files type|Options|
|--|--|--|--|
|Very good|Bad|Single file||

Compress

```scala
val fileToCompress: String = "/foo/a.txt"
val destinationFolder: String = "/bar/"
val compress: Try[CompressionStats] = lzmaCompress(fileToCompress, destinationFolder)
```
Decompress
```scala
val compressedFile: String = "/foo/a.txt.lzma"
val destinationFolder: String =  "/bar/"
val decompress: Try[DecompressionStats] = lzmaDecompress(compressedFile, destinationFolder)
```

To compress a LZMA stream view  [streams](https://gekomad.github.io/scala-compress/docs/streams/)