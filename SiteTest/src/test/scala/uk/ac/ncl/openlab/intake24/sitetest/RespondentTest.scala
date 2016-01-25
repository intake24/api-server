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

package uk.ac.ncl.openlab.intake24.sitetest

import org.scalatest.FunSuite
import org.scalatest.selenium.WebBrowser
import org.openqa.selenium.chrome.ChromeDriver
import org.scalatest.Matchers._
import org.scalatest.BeforeAndAfterAll
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.support.ui.Wait
import org.openqa.selenium.support.ui.ExpectedCondition
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.Keys
import org.scalatest.selenium.WebBrowser.TextField
import scala.collection.JavaConversions._
import org.openqa.selenium.NotFoundException
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.WebDriver
import java.util.concurrent.TimeUnit
import org.openqa.selenium.StaleElementReferenceException

class RespondentTest extends FunSuite with BeforeAndAfterAll with WebBrowser {

  System.setProperty("webdriver.chrome.driver", "/usr/lib/chromium-browser/chromedriver")

  implicit val webDriver = new ChromeDriver

  val actions = new Actions(webDriver)

  val host = "https://intake24.co.uk"

  var currentPrompt: WebElement = null
  var promptCount = 0

  override def beforeAll() {
    webDriver.manage().window().setSize(new org.openqa.selenium.Dimension(1000, 1000));
  }

  override def afterAll() {
    // quit()
  }

  def waitForElement(q: String) = new WebDriverWait(webDriver, 10).until(ExpectedConditions.elementToBeClickable(By.cssSelector(q)))

  def waitForNextPrompt(test: String) = {

    // Wait for the current prompt to be removed
    if (currentPrompt != null) {
      new WebDriverWait(webDriver, 10).until(new ExpectedCondition[Boolean]() {
        def apply(driver: WebDriver) =
          try {
            currentPrompt.isDisplayed() // trigger StaleElementException
            false
          } catch {
            case e: StaleElementReferenceException => true
          }
      })
    }

    currentPrompt = webDriver.findElement(By.cssSelector(".intake24-active-prompt"))
    promptCount += 1
  }

  def setCounter(target: String, textElement: WebElement, upArrow: WebElement, downArrow: WebElement) = {
    if (textElement.getText < target)
      while (textElement.getText() != target) {
        upArrow.click()
      }
    else
      while (textElement.getText() != target) {
        downArrow.click()
      }
  }

  def clickButtonByLabel(label: String) = {
    val elements = webDriver.findElements(By.cssSelector(".intake24-active-prompt .intake24-button"))

    // println(s"Looking for: $label, buttons available: ${elements.toSeq.map(_.getText).mkString(", ")}")

    elements.find(_.getText.toLowerCase() == label.toLowerCase()) match {
      case Some(element) => actions.moveToElement(element).click().perform()
      case None => null
    }
  }

  def clickButtonByLabelAndWait(label: String) = {
    clickButtonByLabel(label)
    waitForNextPrompt("Click button")
  }

  def nextPrompt = {
    val continueButton = webDriver.findElementByCssSelector(".intake24-active-prompt .intake24-green-button")
    actions.moveToElement(continueButton).click().perform()
    waitForNextPrompt("Next prompt")
  }

  def selectFood(desc: String) = {
    val e = waitForElement(".intake24-active-prompt #intake24-food-browser-foods-container")

    e.findElements(By.cssSelector(".intake24-food-browser-food")).find(_.getText.toLowerCase() == desc.toLowerCase()) match {
      case Some(element) => {
        actions.moveToElement(element).click().perform()

        waitForNextPrompt("Select food")
      }
      case None => throw new NotFoundException(desc)
    }
  }

  def browseCategory(desc: String) = {
    val e = waitForElement(".intake24-active-prompt #intake24-food-browser-categories-container")

    e.findElements(By.cssSelector(".intake24-food-browser-category")).find(_.getText.toLowerCase() == desc.toLowerCase()) match {
      case Some(element) => {
        actions.moveToElement(element).click().perform()
      }
      case None => throw new NotFoundException(desc)
    }
  }

