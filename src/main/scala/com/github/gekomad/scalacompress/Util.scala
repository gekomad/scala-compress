package com.github.gekomad.scalacompress

import java.io.{File, IOException}
import java.nio.file.{Files, Paths, StandardCopyOption}

import org.apache.commons.compress.archivers.{ArchiveEntry, ArchiveInputStream}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

object Util {

  /**
    * The system-dependent path-separator character
    */
  val SEP: String = java.io.File.separator

  /**
    *
    * @param dir
    * @return true if dir is a writable folder
    */
  private[scalacompress] def isWritableDirectory(dir: String): Boolean =
    Try(new File(dir)) match {
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
  private[scalacompress] def extractEntries(
    inStream: ArchiveInputStream,
    dest: String,
    entries: Option[List[String]], //if none extract all entries
    outFiles: List[String] = Nil
  ): List[String] = {
    val entry: ArchiveEntry = inStream.getNextEntry
    if (entry != null) {
      if (entries.isEmpty || entries.get.contains(entry.getName) || entries.get.exists(
            e => entry.getName.startsWith(s"$e/")
          )) {
        val fileOut = s"$dest$SEP${entry.getName}"
        Files.createDirectories(new File(fileOut).getParentFile.toPath)
        if (entry.isDirectory) Files.createDirectories(new File(fileOut).toPath)
        else Files.copy(inStream, Paths.get(fileOut), StandardCopyOption.REPLACE_EXISTING)
        extractEntries(inStream, dest, entries, fileOut :: outFiles)
      } else extractEntries(inStream, dest, entries, outFiles)
    } else outFiles
  }

  /**
    *
    * @param file
    * @return File.getParent() or "/" if parent doesn't exist
    */
  def getParent(file: File): String = file.getParent match {
    case null => SEP
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
    * @return A list of all absolute paths under dir and the lenght of dir's parent
    */
  def getListOfFiles(dir: File): Try[List[(File, Int)]] = {
    def go(dir: File): List[File] = dir match {
      case d if d.exists && d.isDirectory =>
        if (d.canRead) {
          val files = d.listFiles.filter(_.isFile).toList
          val dirs  = dir.listFiles.filter(_.isDirectory).toList
          files ::: (if (dirs.isEmpty && files.isEmpty) List(d) else dirs.foldLeft(List.empty[File])(_ ::: go(_)))
        } else throw new IOException(s"Error opening directory '$d': Permission denied")
      case f => List(f)
    }
    Try(go(dir)).map(_.map(i => (i, getParent(dir).length)))
  }

  def javaIteratorToList[A](i: java.util.Iterator[A]): List[A] = {

    def go(l: scala.collection.mutable.ListBuffer[A]): scala.collection.mutable.ListBuffer[A] =
      Try(i.next()) match {
        case Success(a) =>
          l += a
          go(l)
        case _ => l
      }

    go(scala.collection.mutable.ListBuffer.empty[A]).toList
  }
}
