/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat.rules;

import net.scran24.datastore.shared.Time;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.PromptRule;
import net.scran24.user.client.survey.flat.SelectionMode;
import net.scran24.user.client.survey.flat.Survey;
import net.scran24.user.client.survey.flat.SurveyOperation;
import net.scran24.user.client.survey.prompts.ConfirmTimeGapPrompt;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.WithPriority;

import org.pcollections.client.PSet;
import org.pcollections.client.PVector;
import org.workcraft.gwt.shared.client.CollectionUtils.WithIndex;
import org.workcraft.gwt.shared.client.Option;

public class ShowTimeGapPrompt implements PromptRule<Survey, SurveyOperation> {
	private final int timeGapThresholdInMinutes;
	private final Time dayStartTime;
	private final Time dayEndTime;

	public ShowTimeGapPrompt(int timeGapThresholdInMinutes, Time dayStartTime, Time dayEndTime) {
		this.timeGapThresholdInMinutes = timeGapThresholdInMinutes;
		this.dayStartTime = dayStartTime;
		this.dayEndTime = dayEndTime;
	}
	
	@Override
	public Option<Prompt<Survey, SurveyOperation>> apply(Survey state, SelectionMode selectionType, PSet<String> surveyFlags) {
		if (!state.isPortionSizeComplete() || state.meals.isEmpty())
			return Option.none();
		else {
			PVector<WithIndex<Meal>> meals = state.mealsSortedByTime;

			WithIndex<Meal> firstMeal = meals.get(0);
			WithIndex<Meal> lastMeal = meals.get(state.meals.size() - 1);
			
			if (
					firstMeal.value.time.getOrDie().isAfter(dayStartTime) &&
					(firstMeal.value.time.getOrDie().minutesAfter(dayStartTime) > timeGapThresholdInMinutes) &&
					!firstMeal.value.confirmedNoMealsBefore()
					)
				return Option.<Prompt<Survey, SurveyOperation>>some(new ConfirmTimeGapPrompt(state, new ConfirmTimeGapPrompt.TimeGap.BeforeMeal(firstMeal.index)));
			
			if (meals.size() > 1)
				for (int i=0; i< (meals.size() - 1); i++) {
					if (
							(meals.get(i).value.time.getOrDie().minutesBefore(meals.get(i+1).value.time.getOrDie()) > timeGapThresholdInMinutes) &&
							!meals.get(i).value.confirmedNoMealsAfter() &&
							!meals.get(i+1).value.confirmedNoMealsBefore()
							)
						return Option.<Prompt<Survey, SurveyOperation>>some(new ConfirmTimeGapPrompt(state, new ConfirmTimeGapPrompt.TimeGap.BetweenMeals(meals.get(i).index, meals.get(i+1).index)));
				}
			
			if (
					lastMeal.value.time.getOrDie().isBefore(dayEndTime) &&
					(lastMeal.value.time.getOrDie().minutesBefore(dayEndTime) > timeGapThresholdInMinutes) &&
					!lastMeal.value.confirmedNoMealsAfter()
					)
				return Option.<Prompt<Survey, SurveyOperation>>some(new ConfirmTimeGapPrompt(state, new ConfirmTimeGapPrompt.TimeGap.AfterMeal(lastMeal.index)));
			
			return Option.none();
		}
	}

	@Override
	public String toString() {
		return "Show time gap prompt";
	}

	public static WithPriority<PromptRule<Survey, SurveyOperation>> withPriority(int priority, int timeGapThresholdInMinutes, Time dayStartTime, Time dayEndTime) {
		return new WithPriority<PromptRule<Survey, SurveyOperation>>(new ShowTimeGapPrompt(timeGapThresholdInMinutes, dayStartTime, dayEndTime), priority);
	}
}