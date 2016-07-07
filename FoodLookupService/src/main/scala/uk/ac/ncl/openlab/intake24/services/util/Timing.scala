package uk.ac.ncl.openlab.intake24.services.util

import org.slf4j.Logger

trait Timing {
  def time[T](comment: String, logger: Logger)(block: => T): T = {

    val t0 = System.currentTimeMillis()

    val result = block

    logger.info(s"$comment: ${System.currentTimeMillis() - t0} ms")

    result
  }
}
