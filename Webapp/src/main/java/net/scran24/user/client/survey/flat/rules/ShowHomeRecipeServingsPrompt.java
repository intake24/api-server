/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat.rules;

import static org.workcraft.gwt.shared.client.CollectionUtils.forall;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.PromptRule;
import net.scran24.user.client.survey.flat.PromptUtil;
import net.scran24.user.client.survey.flat.SelectionType;
import net.scran24.user.client.survey.prompts.MealOperation;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.client.survey.prompts.simple.FractionalQuantityPrompt;
import net.scran24.user.shared.CompoundFood;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.RawFood;
import net.scran24.user.shared.Recipe;
import net.scran24.user.shared.WithPriority;

import org.pcollections.client.PSet;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;
import org.workcraft.gwt.shared.client.Pair;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

public class ShowHomeRecipeServingsPrompt implements PromptRule<Pair<FoodEntry, Meal>, MealOperation> {
	private final PromptMessages messages = GWT.create(PromptMessages.class);

	@Override
	public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> apply(final Pair<FoodEntry, Meal> data,
			SelectionType selectionType, final PSet<String> surveyFlags) {
		return data.left.accept(new FoodEntry.Visitor<Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>>>() {

			private Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> getPromptIfApplicable(final FoodEntry food) {
				if (forall(Meal.linkedFoods(data.right.foods, data.left), FoodEntry.isPortionSizeComplete)
						&& (!food.customData.containsKey(Recipe.SERVINGS_NUMBER_KEY))) {

					FractionalQuantityPrompt quantityPrompt = new FractionalQuantityPrompt(SafeHtmlUtils
							.fromSafeConstant(messages.homeRecipe_servingsPromptText(SafeHtmlUtils.htmlEscape(food.description()))),
							messages.homeRecipe_servingsButtonLabel());

					return Option.some(PromptUtil.asExtendedFoodPrompt(quantityPrompt, new Function1<Double, MealOperation>() {
						@Override
						public MealOperation apply(final Double servings) {
							return MealOperation.updateFood(data.right.foodIndex(food), new Function1<FoodEntry, FoodEntry>() {
								@Override
								public FoodEntry apply(FoodEntry argument) {
									return argument.withCustomDataField(Recipe.SERVINGS_NUMBER_KEY, Double.toString(servings));
								}
							});

						}
					}));
				} else {
					return Option.none();
				}
			}

			@Override
			public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> visitRaw(RawFood food) {
				return Option.none();
			}

			@Override
			public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> visitEncoded(EncodedFood food) {
				return Option.none();
			}

			@Override
			public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> visitTemplate(final TemplateFood food) {
				if (food.isTemplateComplete())
					return getPromptIfApplicable(food);
				else
					return Option.none();
			}

			@Override
			public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> visitMissing(MissingFood food) {
				return Option.none();
			}

			@Override
			public Option<Prompt<Pair<FoodEntry, Meal>, MealOperation>> visitCompound(CompoundFood food) {
				return getPromptIfApplicable(food);
			}

		});
	}

	@Override
	public String toString() {
		return "Ask how many people did the homemade dish serve";
	}

	public static WithPriority<PromptRule<Pair<FoodEntry, Meal>, MealOperation>> withPriority(int priority) {
		return new WithPriority<PromptRule<Pair<FoodEntry, Meal>, MealOperation>>(new ShowHomeRecipeServingsPrompt(),
				priority);
	}
}