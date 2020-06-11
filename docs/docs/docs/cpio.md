---
layout: docs
title: Cpio
permalink: docs/cpio/
---

## Cpio

|Compression ratio|Speed|Files type|Options|
|--|--|--|--|
|Bad|Very good|Files and folders|Extract single entry|

Compress

```scala
val src: List[String] = List("aa.txt", "a_folder", "a/b/b.txt")
val cpioFile: String = "a.cpio"
val compress: Try[CompressionStats] = cpioCompress(src, cpioFile)
```
Decompress
```scala
val cpioFile: String = "a.cpio"
val destinationFolder: String = "/a/b/c"
val decompress: Try[DecompressionStats] = cpioDecompress(cpioFile, destinationFolder)
```
Extract a single entry
```scala
val cpioFile: String = "/x/a.tar"
val destinationFolder: String = "/foo/"
val decompress: Try[DecompressionStats] = cpioDecompress(cpioFile, destinationFolder, Some(List("/an/entry")))
```