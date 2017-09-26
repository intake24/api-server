package controllers.system.asynchronous

import uk.ac.ncl.openlab.intake24.errors.AnyError

import scala.util.{Failure, Success, Try}

trait ThrottlingScheduler {
  def run(f: => Unit)
}

abstract class ThrottledTask[T] {

  def run(scheduler: ThrottlingScheduler)(onComplete: Try[T] => Unit): Unit

  def map[T2](f: T => T2): ThrottledTask[T2] = {

    val outer = this

    new ThrottledTask[T2] {

      def run(scheduler: ThrottlingScheduler)(onComplete: Try[T2] => Unit) =
        outer.run(scheduler) {
          r => onComplete(r.map(f))
        }
    }
  }

  def flatMap[T2](f: T => ThrottledTask[T2]): ThrottledTask[T2] = {
    val outer = this

    new ThrottledTask[T2] {
      def run(scheduler: ThrottlingScheduler)(onComplete: Try[T2] => Unit) =
        outer.run(scheduler) {
          _ match {
            case Success(result) => scheduler.run {
              f(result).run(scheduler)(onComplete)
            }
            case Failure(e) => onComplete(Failure(e))
          }
        }
    }
  }
}

object ThrottledTask {
  def fromTry[T](f: => Try[T]): ThrottledTask[T] = new ThrottledTask[T] {
    def run(scheduler: ThrottlingScheduler)(onComplete: (Try[T]) => Unit): Unit = onComplete(f)
  }

  def apply[T](f: => T): ThrottledTask[T] = new ThrottledTask[T] {
    def run(scheduler: ThrottlingScheduler)(onComplete: (Try[T]) => Unit): Unit = onComplete(Try(f))
  }

  def fromAnyError[T](f: => Either[AnyError, T]) = new ThrottledTask[T] {
    def run(scheduler: ThrottlingScheduler)(onComplete: (Try[T]) => Unit): Unit = {
      val tryResult = f match {
        case Right(result) => Success(result)
        case Left(error) => Failure(error.exception)
      }

      onComplete(tryResult)
    }
  }
}