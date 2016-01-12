/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat.rules;

import net.scran24.common.client.GoogleAnalytics;
import net.scran24.user.client.survey.FoodTemplates;
import net.scran24.user.client.survey.flat.FoodOperation;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.PromptRule;
import net.scran24.user.client.survey.flat.PromptUtil;
import net.scran24.user.client.survey.flat.SelectionType;
import net.scran24.user.client.survey.flat.Survey;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.client.survey.prompts.simple.RadioButtonPrompt;
import net.scran24.user.shared.CompoundFood;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.FoodLink;
import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.WithPriority;

import org.pcollections.client.PSet;
import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

public class AskIfHomeRecipe implements PromptRule<FoodEntry, FoodOperation> {
	private final PromptMessages messages = GWT.create(PromptMessages.class);
	
	@Override
	public Option<Prompt<FoodEntry, FoodOperation>> apply(FoodEntry data, SelectionType selectionType, PSet<String> surveyFlags) {
		if (!surveyFlags.contains(Survey.FLAG_FREE_ENTRY_COMPLETE))
			return new Option.None<Prompt<FoodEntry, FoodOperation>>();
		else if (data.isMissing() && !(data.flags.contains(MissingFood.NOT_HOME_RECIPE_FLAG) || data.flags.contains(MissingFood.HOME_RECIPE_FLAG)))
			return new Option.Some<Prompt<FoodEntry, FoodOperation>>(buildPrompt(data.description(), data.isDrink()));
		else
			return new Option.None<Prompt<FoodEntry, FoodOperation>>();
	}

	@Override
	public String toString() {
		return "Ask if the missing food was a home-made dish";
	}

	public static WithPriority<PromptRule<FoodEntry, FoodOperation>> withPriority(int priority) {
		return new WithPriority<PromptRule<FoodEntry, FoodOperation>>(new AskIfHomeRecipe(), priority);
	}
	
	private Prompt<FoodEntry, FoodOperation> buildPrompt(final String foodName, final boolean isDrink) {	
		PVector<String> options = TreePVector.<String>empty().plus(messages.homeRecipe_haveRecipeChoice()).plus(messages.homeRecipe_noRecipeChoice()); 
		
		return PromptUtil.asFoodPrompt(new RadioButtonPrompt(SafeHtmlUtils.fromSafeConstant(messages.homeRecipe_promptText(SafeHtmlUtils.htmlEscape(foodName.toLowerCase()))), AskIfHomeRecipe.class.getSimpleName(), options, messages.homeRecipe_continueButtonLabel(), "homeRecipeOption", Option.<String>none()), new Function1<String, FoodOperation>() {
			@Override
			public FoodOperation apply(String argument) {
				if (argument.equals(messages.homeRecipe_haveRecipeChoice())) {
					GoogleAnalytics.trackMissingFoodHomeRecipe();
					return FoodOperation.replaceWith( new CompoundFood(FoodLink.newUnlinked(), foodName, isDrink));
				} else {
					return FoodOperation.update(new Function1<FoodEntry, FoodEntry> () {
						@Override
						public FoodEntry apply(FoodEntry argument) {
							GoogleAnalytics.trackMissingFoodNotHomeRecipe();
							return argument.withFlag(MissingFood.NOT_HOME_RECIPE_FLAG);
						}
					});
				}
			}
		});		
	}
}