  def clickOnImageMap(offsetX: Double, offsetY: Double) = {
    val el = waitForElement("#intake24-guide-image-map")
    val size = el.getSize

    val height = size.getHeight
    val width = size.getWidth

    val xOffset = (width.toDouble * offsetX).toInt
    val yOffset = (height.toDouble * offsetY).toInt

    actions.moveToElement(el, xOffset, yOffset).click().perform()

    waitForNextPrompt("Image map")
  }

  def setTime(hours: String, minutes: String) = {
    val hoursCounter = waitForElement(".intake24-active-prompt #intake24-time-question-hours .counterTextBox")

    val hoursUpArrow = waitForElement(".intake24-active-prompt #intake24-time-question-hours .counterIncButton")

    val hoursDownArrow = waitForElement(".intake24-active-prompt #intake24-time-question-hours .counterDecButton")

    val minutesCounter = waitForElement(".intake24-active-prompt #intake24-time-question-minutes .counterTextBox")

    val minutesUpArrow = waitForElement(".intake24-active-prompt #intake24-time-question-minutes .counterIncButton")

    val minutesDownArrow = waitForElement(".intake24-active-prompt #intake24-time-question-minutes .counterDecButton")

    setCounter(hours, hoursCounter, hoursUpArrow, hoursDownArrow)

    setCounter(minutes, minutesCounter, minutesUpArrow, minutesDownArrow)
  }

  def setFracCounter(whole: String, fraction: String) = {
    val wholeCounter = waitForElement(".intake24-active-prompt #intake24-quantity-prompt-whole-counter .counterTextBox")

    val wholeUpArrow = waitForElement(".intake24-active-prompt #intake24-quantity-prompt-whole-counter .counterIncButton")

    val wholeDownArrow = waitForElement(".intake24-active-prompt #intake24-quantity-prompt-whole-counter .counterDecButton")

    val fracCounter = waitForElement(".intake24-active-prompt #intake24-quantity-prompt-frac-counter .counterTextBox")

    val fracUpArrow = waitForElement(".intake24-active-prompt #intake24-quantity-prompt-frac-counter .counterIncButton")

    val fracDownArrow = waitForElement(".intake24-active-prompt #intake24-quantity-prompt-frac-counter .counterDecButton")

    setCounter(whole, wholeCounter, wholeUpArrow, wholeDownArrow)

    setCounter(fraction, fracCounter, fracUpArrow, fracDownArrow)

  }

  def choosePortionSizeMethod(index: Int) = {
    val panel = waitForElement("#intake24-choose-portion-panel")
    panel.findElements(By.cssSelector(".intake24-choose-portion-label")).get(index).click()

    waitForNextPrompt("Choose PSM")
  }

  def addFood(description: String) = {
    val foodField = new TextField(waitForElement(".intake24-active-prompt #intake24-food-list input.intake24-food-list-textbox-new-item"))

    foodField.value = description
    foodField.underlying.sendKeys(Keys.ENTER)
  }

  def addIngredient(description: String) = {
    val foodField = new TextField(waitForElement(".intake24-active-prompt #intake24-ingredient-list input.intake24-food-list-textbox-new-item"))

    foodField.value = description
    foodField.underlying.sendKeys(Keys.ENTER)
  }

  def addDrink(description: String) = {
    val drinkField = new TextField(waitForElement(".intake24-active-prompt #intake24-drink-list input.intake24-food-list-textbox-new-item"))

    drinkField.value = description
    drinkField.underlying.sendKeys(Keys.ENTER)
  }

  def setSlidingScale(scalePercentage: Double) = {
    val el = waitForElement(".intake24-active-prompt #intake24-sliding-scale-slider")
    val size = el.getSize
    val height = size.getHeight
    val width = size.getWidth

    val yOffset = (height.toDouble * (1.0 - scalePercentage)).toInt
    val xOffset = (width.toDouble * 0.5).toInt

    actions.moveToElement(el, xOffset, yOffset)
    actions.click()
    actions.perform()
  }

  def chooseBrand(description: String) = {
   clickElementByLabel(description)
   nextPrompt
  }

  def clickElementByLabel(description: String) = {
    val el = webDriver.findElementsByCssSelector(".intake24-active-prompt label")
    el.find(_.getText.toLowerCase() == description.toLowerCase()) match {
      case Some(element) => {
        val e = webDriver.findElementById(element.getAttribute("for"))
        actions.moveToElement(e).click().perform()
      }
      case None => throw new NotFoundException(description)
    }
  }
  
