package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation.newzealand

import java.io.{BufferedReader, InputStreamReader}

import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24._
import uk.ac.ncl.openlab.intake24.errors.{ParentRecordNotFound, RecordNotFound, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.foodxml.{Categories, CategoryDef, FoodDef}
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConfigChooser, DatabaseConnection, WarningMessage}
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodsAdminImpl
import uk.ac.ncl.openlab.intake24.sql.tools.food.ImportNutrientTableDescriptions.{getDataSource, options}

import scala.xml.XML

object ImportNewZealandFoods extends App with DatabaseConnection with WarningMessage {

  val standardUnits = Set("whole_stuffed_peppers", "oysters", "large_pies", "pigs_in_blankets", "prawns", "small_steaks", "milles_feuilles", "small_avocados", "burritos", "fajitas", "small_cartons", "medium_parsnips", "medium_jars", "cherries", "mini_boxes", "slices_1_8th_of_pie", "punnets", "pancakes", "average_sized_dim_sum", "scallops", "tablets", "small_parsnips", "packets", "thin_slices", "rings", "medium_carrots", "panna_cottas", "medium_courgettes", "whole_small_quiches", "large_hot_dogs", "mozzarella_sticks", "kebabs", "large_tarts", "average_sized_flapjacks", "cannelloni_tubes", "mushrooms", "mini_fillets", "large_bananas", "small_packets", "large_carrots", "large_parsnips", "medium_beetroot", "multipack_bags", "medium_handfuls", "small_individual_tubs", "fingers", "standard_bags", "bagels", "yams", "sticks", "large_bowls", "large_pancakes", "lollipops", "crackers", "small_bars", "medium_cartons", "regular_churros", "artichoke_hearts", "sweets", "large_avocados", "medium_chops", "poppadums", "treble_measures", "leeks", "standard_boxes", "medium_portions", "mozarella_balls", "berries", "multipack_bottles", "small_squares", "medium_gherkins", "medium_steaks", "enchiladas", "whole_sausages", "large_cartons", "standard_size_sticks", "large_flapjacks", "mini_cobs", "chicken_livers", "sandwiches_made_with_two_slices_of_bread", "squares", "small_sticks", "standard_packs", "level_teaspoons", "hot_pots", "balls", "wafers", "large_spring_rolls", "fatty_cutties", "one_inch_pieces", "waffles", "medium_pots", "mini_eggs", "packs", "vol_au_vents", "sheets", "cups", "marshmallows", "small_eggs", "small_jars", "whole_large_pies", "pouches", "profiteroles", "tins", "large_truffles", "straws", "individual_packs", "aubergine", "mini_skewers", "whole_radishes", "pieces", "falafels", "sachets_made_up_with_milk", "small_carrots", "bacon_and_cheese_grills", "rolls", "standard_bars", "individual_pies", "mini_snack_packs", "fillets", "small_truffles", "large_bars", "small_sundaes", "biscuits", "mussels", "beetroot", "whole_large_quiches", "chocolate_oranges", "blocks", "large_slices", "small_souffles", "large_share_bags", "pots", "average_tubs___bags", "olives", "whole_large_tarts", "small_kiwis", "stuffed_peppers_half_a_pepper", "cassavas", "tubs", "mini_tubs", "sprouts", "extra_large_eggs", "segments", "small_handfuls", "small_individual_pavlovas", "medium_kiwis", "shots", "heaped_teaspoons", "medium_scones", "bites", "pastries", "large_onions", "average_spring_rolls", "large_pastries", "leaves", "extra_large_bars", "large_bags", "dates", "medium_naans", "small_onions", "small_aubergines", "potato_skins", "batons", "sachets", "medium_bags", "small_omelettes_1_egg", "mini_macaroons", "small_beetroot", "large_aubergines", "mini_marshmallows", "small_mushrooms", "shrimps", "ice_cream_sandwiches", "fritters", "small_bags", "medium_omelettes_2_eggs", "bars", "small_naans", "medium_bars", "mini_pots", "scoops_of_powder", "rashers", "large_mushrooms", "medium_onions", "pilchards", "handfuls", "small_plantains", "steaks", "slices_1_8th_of_cake", "average_size_bags", "chillies", "jars", "large_eggs", "mini_flapjacks", "medium_biscuits", "meatballs", "ribs", "dough_balls", "rice_cakes", "medium_eggs", "single_measures", "large_handfuls", "medium_fillet_steaks", "share_bags", "level_tablespoons", "apricots", "medium_plantains", "nuts", "medium_bowls", "slices_1_12th_of_cake", "thick_slices", "large_portions", "mini_churros", "mini_Oreos", "sprigs", "mini_eclairs", "medium_avocados", "meringues", "cartons", "potatoes", "medium_sundaes", "large_biscuits", "tomatoes", "large_pots", "standard_size_bottles", "mini_bars", "pretzels", "small_pies", "peppers", "extra_large_triangles", "large_omelettes_4_eggs", "peaches", "small_biscuits", "small_flatbreads", "cloves", "mini_pastries", "prunes", "pies", "vine_leaves", "new_potatoes", "anchovies", "wings", "small_pots", "sausages", "double_measures", "cakes", "whole_rolls", "bags", "fruits", "onions", "large_bottles", "heaped_tablespoons", "medium_bananas", "whole_cakes", "mints", "medium_tubs", "large_fillets", "nuts_fruits", "individual_pots", "large_jars", "bunches", "small_fillets", "medium_aubergines", "scones", "dumplings", "small_tubs", "wedges", "medium_fillets", "small_hot_dogs", "spears", "whole_camemberts", "individual_tarts", "small_slices", "macaroons", "grapes", "small_tins", "pots_slices", "small_bananas", "large_naan_breads", "large_crackers", "small_portions", "large_squares", "slices", "mini_spring_rolls", "mange_tout", "onion_rings", "small_bowls", "small_gherkins", "large_gherkins", "very_thick_slices", "large_tubs", "kingsize_pots", "mooncakes", "small_bottles", "triangle_slices_half_a_piece_of_bread", "teaspoons", "large_steaks", "large_chops", "portions", "chocolates", "tablespoons", "eggs", "large_plantains", "small_crepes", "large_cobs", "mugs", "large_scones", "nectarines", "medium_slices", "slices_of_large_flatbread", "large_skewers", "small_chops", "large_kiwis", "tarts", "small_scones", "small_pancakes")

