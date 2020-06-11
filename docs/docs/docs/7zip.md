---
layout: docs
title: 7z
permalink: docs/7zip/
---

## 7zip

|Compression ratio|Speed|Files type|Options|
|--|--|--|--|
|Good|Bad|Files and folders|Extract single entry|


Compress
  
```scala
val src: List[String] = List("aa.txt", "a_folder", "a/b/b.txt")
val compressedFile: String = "/foo/a.7z"
val compress: Try[CompressionStats] = sevenZipCompress(src, compressedFile)
```
Decompress
```scala
val compressedFile: String = "/a/a.7z"
val destinationFolder: String = "/foo/"
val decompress: Try[DecompressionStats] = sevenZipDecompress(compressedFile, destinationFolder)
```

Extract a single entry  
```scala
val sevenZipFile: String = "/x/a.7z" 
val destinationFolder: String = "/foo/"
val decompress: Try[DecompressionStats] = sevenZipDecompress(sevenZipFile, destinationFolder, Some(List("/an/entry")))
```