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

This file is based on Intake24 v1.0.

© Crown copyright, 2012, 2013, 2014

Licensed under the Open Government Licence 3.0: 

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat.rules;

import org.pcollections.client.PSet;
import org.workcraft.gwt.shared.client.Either;
import org.workcraft.gwt.shared.client.Option;
import org.workcraft.gwt.shared.client.Pair;

import net.scran24.datastore.shared.CompletedPortionSize;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.PromptRule;
import net.scran24.user.client.survey.flat.SelectionMode;
import net.scran24.user.client.survey.portionsize.experimental.PortionSize;
import net.scran24.user.client.survey.prompts.BreadLinkedFoodAmountPrompt;
import net.scran24.user.client.survey.prompts.MealOperation;
import net.scran24.user.shared.CompoundFood;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.RawFood;
import net.scran24.user.shared.SpecialData;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.UUID;
import net.scran24.user.shared.WithPriority;

public class ShowBreadLinkedFoodAmountPrompt implements PromptRule<Pair<FoodEntry, Meal>, MealOperation> {

  final public static String FLAG_BREAD_LINKED_FOOD_AMOUNT_SHOWN = "bread-linked-food-amount-shown";

  @Override
  public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> apply(final Pair<FoodEntry, Meal> data, SelectionMode selectionType,
      final PSet<String> surveyFlags) {
    return data.left.accept(new FoodEntry.Visitor<Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>>>() {
      @Override
      public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> visitRaw(RawFood food) {
        return Option.none();
      }

      // This is fucking unreadable, that is what it is
      // We want to check that
      // 1) This food is a linked food, and the parent food is in the BRED
      // category
      // 2) Parent food portion size estimation is complete
      // 3) Parent food portion size estimation method is "guide-image" and the
      // "quantity" data field is > 1
      @Override
      public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> visitEncoded(final EncodedFood food) {
        if (!food.isPortionSizeComplete())
          return Option.none();
        else
          return food.link.linkedTo.accept(new Option.Visitor<UUID, Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>>>() {
            @Override
            public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> visitSome(UUID parentId) {
              FoodEntry mainFood = data.right.getFoodById(parentId).getOrDie("Parent food not found in the meal");
              final int mainFoodIndex = data.right.foodIndex(parentId);
              final int foodIndex = data.right.foodIndex(food.link.id);

              if (mainFood.isEncoded()) {
                EncodedFood encodedMainFood = mainFood.asEncoded();

                if (encodedMainFood.isInCategory(SpecialData.CATEGORY_BREAD_TOP_LEVEL) && !food.flags.contains(FLAG_BREAD_LINKED_FOOD_AMOUNT_SHOWN)) {

                  return encodedMainFood.portionSize
                    .accept(new Option.Visitor<Either<PortionSize, CompletedPortionSize>, Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>>>() {
                      @Override
                      public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> visitSome(
                          final Either<PortionSize, CompletedPortionSize> portionSize) {
                        return portionSize
                          .accept(new Either.Visitor<PortionSize, CompletedPortionSize, Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>>>() {
                            @Override
                            public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> visitRight(CompletedPortionSize completedPortionSize) {

                              if (completedPortionSize.scriptName.equals("guide-image")) {
                                double quantity = Double.parseDouble(completedPortionSize.data.get("quantity"));
                                if (quantity > 1.0)
                                  return Option.<Prompt<Pair<FoodEntry, Meal>, MealOperation>>some(
                                      new BreadLinkedFoodAmountPrompt(data.right, foodIndex, mainFoodIndex, quantity));
                                else
                                  return Option.none();
                              } else {
                                return Option.none();
                              }
                            }

                            @Override
                            public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> visitLeft(PortionSize value) {
                              return Option.none();
                            }
                          });
                      }

                      @Override
                      public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> visitNone() {
                        return Option.none();
                      }
                    });
                } else {
                  return Option.none();
                }
              } else
                return Option.none();
            }

            @Override
            public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> visitNone() {
              return Option.none();
            }
          });
      }

      @Override
      public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> visitTemplate(TemplateFood food) {
        return Option.none();
      }

      @Override
      public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> visitMissing(MissingFood food) {
        return Option.none();
      }

      @Override
      public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> visitCompound(CompoundFood food) {
        return Option.none();
      }

    });
  }

  @Override
  public String toString() {
    return "Brand name prompt";
  }

  public static WithPriority<PromptRule<Pair<FoodEntry, Meal>, MealOperation>> withPriority(int priority) {
    return new WithPriority<PromptRule<Pair<FoodEntry, Meal>, MealOperation>>(new ShowBreadLinkedFoodAmountPrompt(), priority);
  }
}