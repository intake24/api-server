package uk.ac.ncl.openlab.intake24.services.fooddb.images

import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.Files

class ImageStorageLocalTests extends AnyFunSuite {

  val tempBaseDir = Files.createTempDirectory("intake24test")
  val testFile = Files.createTempFile("intake24test", "")
  val urlPrefix = "http://test/images"

  val pathOutsideBaseDirMessage = "Paths outside the base directory are not allowed"
  val pathsOutsideBaseDir = List("./..", "..", "../../bad", "./../bad", "good/../../bad")

  val service = new ImageStorageLocal(LocalImageStorageSettings(tempBaseDir.toString, urlPrefix))

  private def expectSuccess[T](result: Either[ImageStorageError, T]): T = result match {
    case Left(e) => fail(e.e)
    case Right(value) => value
  }

  private def expectFailure(result: Either[ImageStorageError, Any], expectedMessage: String) =
    result match {
      case Left(e) => assert(expectedMessage == e.e.getMessage)
      case Right(_) => fail("Operation expected to fail but was successful")
    }

  test("uploadImage should generate random names when suggestedPath is null") {
    val path = expectSuccess(service.uploadImage(null, testFile))
    assert(path.length == 36) // UUID string representation
  }

  test("uploadImage should generate random names when suggestedPath is empty") {
    val path = expectSuccess(service.uploadImage("", testFile))
    assert(path.length == 36) // UUID string representation
  }

  test("uploadImage should generate alternative names when file already exists") {
    val name = "test1"

    val path1 = expectSuccess(service.uploadImage(name, testFile))
    val path2 = expectSuccess(service.uploadImage(name, testFile))

    assert(path1 == name)
    assert(path2.length == (name.length + 37)) // name + '-' + UUID
  }

  test("uploadImage should allow and automatically create subdirectories") {
    val path = expectSuccess(service.uploadImage("dir1/dir2/test", testFile))
    assert(Files.exists(tempBaseDir.resolve(path)))
  }

  test("uploadImage should not allow paths outside the base directory") {
    pathsOutsideBaseDir.foreach(path => expectFailure(service.uploadImage(path, testFile), pathOutsideBaseDirMessage))
  }

  test("deleteImage should not allow access to paths outside the base directory") {
    pathsOutsideBaseDir.foreach(path => expectFailure(service.deleteImage(path), pathOutsideBaseDirMessage))
  }

  test("downloadImage should not allow access to paths outside the base directory") {
    pathsOutsideBaseDir.foreach(path => expectFailure(service.downloadImage(path, Files.createTempFile("intake24test", "")), pathOutsideBaseDirMessage))
  }
}
