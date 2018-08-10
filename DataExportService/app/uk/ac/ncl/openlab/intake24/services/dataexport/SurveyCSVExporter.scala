package uk.ac.ncl.openlab.intake24.services.dataexport

import java.io.{File, FileWriter, IOException}
import java.time.Duration
import java.time.format.DateTimeFormatter

import au.com.bytecode.opencsv.CSVWriter
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.FoodGroupRecord
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{CustomDataScheme, ExportSubmission, LocalNutrientDescription}

import scala.collection.mutable

object SurveyCSVExporter {

  val logger = LoggerFactory.getLogger(SurveyCSVExporter.getClass)

  val missingFoodCode = "MISSING"

  val noData = "N/A"

  @throws[IOException]
  def createTempFile(): File = File.createTempFile("intake24-export-", ".csv")

  @throws[IOException]
  def writeHeader(fileWriter: FileWriter, csvWriter: CSVWriter, dataScheme: CustomDataScheme, localNutrients: Seq[LocalNutrientDescription], insertBOM: Boolean): Unit = {
    if (insertBOM) {
      fileWriter.append('\ufeff')
    }

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

    csvWriter.writeNext(header.toArray)

    Right(())
  }


  @throws[IOException]
  def writeSubmissionsBatch(csvWriter: CSVWriter, dataScheme: CustomDataScheme, foodGroups: Map[Int, FoodGroupRecord], localNutrients: Seq[LocalNutrientDescription], submissions: Seq[ExportSubmission]): Unit =
    submissions.foreach {
      submission =>

        var mealId = 1

        submission.meals.foreach {
          meal =>
            var foodId = 1

            def appendSubmissionAndMealColumns(row: mutable.Buffer[String]): Unit = {
              row.append(submission.id.toString, submission.userAlias.getOrElse(noData))
              dataScheme.userCustomFields.map(_.key).foreach(k => row.append(submission.userCustomData.getOrElse(k, noData)))
              val timeToComplete = s"${Duration.between(submission.startTime, submission.endTime).getSeconds / 60} min"
              row.append(DateTimeFormatter.ISO_INSTANT.format(submission.startTime), DateTimeFormatter.ISO_INSTANT.format(submission.endTime), timeToComplete)
              dataScheme.surveyCustomFields.map(_.key).foreach(k => row.append(submission.surveyCustomData.getOrElse(k, noData)))
              row.append(mealId.toString, meal.name)
              dataScheme.mealCustomFields.map(_.key).foreach(k => row.append(meal.customData.getOrElse(k, noData)))
            }

            meal.foods.foreach {
              food =>
                val row = mutable.Buffer[String]()

                appendSubmissionAndMealColumns(row)

                val foodGroupRecord = foodGroups.get(food.foodGroupId)
                val foodGroupEnglishDescription = foodGroupRecord.map(_.main.englishDescription).getOrElse(noData)
                val foodGroupLocalDescription = foodGroupRecord.flatMap(_.local.localDescription).getOrElse(noData)

                row.append(foodId.toString, food.searchTerm, food.code, food.englishDescription, food.localDescription.getOrElse(noData),
                  food.nutrientTableId, food.nutrientTableCode, food.foodGroupId.toString, foodGroupEnglishDescription, foodGroupLocalDescription,
                  food.isReadyMeal.toString, food.brand)

                dataScheme.foodCustomFields.map(_.key).foreach(k => row.append(food.customData.getOrElse(k, noData)))

                row.append(f"${food.portionSize.servingWeight}%.2f", food.portionSize.data.getOrElse("servingImage", noData),
                  f"${food.portionSize.leftoversWeight}%.2f", food.portionSize.data.getOrElse("leftoversImage", noData),
                  f"${food.portionSize.portionWeight}%.2f", food.reasonableAmount.toString)

                row.append(noData, noData, noData)

                localNutrients.map(_.nutrientTypeId).foreach {
                  nutrientTypeId =>
                    row.append(food.nutrients.get(nutrientTypeId).map("%.2f".format(_)).getOrElse(noData))
                }

                csvWriter.writeNext(row.toArray)

                foodId += 1
            }

            meal.missingFoods.foreach {
              missingFood =>
                val row = mutable.Buffer[String]()

                appendSubmissionAndMealColumns(row)

                row.append(foodId.toString, missingFood.name, missingFoodCode, noData, noData,
                  noData, noData, noData, noData, noData, noData, missingFood.brand)

                dataScheme.foodCustomFields.map(_.key).foreach(_ => row.append(noData))


                row.append(noData, noData,
                  noData, noData,
                  noData, noData)

                row.append(if (missingFood.description.isEmpty) noData else missingFood.description)
                row.append(if (missingFood.portionSize.isEmpty) noData else missingFood.portionSize)
                row.append(if (missingFood.leftovers.isEmpty) noData else missingFood.leftovers)


                localNutrients.map(_.nutrientTypeId).foreach {
                  _ =>
                    row.append(noData)
                }

                csvWriter.writeNext(row.toArray)

                foodId += 1
            }

            mealId += 1
        }
    }


  def exportSurveySubmissions(dataScheme: CustomDataScheme, foodGroups: Map[Int, FoodGroupRecord], localNutrients: Seq[LocalNutrientDescription], submissions: Seq[ExportSubmission], insertBOM: Boolean): Either[String, File] = {

    var csvWriter: CSVWriter = null
    var fileWriter: FileWriter = null

    try {
      val file = createTempFile()

      fileWriter = new FileWriter(file)
      csvWriter = new CSVWriter(fileWriter)

      writeHeader(fileWriter, csvWriter, dataScheme, localNutrients, insertBOM)
      writeSubmissionsBatch(csvWriter, dataScheme, foodGroups, localNutrients, submissions)

      Right(file)

    } catch {
      case e: Throwable => Left(s"""${e.getClass.getSimpleName}: ${e.getMessage}""")
    } finally {
      try {
        if (csvWriter != null)
          csvWriter.close()
        if (fileWriter != null)
          fileWriter.close()
      } catch {
        case e: Throwable => logger.error("Failed to close file writer", e)
      }
    }
  }

}
