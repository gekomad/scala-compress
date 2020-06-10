import java.io.File
import java.util.UUID

import Util.root
import com.github.gekomad.scalacompress.Util.{SEP, fileAccess}
import org.scalatest.funsuite.AnyFunSuite

class UtilTest extends AnyFunSuite {

  test("fileAccess") {
    val uuid = UUID.randomUUID().toString
    fileAccess(List(s"$uuid")) match {
      case Some(e) => assert(e == List(s"Can't access to $uuid"))
      case None    => assert(false)
    }
  }

  test("commonPath") {

    val a = List(root + "/a/b/c/d/a.txt", "/a/b/c/j.txt").map(_.replace("/", SEP))
    val b = List(root + "/a/b/c/d/a.txt", "/x/b/c/j.txt").map(_.replace("/", SEP))
    val c = List(root + "/a/b/c/d/a.txt", "/a/b/c/d/b.txt").map(_.replace("/", SEP))

    assert(Util.commonPath(List("a.txt", "b.txt").map(new File(_))) == root)
    assert(Util.commonPath(a.map(new File(_))) == root + "a/b/c".replace("/", SEP))
    assert(Util.commonPath(b.map(new File(_))) == root)
    assert(Util.commonPath(c.map(new File(_))) == root + "a/b/c/d".replace("/", SEP))

  }

}