  val options = new ScallopConf(args) {
    val dbConfigDir = opt[String](required = true)
    val xmlDataDir = opt[String](required = true)
  }

  options.verify()

  val databaseConfig = DatabaseConfigChooser.chooseDatabaseConfiguration(options.dbConfigDir())
  val dataSource = getDataSource(databaseConfig)

  println("Parsing XML food data...")

  val foodsFromXml = FoodDef.parseXml(XML.load(options.xmlDataDir() + "/foods.xml"))
  val categoriesFromXml = CategoryDef.parseXml(XML.load(options.xmlDataDir() + "/categories.xml"))

  val categories = Categories(categoriesFromXml)

  val foodsAdminService = new FoodsAdminImpl(dataSource)

  val psmChoicesCache = scala.collection.mutable.Map[String, Boolean]("AGAV" -> false, "ACNW" -> false, "ADUK" -> false)


  /*
    Convert standard portion unit names to constant names (replace spaces with underscores)
  */
  def formatStandardUnits(psm: Seq[PortionSizeMethod]): Seq[PortionSizeMethod] =
    psm.map {
      m =>
        if (m.method == "standard-portion") {
          val params = m.parameters.map {
            par =>
              if (par.name.matches("unit[0-9]+-name"))
                PortionSizeMethodParameter(par.name, par.value.replaceAll("\\s", "_"))
              else
                par
          }

          m.copy(parameters = params)
        } else
          m
    }

  /*
    Check that all standard-portion methods in the list refer to known units
  */
  def verifyStandardUnits(psm: Seq[PortionSizeMethod]): Option[String] = {
    val stdp = psm.filter(_.method == "standard-portion")

    if (stdp.isEmpty)
      None
    else {
      val unitNames = stdp.flatMap {
        m =>
          m.parameters.filter(_.name.matches("unit[0-9]+-name")).map(_.value)
      }

      unitNames.find(name => !standardUnits.contains(name))
    }
  }

  def comparePortionSizeMethods(nzPsm: Seq[PortionSizeMethod], enPsm: Seq[PortionSizeMethod]): Boolean = {
    val p1 = nzPsm.map(m => m.copy(imageUrl = "", parameters = m.parameters.sortBy(_.name))).sortBy(_.method)
    val p2 = enPsm.map(m => m.copy(imageUrl = "", parameters = m.parameters.sortBy(_.name))).sortBy(_.method)

    p1 == p2
  }