  test("Open landing page") {
    go to host
    pageTitle should be("Intake24 | Home")
  }
  
  test("Set debug session variables") {
    webDriver.getSessionStorage.setItem("intake24-disable-auto-scroll", "true")
    webDriver.getSessionStorage.setItem("intake24-prompt-anim-duration", "100")
  }

  test("Log into demo survey") {
    go to host + "/surveys/demo/?genUser"
    click on waitForElement(".intake24-green-button")
  }

  test("Proceed from welcome page") {
    click on waitForElement(".intake24-green-button")
  }

  test("Confirm breakfast") {
    setTime("08", "00")

    nextPrompt
  }

  test("Enter breakfast foods") {
    addFood("toast")
    addDrink("tea")
    addDrink("juice")

    nextPrompt
    setTime("14", "00")
  }

  test("Confirm early snack") {
    setTime("14", "00")

    nextPrompt
  }

  test("Enter early snack foods") {
    addFood("chips")

    nextPrompt
  }

  test("Confirm lunch") {
    setTime("14", "30")

    nextPrompt
  }

  test("Enter lunch foods") {
    addFood("tuna and cheese salad")

    nextPrompt
  }

  // Afternoon snack/drink: Enter 16:00 add in pizza, cereal, strawberries and tea

  test("Confirm arternoon snack") {
    setTime("16", "00")

    nextPrompt
  }

  test("Enter afternoon snack foods") {
    addFood("pizza")
    addFood("cereal")
    addFood("strawberries")
    addDrink("tea")

    nextPrompt
  }

  //"Evening meal: Enter 19:00 add in parmo and parmo (not a typo, enter it twice!) and coffee"

  test("Confirm evening meal") {
    setTime("19", "00")

    nextPrompt
  }

  test("Enter evening foods") {
    addFood("parmo")
    addFood("parmo")
    addDrink("coffee")

    nextPrompt
  }

  // "Late snack: I did not have a late snack"

  test("Delete late snack") {
    click on waitForElement(".intake24-active-prompt #intake24-time-question-skip-button")
  }

  // Choose ‘toast, white bread’. Click an image. Click on arrow to say how many
  // slices you had e.g. 2

  test("Choose 'toast, white bread'") {
    selectFood("Toast, white bread")
  }

  test("Click on guide image for bread") {
    clickOnImageMap(0.83, 0.58)
  }

  test("Set bread count") {
    setFracCounter("2", "0")

    nextPrompt
  }

  // Click ‘tea’. Click ‘in a mug’. Click image ‘1’ and fill up to ~75ml using slider. Click
  // ‘it was that full’. Click ‘no’ to show you didn’t leave any.

  test("Tea") {
    selectFood("Tea")
    choosePortionSizeMethod(0)
    clickOnImageMap(0.24, 0.29)
    setSlidingScale(0.328)
    nextPrompt
    clickButtonByLabelAndWait("No")
  }

  test("Orange juice") {
    selectFood("Fresh orange juice, from concentrate")
    choosePortionSizeMethod(0)
    clickOnImageMap(0.88, 0.68)
    setSlidingScale(0.37)
    nextPrompt
    clickButtonByLabelAndWait("No")
    chooseBrand("Sainsbury's organics")
  }

  test("Confirm butter on toast") {
    clickButtonByLabel("Yes, I had some")
    browseCategory("Butter")
    selectFood("Butter, unsalted")
  }

  test("Reject jam on toast") {
    clickButtonByLabelAndWait("No, I did not")
  }

  test("Butter portion size") {
    choosePortionSizeMethod(1)
    nextPrompt
  }

  test("Confirm sugar in tea") {
    clickButtonByLabel("Yes, I had some")
    selectFood("White sugar")
  }

  test("Reject milk in tea") {
    clickButtonByLabelAndWait("No, I did not")
  }

  test("Sugar portion size") {
    choosePortionSizeMethod(0)
    clickOnImageMap(0.35, 0.52)
    nextPrompt
  }

