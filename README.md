[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.gekomad/scala-compress_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.gekomad/scala-compress_2.13)  
======  
  
Archivers and Compressors for Scala
  
#### Add the library to your project  
  
`libraryDependencies += "com.github.gekomad" %% "scala-compress" % "0.0.1"`  

Compression/Decompression methods return Statistics

```scala
case class CompressionStats(
  method: String,
  fileIn: List[String],
  fileOut: String,
  sizeIn: Long,
  sizeOut: Long,
  compressionRatio: Float,
  millSeconds: Long,
  mbPerSecond: Float
)
```

```scala
case class DecompressionStats(
  method: String,
  fileIn: String,
  fileOut: List[String],
  sizeIn: Long,
  sizeOut: Long,
  compressionRatio: Float,
  millSeconds: Long,
  mbPerSecond: Float
)
```

Here some results on compressing/decompressing a big scala file (20.971.520 bytes - 20 Mb) with Intel(R) Core(TM) i7-9750H CPU @ 2.60GHz and ssd disk.

|Compression method |Output file size|Compression ratio|Milliseconds to compress|MB per second compression|Milliseconds to decompress|MB per second decompression|    
|--|--|--|--|--|--|--|    
|lzma|3.122.916|6.72|11004|2|304|69|
|7z|3.123.495|6.71|10468|2|274|77|
|xz|3.123.416|6.71|10503|2|385|54|
|bz2|3.610.542|5.81|2675|8|764|27|
|zStandard|4.413.677|4.75|104|202|35|599|
|deflate|4.546.808|4.61|557|38|94|223|
|zip|4.546.936|4.61|572|37|74|283|
|gz|4.546.820|4.61|579|36|77|272|
|snappy|6.380.034|3.29|1331|16|2672|8|
|lz4|7.387.702|2.84|72|291|25|839|
|cpio|20.972.032|1.00|25|839|18|1165|
|ar|20.971.588|1.00|30|699|33|636|
|tar|20.973.056|1.00|74|283|16|1311|

For following examples use these imports 
```scala
import com.github.gekomad.scalacompress.Compressors._  
import com.github.gekomad.scalacompress.CompressionStats
import com.github.gekomad.scalacompress.DecompressionStats
```

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
## Gz  
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
Compress Stream
```scala
val in: InputStream = ???
val out: OutputStream = ???
val compress: Try[Unit] = gzCompressStream(in, out)
```

Decompress Stream  
```scala
val in: InputStream = ???
val out: OutputStream = ???
val decompress: Try[Unit] = gzDecompressStream(in, out)
``` 

Compress buffer  
```scala
val gzStream         = GzCompressBuffer(new FileOutputStream("/tmp/file.gz"))
val foo: Array[Byte] = "foo".getBytes(StandardCharsets.UTF_8) 
val bar: Array[Byte] = "bar".getBytes(StandardCharsets.UTF_8)        
                                                                     
val c1: Try[Unit] = gzStream.compressBuffer(foo)
val c2: Try[Unit] = gzStream.compressBuffer(bar)
val cl: Try[Unit] = gzStream.close()                                              
``` 

Decompress in buffer  
```scala
val gzStream     = GzDecompressInBuffer(new FileInputStream("/tmp/file.gz"))          
val buffer       = new Array[Byte](2)                                         
val decompressed = new StringBuilder                                          
                                                                              
@tailrec                                                                      
def readBuffer(): Unit = {                                                       
  gzStream.readInBuffer(buffer) match {                                       
    case Failure(exception) => exception.printStackTrace                      
    case Success(bytesRead) =>                                                
      if (bytesRead != -1) {                                                  
        decompressed.append(new String(buffer, StandardCharsets.UTF_8))       
        readBuffer()                                                          
      } else {                                                                
        println                                                               
        gzStream.close()                                                      
      }                                                                       
  }                                                                           
}                                                                             
readBuffer()
val cl: Try[Unit] = gzStream.close()                                                             
assert(decompressed.toString == "foobar")                                                                                 
``` 
## Tar  

|Compression ratio|Speed|Files type|Options|    
|--|--|--|--|
|Bad|Very good|File and folders||

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

## Bzip2  

|Compression ratio|Speed|Files type|Options|    
|--|--|--|--|
|Very good|Medium|Single file||

Compress  
  
```scala
val fileToCompress: String = "/a/b/foo.txt"  
val destinationFolder: String = "/a/b/"
val compress: Try[CompressionStats] = bzip2Compress(fileToCompress, destinationFolder)  
```
Decompress  
```scala
val compressedFile: String = "/a/a.bz2"  
val destinationFolder: String = "/bar/"
val decompress: Try[DecompressionStats] = bzip2Decompress(compressedFile, destinationFolder)  
```
  
## Ar
|Compression ratio|Speed|Files type|Options|    
|--|--|--|--|
|Bad|Very good|Files and folders||

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
  
## Lz4  

|Compression ratio|Speed|Files type|Options|    
|--|--|--|--|
|Medium|Very good|Single file||

Compress  
  
```scala
val fileToCompress: String = "/foo/a.txt"  
val destinationFolder: String = "/bar/"
val compress: Try[CompressionStats] = lz4Compress(fileToCompress, destinationFolder)  
```
Decompress  
```scala
val compressedFile: String = "/foo/a.txt.lz4"
val destinationFolder: String =  "/bar/"
val decompress: Try[DecompressionStats] = lz4Decompress(compressedFile, destinationFolder)  
```
  
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
## Cpio  

|Compression ratio|Speed|Files type|Options|    
|--|--|--|--|
|Bad|Very good|Files and folders||


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
## 7zip  
|Compression ratio|Speed|Files type|Options|    
|--|--|--|--|
|Good|Bad|Files and folders||
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
  
## Snappy
|Compression ratio|Speed|Files type|Options|    
|--|--|--|--|
|Medium|Medium|Single file||
Compress  
  
```scala
val fileToCompress: String = "/foo/a.txt"  
val destinationFolder: String = "/bar/"
val compress: Try[CompressionStats] = snappyCompress(fileToCompress, destinationFolder)  
```
Decompress  
```scala
val compressedFile: String =  "/a/a.sz"  
val destinationFolder: String = "/foo/"
val decompress: Try[DecompressionStats] = snappyDecompress(compressedFile, destinationFolder)  
```
  
## Xz
|Compression ratio|Speed|Files type|Options|    
|--|--|--|--|
|Very good|Bad|Single file||
Compress  
  
```scala
val fileToCompress: String = "/foo/a.txt"
val destinationFolder: String = "/bar/"
val compress: Try[CompressionStats] = xzCompress(fileToCompress, destinationFolder)  
```
Decompress  
```scala
val compressedFile: String = "/foo/a.txt.xz"
val destinationFolder: String = "/bar/"
val decompress: Try[DecompressionStats] = xzDecompress(compressedFile, destinationFolder)  
```
  
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
val compressedFile: String = "/foo/a.txt.zstd"  
val destinationFolder: String = "/bar/"  
val decompress: Try[DecompressionStats] = zStandardDecompress(compressedFile, destinationFolder)  
```
  
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
## Scaladoc API  
[Scala doc](https://javadoc.io/doc/com.github.gekomad/scala-compress_2.13)  
  
## Bugs and Feedback  
For bugs, questions and discussions please use [Github Issues](https://github.com/gekomad/scala-compress/issues).  
  
## License  
  
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance  
with the License. You may obtain a copy of the License at  
  
[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)  
  
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  
See the License for the specific language governing permissions and limitations under the License.
