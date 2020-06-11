---
layout: docs
title: Gzip
permalink: docs/gzip/
---

## Gzip

|Compression ratio|Speed|Files type|Options|
|--|--|--|--|
|Good|Good|Single File|Extract single entry, compress string, compress stream, compress buffer|

Compress

```scala
val fileToCompress: String = "a.txt"
val destinationFolder: String = "/a/b/"
val compress: Try[CompressionStats] = gzCompress(fileToCompress, destinationFolder)
```
Decompress
```scala
val compressedFile: String = "/a/b/a.txt.gz"
val destinationFolder: String = "/x/y/"
val decompress: Try[DecompressionStats] = gzDecompress(compressedFile, destinationFolder)
```

To compress a Gzip stream view  [streams](https://gekomad.github.io/scala-compress/docs/streams/)