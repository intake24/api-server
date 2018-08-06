package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation.newzealand

import java.io.FileReader

import com.opencsv.CSVReader
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.api.data.admin.{LocalFoodRecordUpdate, NewMainFoodRecord}
import uk.ac.ncl.openlab.intake24.api.data.{PortionSizeMethod, PortionSizeMethodParameter}
import uk.ac.ncl.openlab.intake24.errors.{RecordNotFound, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.foodsql.admin.{AsServedSetsAdminImpl, FoodsAdminImpl, GuideImageAdminImpl}
import uk.ac.ncl.openlab.intake24.foodsql.user.{AsServedSetsServiceImpl, GuideImageServiceImpl}
import uk.ac.ncl.openlab.intake24.foodxml.{Categories, CategoryDef, FoodDef}
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConfigChooser, DatabaseConnection, WarningMessage}

import scala.collection.JavaConverters._
import scala.language.reflectiveCalls
import scala.xml.XML

object ImportNewZealandFoods extends App with DatabaseConnection with WarningMessage {

  val knownStandardUnits = Set("whole_stuffed_peppers", "oysters", "large_pies", "pigs_in_blankets", "prawns", "small_steaks", "milles_feuilles", "small_avocados", "burritos", "fajitas", "small_cartons", "medium_parsnips", "medium_jars", "cherries", "mini_boxes", "slices_1_8th_of_pie", "punnets", "pancakes", "average_sized_dim_sum", "scallops", "tablets", "small_parsnips", "packets", "thin_slices", "rings", "medium_carrots", "panna_cottas", "medium_courgettes", "whole_small_quiches", "large_hot_dogs", "mozzarella_sticks", "kebabs", "large_tarts", "average_sized_flapjacks", "cannelloni_tubes", "mushrooms", "mini_fillets", "large_bananas", "small_packets", "large_carrots", "large_parsnips", "medium_beetroot", "multipack_bags", "medium_handfuls", "small_individual_tubs", "fingers", "standard_bags", "bagels", "yams", "sticks", "large_bowls", "large_pancakes", "lollipops", "crackers", "small_bars", "medium_cartons", "regular_churros", "artichoke_hearts", "sweets", "large_avocados", "medium_chops", "poppadums", "treble_measures", "leeks", "standard_boxes", "medium_portions", "mozarella_balls", "berries", "multipack_bottles", "small_squares", "medium_gherkins", "medium_steaks", "enchiladas", "whole_sausages", "large_cartons", "standard_size_sticks", "large_flapjacks", "mini_cobs", "chicken_livers", "sandwiches_made_with_two_slices_of_bread", "squares", "small_sticks", "standard_packs", "level_teaspoons", "hot_pots", "balls", "wafers", "large_spring_rolls", "fatty_cutties", "one_inch_pieces", "waffles", "medium_pots", "mini_eggs", "packs", "vol_au_vents", "sheets", "cups", "marshmallows", "small_eggs", "small_jars", "whole_large_pies", "pouches", "profiteroles", "tins", "large_truffles", "straws", "individual_packs", "aubergine", "mini_skewers", "whole_radishes", "pieces", "falafels", "sachets_made_up_with_milk", "small_carrots", "bacon_and_cheese_grills", "rolls", "standard_bars", "individual_pies", "mini_snack_packs", "fillets", "small_truffles", "large_bars", "small_sundaes", "biscuits", "mussels", "beetroot", "whole_large_quiches", "chocolate_oranges", "blocks", "large_slices", "small_souffles", "large_share_bags", "pots", "average_tubs___bags", "olives", "whole_large_tarts", "small_kiwis", "stuffed_peppers_half_a_pepper", "cassavas", "tubs", "mini_tubs", "sprouts", "extra_large_eggs", "segments", "small_handfuls", "small_individual_pavlovas", "medium_kiwis", "shots", "heaped_teaspoons", "medium_scones", "bites", "pastries", "large_onions", "average_spring_rolls", "large_pastries", "leaves", "extra_large_bars", "large_bags", "dates", "medium_naans", "small_onions", "small_aubergines", "potato_skins", "batons", "sachets", "medium_bags", "small_omelettes_1_egg", "mini_macaroons", "small_beetroot", "large_aubergines", "mini_marshmallows", "small_mushrooms", "shrimps", "ice_cream_sandwiches", "fritters", "small_bags", "medium_omelettes_2_eggs", "bars", "small_naans", "medium_bars", "mini_pots", "scoops_of_powder", "rashers", "large_mushrooms", "medium_onions", "pilchards", "handfuls", "small_plantains", "steaks", "slices_1_8th_of_cake", "average_size_bags", "chillies", "jars", "large_eggs", "mini_flapjacks", "medium_biscuits", "meatballs", "ribs", "dough_balls", "rice_cakes", "medium_eggs", "single_measures", "large_handfuls", "medium_fillet_steaks", "share_bags", "level_tablespoons", "apricots", "medium_plantains", "nuts", "medium_bowls", "slices_1_12th_of_cake", "thick_slices", "large_portions", "mini_churros", "mini_Oreos", "sprigs", "mini_eclairs", "medium_avocados", "meringues", "cartons", "potatoes", "medium_sundaes", "large_biscuits", "tomatoes", "large_pots", "standard_size_bottles", "mini_bars", "pretzels", "small_pies", "peppers", "extra_large_triangles", "large_omelettes_4_eggs", "peaches", "small_biscuits", "small_flatbreads", "cloves", "mini_pastries", "prunes", "pies", "vine_leaves", "new_potatoes", "anchovies", "wings", "small_pots", "sausages", "double_measures", "cakes", "whole_rolls", "bags", "fruits", "onions", "large_bottles", "heaped_tablespoons", "medium_bananas", "whole_cakes", "mints", "medium_tubs", "large_fillets", "nuts_fruits", "individual_pots", "large_jars", "bunches", "small_fillets", "medium_aubergines", "scones", "dumplings", "small_tubs", "wedges", "medium_fillets", "small_hot_dogs", "spears", "whole_camemberts", "individual_tarts", "small_slices", "macaroons", "grapes", "small_tins", "pots_slices", "small_bananas", "large_naan_breads", "large_crackers", "small_portions", "large_squares", "slices", "mini_spring_rolls", "mange_tout", "onion_rings", "small_bowls", "small_gherkins", "large_gherkins", "very_thick_slices", "large_tubs", "kingsize_pots", "mooncakes", "small_bottles", "triangle_slices_half_a_piece_of_bread", "teaspoons", "large_steaks", "large_chops", "portions", "chocolates", "tablespoons", "eggs", "large_plantains", "small_crepes", "large_cobs", "mugs", "large_scones", "nectarines", "medium_slices", "slices_of_large_flatbread", "large_skewers", "small_chops", "large_kiwis", "tarts", "small_scones", "small_pancakes")

