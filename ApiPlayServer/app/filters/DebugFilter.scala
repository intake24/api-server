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

import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.Logger
import play.api.mvc.Filter

import scala.concurrent.{ExecutionContext, Future}
import akka.stream.Materializer
import javax.inject.Inject

class DebugFilter @Inject()(implicit val mat: Materializer,
                            implicit val executionContext: ExecutionContext) extends Filter {
  def apply(next: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    next.apply(rh).map {
      result =>
        Logger.debug(s"${rh.method} ${rh.uri}")
        Logger.debug("REQUEST HEADERS:")
        rh.headers.keys.foreach {
          k =>
            Logger.info(s"${k}: ${rh.headers(k)}")
        }
        result
    }
  }
}