  val stdin = new BufferedReader(new InputStreamReader(System.in))

  def getChoice(): Boolean = {

    print("Press 1 to use NZ portion size methods, 2 to use en_GB portion size methods: ")

    var choice = ""

    while (choice != "1" && choice != "2") {
      choice = stdin.readLine()
    }

    choice == "1"
  }

  foodsFromXml.foreach {
    nzFood =>
      println(s"Processing ${nzFood.code} (${nzFood.description})...")


      foodsAdminService.getFoodRecord(nzFood.code, "en_NZ") match {
        case Right(existingNzFood) if (existingNzFood.local.version.isDefined) =>
          println("\033[33mSKIPPING: food already updated\033[39m")
          println()
        case Left(UnexpectedDatabaseError(e)) => throw e
        case _ =>
          foodsAdminService.getFoodRecord(nzFood.code, "en_GB") match {
            case Right(enFood) =>
              println("  food exists in en_GB")

              val nutrientTableCodes: Map[String, String] = if (nzFood.nutrientTableCodes == enFood.local.nutrientTableCodes) {
                println("  food composition table codes match, inheriting en_GB codes")
                Map()
              } else {
                println("  food composition table codes do not match, using NZ codes")
                nzFood.nutrientTableCodes
              }

              val formattedNzPsm = formatStandardUnits(nzFood.portionSizeMethods)

              if (comparePortionSizeMethods(formattedNzPsm, enFood.local.portionSize)) {
                println("  portion size methods match")
                Seq[PortionSizeMethod]()
              } else {
                println("  \033[33mportion size methods do not match\033[39m")

                println("  NZ methods:")
                nzFood.portionSizeMethods.foreach(m => println("    " + m.toString))

                verifyStandardUnits(formattedNzPsm) match {
                  case Some(name) => println(s"   WARNING: $name is not a known standard unit name")
                  case _ => ()
                }

                println("  en_GB methods:")
                enFood.local.portionSize.foreach(m => println("    " + m.toString))
              }


              println("  updating local food record")

              foodsAdminService.getFoodRecord(nzFood.code, "en_NZ").right.flatMap {
                existingRecord =>
                  foodsAdminService.updateLocalFoodRecord(nzFood.code, LocalFoodRecordUpdate(existingRecord.local.version, None, false, nutrientTableCodes, Seq(), Seq(), Seq()), "en_NZ")
              } match {
                case Right(()) =>
                  println("\033[32mOK\033[39m")
                  println()
                case Left(e) =>
                  println("\033[101mERROR: " + e.exception.getMessage + "\033[39m")
                  println()
              }


            case Left(RecordNotFound(e)) =>
              println("  food does not exist in en_GB")

              val formattedNzPsm = formatStandardUnits(nzFood.portionSizeMethods)

              verifyStandardUnits(formattedNzPsm) match {
                case Some(name) => println(s"\033[101mSKIPPING: $name is not a known standard unit name, fix this and retry!\033[39m")
                case _ => {

                  println("  creating new food")

                  val result = for (_ <- foodsAdminService.createFood(NewMainFoodRecord(nzFood.code, nzFood.description, nzFood.groupCode, nzFood.attributes, categories.foodSuperCategories(nzFood.code), Seq("en_NZ"))).right;
                                    _ <- foodsAdminService.updateLocalFoodRecord(nzFood.code, LocalFoodRecordUpdate(None, None, false, nzFood.nutrientTableCodes, formattedNzPsm, Seq(), Seq()), "en_NZ").right)
                    yield ()

                  result match {
                    case Right(()) =>
                      println("\033[32mOK\033[39m")
                      println()
                    case Left(e) =>
                      println("\033[101mERROR: " + e.exception.getMessage + "\033[39m")
                      println()
                  }
                }
              }


            case Left(e) => throw e.exception
          }

      }


  }


  println()
  print("val psmChoicesCache = scala.collection.mutable.Map[String, Boolean](")
  print(psmChoicesCache.toSeq.map {
    case (code, choice) => "\"" + code + "\" -> " + choice.toString
  }.mkString(", "))
  println(")")

}
