import java.io.File
import java.util.UUID

import com.github.gekomad.scalacompress.Util.fileAccess
import org.scalatest.funsuite.AnyFunSuite

class UtilTest extends AnyFunSuite {

  test("isDirectory") {
    assert(!com.github.gekomad.scalacompress.Util.isWritableDirectory(UUID.randomUUID().toString))
    assert(com.github.gekomad.scalacompress.Util.isWritableDirectory("."))
  }

  test("fileAccess") {
    val uuid = UUID.randomUUID().toString
    fileAccess(List(s"$uuid")) match {
      case Some(e) => assert(e == List(s"Can't access to $uuid"))
      case None    => assert(false)
    }
  }

  test("commonPath") {

    assert(Util.commonPath(List("a.txt", "b.txt").map(new File(_))) == "/")
    assert(Util.commonPath(List("/a/b/c/d/a.txt", "/a/b/c/j.txt").map(new File(_))) == "/a/b/c")
    assert(Util.commonPath(List("/a/b/c/d/a.txt", "/x/b/c/j.txt").map(new File(_))) == "/")
    assert(Util.commonPath(List("/a/b/c/d/a.txt", "/a/b/c/d/b.txt").map(new File(_))) == "/a/b/c/d")

  }

}
