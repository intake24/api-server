/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package filters

import akka.stream.Materializer
import org.slf4j.LoggerFactory
import play.api.mvc.{Filter, RequestHeader, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DebugFilter @Inject()(implicit val mat: Materializer,
                            implicit val executionContext: ExecutionContext) extends Filter {

  val logger = LoggerFactory.getLogger(classOf[DebugFilter])

  def apply(next: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    next.apply(rh).map {
      result =>
        logger.debug(s"${rh.method} ${rh.uri}")
        logger.debug("REQUEST HEADERS:")
        rh.headers.keys.foreach {
          k =>
            logger.info(s"${k}: ${rh.headers(k)}")
        }
        result
    }
  }
}
