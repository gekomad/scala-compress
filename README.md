[![Javadocs](https://javadoc.io/badge/com.github.gekomad/scala-compress_2.13.svg)](https://javadoc.io/doc/com.github.gekomad/scala-compress_2.13/latest/com/github/gekomad/scalacompress/index.html)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.gekomad/scala-compress_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.gekomad/scala-compress_2.13)

Archivers and Compressors for Scala
======

#### Add the library to your project

`libraryDependencies += "com.github.gekomad" %% "scala-compress" % "0.0.3"`

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

To use the library add these **imports**
```scala
import com.github.gekomad.scalacompress.Compressors._
import com.github.gekomad.scalacompress.CompressionStats
import com.github.gekomad.scalacompress.DecompressionStats
```


## View [microsite](https://gekomad.github.io/scala-compress/docs/) for more information.

## Bugs and Feedback
For bugs, questions and discussions please use [Github Issues](https://github.com/gekomad/scala-compress/issues).

## License

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and limitations under the License.