  val options = new ScallopConf(args) {
    val dbConfigDir = opt[String](required = true)
    val xmlDataDir = opt[String](required = true)
    val codesCsv = opt[String](required = true)
  }

  options.verify()

  val databaseConfig = DatabaseConfigChooser.chooseDatabaseConfiguration(options.dbConfigDir())
  val dataSource = getDataSource(databaseConfig)

  println("Parsing XML food data...")

  val foodsFromXml = FoodDef.parseXml(XML.load(options.xmlDataDir() + "/foods.xml"))
  val categoriesFromXml = CategoryDef.parseXml(XML.load(options.xmlDataDir() + "/categories.xml"))

  val categories = Categories(categoriesFromXml)

  val foodsAdminService = new FoodsAdminImpl(dataSource)
  val asServedService = new AsServedSetsAdminImpl(dataSource, new AsServedSetsServiceImpl(dataSource))
  //  val guideImageService = new GuideImageAdminImpl(dataSource, new GuideImageServiceImpl(dataSource))

  val knownAsServedSets = asServedService.listAsServedSets().right.get.keySet
  //  val knownGuideImages = guideImageService.listGuideImages().right.get.map(_.id).toSet

  println("Known as served sets: " + knownAsServedSets.mkString(", "))
  //  println("Known guide images: " + knownGuideImages.mkString(", "))

  val replacementCodes = {
    val reader = new CSVReader(new FileReader(options.codesCsv()))

    val result = reader.readAll().asScala.foldLeft(Map[String, Map[String, String]]()) {
      (m, row) =>
        val foodCode = row(0)

        val nzCode = row(4).trim
        val ndnsCode = row(5).trim

        if (nzCode.nonEmpty)
          m + (foodCode -> Map("NZ" -> nzCode))
        else if (ndnsCode.nonEmpty)
          m + (foodCode -> Map("NDNS" -> ndnsCode))
        else
          m + (foodCode -> Map())

    }

    reader.close()

    result
  }

