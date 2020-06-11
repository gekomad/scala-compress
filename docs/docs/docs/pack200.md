---
layout: docs
title: Pack200
permalink: docs/pack200/
---

## Pack200

|Compression ratio|Speed|Files type|Options|
|--|--|--|--|
|||Single file|Only jar file|

Compress

```scala
val fileToCompress: String = "/foo/a.jar"
val destinationFolder: String = "/bar/"
val compress: Try[CompressionStats] = pack200Compress(fileToCompress, destinationFolder)
```
Decompress
```scala
val compressedFile: String = "/a/a.pack"
val destinationFolder: String = "/bar/"
val decompress: Try[DecompressionStats] = pack200Decompress(compressedFile, destinationFolder)
```

To compress a Pack200 stream view  [streams](https://gekomad.github.io/scala-compress/docs/streams/)