  test("Chips portion size") {
    selectFood("Oven chips")
    choosePortionSizeMethod(3)
    nextPrompt
    clickButtonByLabelAndWait("No")
    clickButtonByLabelAndWait("No, I did not")
  }

  test("Confirm no drink") {
    clickButtonByLabelAndWait("I did not have a drink")
  }

  test("Build salad") {
    selectFood("Add your own salad »")
    browseCategory("Vegetables")
    selectFood("Lettuce")
    browseCategory("Vegetables")
    browseCategory("Peppers")
    selectFood("Red peppers, raw")
    browseCategory("Fish & fish products")
    browseCategory("Canned fish")
    selectFood("Tuna mayo")
    browseCategory("Cheese & cheese products")
    browseCategory("Hard cheese")
    selectFood("Cathedral city cheese")
    clickButtonByLabelAndWait("I did not have any other ingredients")
    clickButtonByLabelAndWait("I did not have any sauce/dressing")
  }

  test("Salad ingredients portion sizes") {
    choosePortionSizeMethod(0)
    nextPrompt
    clickButtonByLabelAndWait("No")
    setFracCounter("0", "½")
    nextPrompt
    choosePortionSizeMethod(0)
    nextPrompt
    clickButtonByLabelAndWait("No")
    choosePortionSizeMethod(2)
    nextPrompt
    clickButtonByLabelAndWait("No")
    setFracCounter("1", "0")
    nextPrompt
  }

  test("Save salad recipe") {
    val foodField = new TextField(waitForElement(".intake24-active-prompt input"))
    foodField.value = "Tuna and cheese salad"
    nextPrompt
  }

  test("Confirm no drinks") {
    clickButtonByLabelAndWait("I did not have a drink")
  }

  test("Pizza") {
    selectFood("Vegetable pizza (includes homemade)")
    clickOnImageMap(0.51, 0.15)
    clickOnImageMap(0.1, 0.1)
    clickOnImageMap(0.61, 0.21)
    setFracCounter("2", "0")
    nextPrompt

  }

  test("Cereal") {
    selectFood("strawberry crunch cereal (granola)")
    nextPrompt
    clickButtonByLabelAndWait("No")
  }

  test("Strawberries") {
    selectFood("Strawberries")
    choosePortionSizeMethod(1)
    nextPrompt
    nextPrompt
  }

  test("Tea same as before") {
    selectFood("Tea")
    clickButtonByLabelAndWait("Yes, I had the same as before")
  }

  test("Cereal assoc foods") {
    clickButtonByLabel("Yes, I had some")
    selectFood("Skimmed milk")
    clickButtonByLabelAndWait("No, I did not")
  }

  test("Cereal portion size") {
    choosePortionSizeMethod(2)
    clickOnImageMap(0.15, 0.37)
    clickOnImageMap(0.453, 0.239)
  }

  test("Strawberries assoc foods") {
    clickButtonByLabelAndWait("No, I did not")
    clickButtonByLabelAndWait("No, I did not")
  }

  test("Pizza ready meal") {
    clickElementByLabel("Vegetable pizza (includes homemade)")
    clickButtonByLabelAndWait("I have finished, continue")
  }

  test("Report missing food / home recipe") {
    clickButtonByLabel("I can't find my food")
    clickButtonByLabelAndWait("Report a missing food")
    clickElementByLabel("This was a home made dish and I know the ingredients")
    clickButtonByLabelAndWait("Continue")
    addIngredient("chicken")
    addIngredient("cheese sauce")
    nextPrompt
    selectFood("Chicken breast fillet")
    choosePortionSizeMethod(1)
    val weightField = new TextField(waitForElement(".intake24-active-prompt input"))
    weightField.value = "150"
    nextPrompt
    selectFood("Cheese sauce")
    choosePortionSizeMethod(1)
    val weightField2 = new TextField(waitForElement(".intake24-active-prompt input"))
    weightField2.value = "50"
    nextPrompt
  }

  test("Report missing food / no recipe") {
    clickButtonByLabel("I can't find my food")
    clickButtonByLabelAndWait("Report a missing food")
    clickElementByLabel("I don't know the ingredients / this was not a home made dish")
    clickButtonByLabelAndWait("Continue")
  }

}