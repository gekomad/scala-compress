---
layout: docs
title: Zstandard
permalink: docs/zstandard/
---

## Zstandard

|Compression ratio|Speed|Files type|Options|
|--|--|--|--|
|Good|Very good|Single file||

Compress

```scala
val fileToCompress: String = "/foo/a.txt"
val destinationFolder: String = "/bar/"
val compress: Try[CompressionStats] = zStandardCompress(fileToCompress, destinationFolder)
```
Decompress
```scala
val compressedFile: String = "/foo/a.txt.zst"
val destinationFolder: String = "/bar/"
val decompress: Try[DecompressionStats] = zStandardDecompress(compressedFile, destinationFolder)
```

To compress a Zstandard stream view  [streams](https://gekomad.github.io/scala-compress/docs/streams/)