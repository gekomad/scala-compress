---
layout: docs
title: Zip
permalink: docs/zip/
---

## Zip

|Compression ratio|Speed|Files type|Options|
|--|--|--|--|
|Medium|Good|Files and Folders|Extract single entry, compress string|

Compress

```scala
val src: List[String] = List("aa.txt", "a_folder", "a/b/b.txt")
val dest: String = ??? // the destination folder or the zipped file name
val compress: Try[CompressionStats] = zipCompress(src, dest)

```
Decompress
```scala
val zippedFile: String = "a.zip"
val dest: String = "/foo/b/c/" // destination folder
val decompress: Try[DecompressionStats] = zipDecompress(zippedFile, dest)
```

Extract a single entry from a compressed file
```scala
val zippedFile: String = "a.zip"
val array: Try[Array[Byte]] = zipDecompressEntry(zippedFile, "a/b/c/c.txt")
```

Read entries
```scala
val zippedFile: String = "a.zip"
val entries: Try[List[ZipEntry]] = zipEntries(zippedFile)
```

Compress String
```scala
val aString: String = "foo"
val compressed: Try[Array[Byte]] = zipString(aString)
```

Decompress String
```scala
val compressedArray: Array[Byte] = ???
val decompressed: Try[Array[Byte]] = unzipString(compressedArray)
```