  val replacementUnits = Map(
    "standard_oatcakes" -> "biscuits",
    "large_oatcakes" -> "large_biscuits",
    "small_handful" -> "small_handfuls",
    "medium_handful" -> "medium_handfuls",
    "large_handful" -> "large_handfuls",
    "tablespoon" -> "level_tablespoons",
    "medallions" -> "small_steaks",
    "Paua" -> "tablespoons",
    "crispbreads" -> "crackers",
    "Feijoa" -> "fruits",
    "muffins" -> "cakes",
    "scoops" -> "tablespoons",
    "spiced_buns" -> "rolls",
    "Squiggles" -> "biscuits",
    "dessert_pots" -> "individual_pots",
    "scoops" -> "tablespoons",
    "small_chops_(escalopes)" -> "small_steaks",
    "small_pork_medallions" -> "small_steaks",
    "medium_pork_medallions" -> "medium_steaks",
    "large_pork_medallions" -> "large_steaks",
    "small_steaks_(escalopes)" -> "small_steaks",
    "chestnuts" -> "nuts",
    "shrimp" -> "shrimps",
    "250g_slab" -> "large_bars",
    "150g_bar_" -> "large_bars",
    "Toheroa" -> "tablespoons",
    "large_whole_pies" -> "large_pies",
    "punnet" -> "punnets",
    "average_tubs" -> "tubs"
  )

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
  def verifyStandardUnits(params: Seq[PortionSizeMethodParameter]) =
    params.filter(_.name.matches("unit[0-9]+-name")).map(_.value).foreach {
      name =>
        if (!knownStandardUnits.contains(name))
          println(s"    \u001b[101mUndefined standard unit: $name \u001b[39m")
    }

  def verifyAsServed(params: Seq[PortionSizeMethodParameter]) = {
    def checkSet(id: String) =
      if (!knownAsServedSets.contains(id))
        println(s"    \u001b[101mUndefined as served set: $id \u001b[39m")


    val servingSet = params.find(_.name == "serving-image-set").get.value
    val leftoversSet = params.find(_.name == "leftovers-image-set").map(_.value)

    checkSet(servingSet)


    leftoversSet match {
      case Some(set) => checkSet(set)
      case None => ()
    }
  }

  def verifyGuideImage(params: Seq[PortionSizeMethodParameter]) = {
    val id = params.find(_.name == "guide-image-id").get.value

    //    if (!knownGuideImages.contains(id))
    //      println(s"    \u001b[101mUndefined guide image: $id \u001b[39m")
  }


  def verifyPortionSizeMethods(psm: Seq[PortionSizeMethod]): Unit = {
    psm.foreach {
      m =>
        m.method match {
          case "standard-portion" => verifyStandardUnits(m.parameters)
          case "as-served" => verifyAsServed(m.parameters)
          case "guide-image" => verifyGuideImage(m.parameters)
          case _ => ()
        }
    }
  }

  case class StandardPortionUnit(unitId: String, weight: Double, omitFoodDescription: Boolean)

  def parseStandardPortionParameters(params: Seq[PortionSizeMethodParameter]): Seq[StandardPortionUnit] = {
    val count = params.find(_.name == "units-count").get.value.toInt

    Range(0, count).map {
      unitIndex =>
        val unitId = params.find(_.name == s"unit$unitIndex-name").get.value
        val weight = params.find(_.name == s"unit$unitIndex-weight").get.value.toDouble
        val omitFoodDescription = params.find(_.name == s"unit$unitIndex-omit-food-description").get.value.toBoolean

        StandardPortionUnit(unitId, weight, omitFoodDescription)
    }
  }

  def toStandardPortionParameters(units: Seq[StandardPortionUnit]): Seq[PortionSizeMethodParameter] = {
    val count = PortionSizeMethodParameter("units-count", units.size.toString)

    units.zipWithIndex.foldLeft(Seq(count)) {
      case (result, (StandardPortionUnit(unitId, weight, omitFoodDescription), index)) =>
        PortionSizeMethodParameter(s"unit$index-name", unitId) +:
          PortionSizeMethodParameter(s"unit$index-weight", weight.toString) +:
          PortionSizeMethodParameter(s"unit$index-omit-food-description", omitFoodDescription.toString) +: result
    }
  }

  val modifiedStandardPortion = collection.mutable.HashSet[String]()

  case class UndefinedStandardUnitRow(foodCode: String, foodDescription: String, unitId: String)

  val undefinedStandardUnits = collection.mutable.Buffer[UndefinedStandardUnitRow]()

  def fixStandardUnits(foodCode: String, psm: Seq[PortionSizeMethod]): Seq[PortionSizeMethod] =
    psm.map {
      m =>
        if (m.method == "standard-portion") {

          val units = parseStandardPortionParameters(m.parameters)

          var log = false

          val replacedUnits = units.map {
            unit =>
              if (!knownStandardUnits.contains(unit.unitId)) {
                println(s"Replacing ${unit.unitId} with ${replacementUnits(unit.unitId)}")

                modifiedStandardPortion += foodCode
                log = true

                unit.copy(unitId = replacementUnits(unit.unitId))
              }
              else
                unit
          }

          val seen = collection.mutable.HashSet[String]()

          val uniqueUnits = replacedUnits.foldLeft(Seq[StandardPortionUnit]()) {
            case (r, unit) =>
              if (seen.contains(unit.unitId)) {
                log = true
                modifiedStandardPortion += foodCode
                r
              }
              else {
                seen += unit.unitId
                unit +: r
              }
          }

          if (log) {
            println("  standard units changed")
            println("    original: " + units.mkString(", "))
            println("    modified: " + uniqueUnits.mkString(", "))
          }

          m.copy(parameters = toStandardPortionParameters(uniqueUnits))
        } else
          m
    }

