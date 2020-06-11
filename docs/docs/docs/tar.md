---
layout: docs
title: Tar
permalink: docs/tar/
---

## Tar

|Compression ratio|Speed|Files type|Options|
|--|--|--|--|
|Bad|Very good|File and folders|Extract single entry|

Compress

```scala
val src: List[String] = List("aa.txt", "a_folder", "a/b/b.txt")
val tarFile: String = "/x/a.tar"
val compress: Try[CompressionStats] = tarCompress(src, tarFile)  
```
Decompress  
```scala
val tarFile: String = "/x/a.tar"
val destinationFolder: String = "/foo/"
val decompress: Try[DecompressionStats] = tarDecompress(tarFile, destinationFolder)
```
Extract a single entry
```scala
val tarFile: String = "/x/a.tar"
val destinationFolder: String = "/foo/"
val decompress: Try[DecompressionStats] = tarDecompress(tarFile, destinationFolder, Some(List("/an/entry")))
```