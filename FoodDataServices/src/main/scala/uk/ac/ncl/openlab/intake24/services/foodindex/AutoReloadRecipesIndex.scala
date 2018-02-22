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

package uk.ac.ncl.openlab.intake24.services.foodindex

import java.util.TimerTask
import java.util.concurrent.atomic.AtomicReference

import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.services.RecipesAttributeIndex

import scala.concurrent.duration._

class AutoReloadRecipesIndex(reload: () => RecipesAttributeIndex, delay: Duration, period: Duration) extends RecipesAttributeIndex {
  val log = LoggerFactory.getLogger(classOf[AutoReloadRecipesIndex])

  val ref = new AtomicReference[RecipesAttributeIndex](reload.apply()) // load the first index synchronously

  val reloadTimer = new java.util.Timer(true)

  reloadTimer.schedule(new TimerTask() {
    override def run() = {
      log.debug(s"Reloading recipe attributes index...")
      val t0 = System.currentTimeMillis()
      ref.set(reload.apply())
      log.debug(s"Reloaded recipe attributes index in ${System.currentTimeMillis() - t0} ms")
    }
  }, delay.toMillis, period.toMillis)

  def filterForRecipes(indexLookupResult: IndexLookupResult) = ref.get().filterForRecipes(indexLookupResult)

  def filterForRegularFoods(indexLookupResult: IndexLookupResult) = ref.get().filterForRegularFoods(indexLookupResult)
}