---
layout: docs
title: Ar
permalink: docs/ar/
---

## Ar

|Compression ratio|Speed|Files type|Options|
|--|--|--|--|
|Bad|Very good|Files and folders|Extract single entry, limit size file 16 chars|

Compress
```scala
val src: List[String] = List("aa.txt", "a_folder", "a/b/b.txt")
val compressedFile: String = "/a/a.ar"
val compress: Try[CompressionStats] = arCompress(src, compressedFile)
```
Decompress
```scala
val compressedFile: String = "/a/a.ar"
val destinationFolder: String = "/foo/"
val decompress: Try[DecompressionStats] = arDecompress(compressedFile, destinationFolder)
```

Extract a single entry
```scala
val arFile: String = "/x/a.tar" 
val destinationFolder: String = "/foo/"
val decompress: Try[DecompressionStats] = arDecompress(arFile, destinationFolder, Some(List("/an/entry")))
```