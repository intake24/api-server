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

import java.util.concurrent.atomic.AtomicReference
import java.util.TimerTask

import scala.concurrent.duration._
import org.slf4j.LoggerFactory

class AutoReloadIndex(reload: () => AbstractFoodIndex, delay: Duration, period: Duration, description: String) extends FoodIndex {
  val log = LoggerFactory.getLogger(classOf[AutoReloadIndex])
  
  val ref = new AtomicReference[AbstractFoodIndex](reload.apply()) // load the first index synchronously 
  
  val reloadTimer = new java.util.Timer(true) 
    
  reloadTimer.schedule(new TimerTask() {
    override def run() = {
      log.debug(s"Reloading $description index...")
      val t0 = System.currentTimeMillis()
      ref.set(reload.apply())
      log.debug(s"Reloaded $description index in ${System.currentTimeMillis() - t0} ms")
    }
  }, delay.toMillis, period.toMillis)
  
  def lookup(description: String, maxFoods: Int, maxCategories: Int): IndexLookupResult = ref.get.lookup(description, maxFoods, maxCategories)
}