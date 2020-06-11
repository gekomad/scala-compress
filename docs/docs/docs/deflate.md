---
layout: docs
title: Deflate
permalink: docs/deflatep/
---

## Deflate

|Compression ratio|Speed|Files type|Options|
|--|--|--|--|
|Good|Good|Single file||

Compress

```scala
val fileToCompress: String = "/foo/a.txt"
val destinationFolder: String = "/foo"
val compress: Try[CompressionStats] = deflateCompress(fileToCompress, destinationFolder)
```
Decompress
```scala
val compressedFile: String = "/foo/a.txt.deflate"
val destinationFolder: String = "/bar/"
val decompress: Try[DecompressionStats] = deflateDecompress(compressedFile, destinationFolder)
```

To compress a Deflate stream view  [streams](https://gekomad.github.io/scala-compress/docs/streams/)