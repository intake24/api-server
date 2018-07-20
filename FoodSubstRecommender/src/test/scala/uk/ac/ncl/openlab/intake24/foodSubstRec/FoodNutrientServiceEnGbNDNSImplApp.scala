package uk.ac.ncl.openlab.intake24.foodSubstRec

import java.io.File
import java.sql.DriverManager

import com.typesafe.config.ConfigFactory
import uk.ac.ncl.openlab.intake24.foodsql.user.FoodCompositionServiceImpl

/**
  * Created by Tim Osadchiy on 16/07/2018.
  */
object FoodNutrientServiceEnGbNDNSImplApp extends App {

  val configPath = "apiPlayServer/conf/application.conf"

  DriverManager.registerDriver(new org.postgresql.Driver)

  val conf = ConfigFactory.parseFile(new File(configPath))
  val dataSource = new org.postgresql.ds.PGSimpleDataSource()

  dataSource.setUser(conf.getString("db.intake24_foods.username"))
  dataSource.setUrl(conf.getString("db.intake24_foods.url"))
  dataSource.setPassword(conf.getString("db.intake24_foods.password"))

  val foodCompositionService = new FoodCompositionServiceImpl(dataSource)

  val t0 = System.currentTimeMillis()
  val service = new FoodNutrientServiceEnGbNDNSImpl(foodCompositionService)
  println(s"Time spent: ${System.currentTimeMillis() - t0}")

}
