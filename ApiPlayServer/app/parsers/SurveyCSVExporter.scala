package parsers

import java.io.{File, FileWriter}
import java.time.Duration
import java.time.format.DateTimeFormatter

import au.com.bytecode.opencsv.CSVWriter
import uk.ac.ncl.openlab.intake24.FoodGroupRecord
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{CustomDataScheme, LocalNutrientDescription}
import uk.ac.ncl.openlab.intake24.surveydata.ExportSubmission

import scala.collection.mutable

object SurveyCSVExporter {
  val MISSING_FOOD_KEY_DESCRIPTION = "missingFoodDescription"
  val MISSING_FOOD_KEY_PORTION_SIZE = "missingFoodPortionSize"
  val MISSING_FOOD_KEY_LEFTOVERS = "missingFoodLeftovers"

  def exportSurveySubmissions(dataScheme: CustomDataScheme, foodGroups: Map[Int, FoodGroupRecord], localNutrients: Seq[LocalNutrientDescription], submissions: Seq[ExportSubmission]): Either[String, File] = {
    val file = File.createTempFile("intake24-export-", ".csv")
    val writer = new CSVWriter(new FileWriter(file))
    try {
      val header = mutable.Buffer[String]()
      header.append("Survey ID", "User ID")
      header.append(dataScheme.userCustomFields.map(_.description): _*)
      header.append("Start time", "Submission time", "Time to complete")
      header.append(dataScheme.surveyCustomFields.map(_.description): _*)
      header.append("Meal ID", "Meal name")
      header.append(dataScheme.mealCustomFields.map(_.description): _*)
      header.append("Food ID", "Search term", "Intake24 food code", "Description (en)", "Description (local)", "Nutrient table name", "Nutrient table code",
        "Food group code", "Food group (en)", "Food group (local)", "Ready meal", "Brand")
      header.append(dataScheme.foodCustomFields.map(_.description): _*)
      header.append("Serving size (g/ml)", "Serving image", "Leftovers (g/ml)", "Leftovers image", "Portion size (g/ml)", "Reasonable amount")
      header.append("Missing food description", "Missing food portion size", "Missing food leftovers")
      header.append(localNutrients.map(_.description): _*)

      writer.writeNext(header.toArray)

      submissions.foreach {
        submission =>

          var mealId = 0

          submission.meals.foreach {
            meal =>
              var foodId = 0

              meal.foods.foreach {
                food =>
                  val row = mutable.Buffer[String]()
                  row.append(submission.id.toString, submission.userName)
                  dataScheme.userCustomFields.map(_.key).foreach(k => row.append(submission.userCustomData.getOrElse(k, "N/A")))
                  val timeToComplete = s"${Duration.between(submission.startTime, submission.endTime).getSeconds / 60} min"
                  row.append(DateTimeFormatter.ISO_INSTANT.format(submission.startTime), DateTimeFormatter.ISO_INSTANT.format(submission.endTime), timeToComplete)
                  dataScheme.surveyCustomFields.map(_.key).foreach(k => row.append(submission.surveyCustomData.getOrElse(k, "N/A")))
                  row.append(mealId.toString, meal.name)
                  dataScheme.mealCustomFields.map(_.key).foreach(k => row.append(meal.customData.getOrElse(k, "N/A")))

                  val foodGroupRecord = foodGroups.get(food.foodGroupId)
                  val foodGroupEnglishDescription = foodGroupRecord.map(_.main.englishDescription).getOrElse("N/A")
                  val foodGroupLocalDescription = foodGroupRecord.flatMap(_.local.localDescription).getOrElse("N/A")

                  row.append(foodId.toString, food.searchTerm, food.code, food.englishDescription, food.localDescription.getOrElse("N/A"),
                    food.nutrientTableCode, food.nutrientTableCode, food.foodGroupId.toString, foodGroupEnglishDescription, foodGroupLocalDescription,
                    food.isReadyMeal.toString, food.brand)

                  dataScheme.foodCustomFields.map(_.key).foreach(k => row.append(food.customData.getOrElse(k, "N/A")))

                  val servingWeight = food.portionSize.data.get("servingWeight").map(_.toDouble)
                  val leftoversWeight = food.portionSize.data.get("leftoversWeight").map(_.toDouble)

                  val portionWeight = for (sw <- servingWeight; lw <- leftoversWeight) yield sw - lw

                  row.append(servingWeight.map("%.2f".format(_)).getOrElse("N/A"), food.portionSize.data.getOrElse("servingImage", "N/A"),
                    leftoversWeight.map("%.2f".format(_)).getOrElse("N/A"), food.portionSize.data.getOrElse("leftoversImage", "N/A"),
                    portionWeight.map("%.2f".format(_)).getOrElse("N/A"), food.reasonableAmount.toString)

                  row.append(food.customData.getOrElse(MISSING_FOOD_KEY_DESCRIPTION, "N/A"))
                  row.append(food.customData.getOrElse(MISSING_FOOD_KEY_PORTION_SIZE, "N/A"))
                  row.append(food.customData.getOrElse(MISSING_FOOD_KEY_LEFTOVERS, "N/A"))


                  localNutrients.map(_.nutrientTypeId).foreach {
                    nutrientTypeId =>
                      row.append(food.nutrients.get(nutrientTypeId).map("%.2f".format(_)).getOrElse("N/A"))
                  }

                  writer.writeNext(row.toArray)

                  foodId += 1
              }

              mealId += 1
          }

      }
      Right(file)
    }
    catch {
      case e: Throwable => Left(s"""${e.getClass.getSimpleName}: ${e.getMessage}""")
    }
    finally {
      writer.close()
    }
  }
}
