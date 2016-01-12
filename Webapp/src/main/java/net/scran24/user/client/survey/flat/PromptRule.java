/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat;

import org.pcollections.client.PSet;
import org.workcraft.gwt.shared.client.Option;

/**
 * Specifies a prompt applicability rule.
 * @param <T> Type of the element that the prompt is applicable to - can be e.g. FoodEntry, Meal or Survey 
 * @param <Op> Type of the operation applied by this prompt - can be FoodOperation, MealOperation or SurveyOperation
 */
public interface PromptRule<T, Op> {
	/**
	 * @param state The currently selected element.
	 * @param selectionType Specifies whether the selection was made by the user or automatically by the selection manager.
	 * @param surveyFlags flags describing the overall state of the survey (e.g. whether certain sections have been complete)
	 * @return
	 * Some(prompt), an instance of a prompt if this rule is applicable
	 * or None if this rule is not currently applicable. 
	 */
	Option<Prompt<T, Op>> apply (final T state, SelectionType selectionType, PSet<String> surveyFlags);
}