  def comparePortionSizeMethods(nzPsm: Seq[PortionSizeMethod], enPsm: Seq[PortionSizeMethod]): Boolean = {
    val p1 = nzPsm.map(m => m.copy(imageUrl = "", parameters = m.parameters.sortBy(_.name))).sortBy(_.method)
    val p2 = enPsm.map(m => m.copy(imageUrl = "", parameters = m.parameters.sortBy(_.name))).sortBy(_.method)

    p1 == p2
  }


  foodsFromXml.foreach {
    nzFood =>
      println(s"Processing ${nzFood.code} (${nzFood.description})...")

      foodsAdminService.getFoodRecord(nzFood.code, "en_NZ") match {
        case Right(existingNzFood) if (existingNzFood.local.version.isDefined) =>
          println("\u001b[33mSKIPPING: food already updated\u001b[39m")
          println()
        case Left(UnexpectedDatabaseError(e)) => throw e
        case _ =>
          foodsAdminService.getFoodRecord(nzFood.code, "en_GB") match {
            case Right(enFood) =>
              println("  main food record exists")

              val nutrientTableCodes: Map[String, String] = if (nzFood.nutrientTableCodes == enFood.local.nutrientTableCodes) {
                println("  food composition table codes match, inheriting en_GB codes")
                Map()
              } else {
                println("  food composition table codes do not match, using NZ codes")

                replacementCodes.get(nzFood.code) match {
                  case Some(codes) =>
                    println("     using replacement food composition table code from CSV")
                    codes
                  case None =>
                    nzFood.nutrientTableCodes
                }
              }

              val formattedNzPsm = formatStandardUnits(nzFood.portionSizeMethods)

              if (comparePortionSizeMethods(formattedNzPsm, enFood.local.portionSize)) {
                println("  portion size methods match")
                Seq[PortionSizeMethod]()
              } else {
                println("  \u001b[33mportion size methods do not match -- using UK methods\u001b[39m")

                println("  NZ methods:")
                nzFood.portionSizeMethods.foreach(m => println("    " + m.toString))

                println("  en_GB methods:")
                enFood.local.portionSize.foreach(m => println("    " + m.toString))

                Seq[PortionSizeMethod]()
              }

              println("  updating local food record")

              foodsAdminService.getFoodRecord(nzFood.code, "en_NZ").right.flatMap {
                existingRecord =>
                  foodsAdminService.updateLocalFoodRecord(nzFood.code, LocalFoodRecordUpdate(existingRecord.local.version, None, false, nutrientTableCodes, Seq(), Seq(), Seq()), "en_NZ")
              } match {
                case Right(()) =>
                  println("\u001b[32mOK\u001b[39m")
                  println()
                case Left(e) =>
                  println("\u001b[101mERROR: " + e.exception.getMessage + "\u001b[39m")
                  println()
              }


            case Left(RecordNotFound(e)) =>
              println("  main food record does not exist")

              val formattedNzPsm = formatStandardUnits(nzFood.portionSizeMethods)

              val fixedStandardUnits = fixStandardUnits(nzFood.code, formattedNzPsm)

              verifyPortionSizeMethods(fixedStandardUnits)

              println("  creating new food")

              val result = for (_ <- foodsAdminService.createFood(NewMainFoodRecord(nzFood.code, nzFood.description, nzFood.groupCode, nzFood.attributes, categories.foodSuperCategories(nzFood.code), Seq("en_NZ"))).right;
                                _ <- foodsAdminService.updateLocalFoodRecord(nzFood.code, LocalFoodRecordUpdate(None, Some(nzFood.description), false, nzFood.nutrientTableCodes, fixedStandardUnits, Seq(), Seq()), "en_NZ").right)
                yield ()

              result match {
                case Right(()) =>
                  println("\u001b[32mOK\u001b[39m")
                  println()
                case Left(e) =>
                  println("\u001b[101mERROR: " + e.exception.getMessage + "\u001b[39m")
                  println()
              }

            /* case x => {
               println(s"\u001b[101mSKIPPING: undefined standard units: ${x.mkString(", ")}, fix this and retry!\u001b[39m")
               undefinedStandardUnits ++= x.map {
                 unitId =>
                   UndefinedStandardUnitRow(nzFood.code, nzFood.description, unitId)
               }
             }*/



            case Left(e) => throw e.exception
          }

      }
  }

  println(modifiedStandardPortion.mkString(", "))
}
