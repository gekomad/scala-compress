package com.github.gekomad.scalacompress

import java.io.{File, IOException, InputStream, OutputStream}
import java.nio.file.{Files, Paths, StandardCopyOption}
import org.apache.commons.compress.archivers.{ArchiveEntry, ArchiveInputStream}
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

object Util {

  /**
    *
    * @param s
    * @return true if the folder is writable
    */
  def isWritableDirectory(s: String): Boolean =
    Try(new File(s)) match {
      case Success(f) if f.canWrite && f.isDirectory => true
      case _                                         => false
    }

  private[scalacompress] def isWritableFile(s: String): Boolean =
    Try(new File(s)) match {
      case Success(f) if f.canWrite && f.isFile => true
      case _                                    => false
    }

  /**
    *
    * @param fileList
    * @return None if all files in fileList are accessible or Some(error List)
    */
  def fileAccess(fileList: List[String]): Option[List[String]] = {
    @tailrec
    def go(fileList: List[String], err: List[String]): List[String] = {
      fileList match {
        case Nil => err
        case ::(head, tl) =>
          Try(new File(head)) match {
            case Failure(exception)      => go(tl, s"file $head ${exception.getMessage}" :: err)
            case Success(f) if f.canRead => go(tl, err)
            case Success(f)              => go(tl, s"Can't access to $f" :: err)
          }
      }
    }
    val myList = go(fileList, Nil)
    myList.headOption.map(_ => myList)
  }

  @tailrec
  private[scalacompress] def write(
    inStream: ArchiveInputStream,
    dest: String,
    outFiles: List[String] = Nil
  ): List[String] = {
    val entry: ArchiveEntry = inStream.getNextEntry
    if (entry != null) {
      val fileOut = s"$dest/${entry.getName}"
      new File(fileOut).getParentFile.mkdirs()
      Files.copy(inStream, Paths.get(fileOut), StandardCopyOption.REPLACE_EXISTING)
      write(inStream, dest, fileOut :: outFiles)
    } else outFiles
  }

  private[scalacompress] def writeBuffer(in: InputStream, zOut: OutputStream, bufferSize: Int = 4096): Unit = {
    val buffer = new Array[Byte](bufferSize)
    @tailrec
    def go(): Unit = {
      val c = in.read(buffer)
      if (c != -1) {
        zOut.write(buffer, 0, c)
        go()
      }
    }
    go()
  }

  def getParent(f: File): String = f.getParent match {
    case null => "/"
    case a    => a
  }

  private[scalacompress] def duplicate(l: List[String]): List[String] =
    l.groupBy(identity).collect { case (x, List(_, _, _*)) => x }.toList

  private[scalacompress] def autoClose[A <: AutoCloseable, B](resource: A)(code: A => B): B = {
    try code(resource)
    finally resource.close()
  }

  private[scalacompress] def getTotalSize(f: String): Try[Long] =
    getListOfFiles(new File(f)).map(_.map(_._1.length).sum)

  /**
    *
    * @param dir
    * @return a list of all paths and the lenght of dir's parent
    */
  def getListOfFiles(dir: File): Try[List[(File, Int)]] = {
    def go(dir: File): List[File] = dir match {
      case d if d.exists && d.isDirectory =>
        if (d.canRead) {
          val files = d.listFiles.filter(_.isFile).toList
          val dirs  = dir.listFiles.filter(_.isDirectory).toList
          files ::: dirs.foldLeft(List.empty[File])(_ ::: go(_))
        } else throw new IOException(s"Error opening directory '$d': Permission denied")
      case f => List(f)
    }
    Try(go(dir)).map(_.map(i => (i, getParent(dir).length)))
  }
}
