/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat;

import net.scran24.user.client.survey.SurveyStageInterface;

import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;

/**
 * Defines a single prompt, that is a simple straightforward question with a set
 * of UI elements allowing user to answer that question.
 * 
 * @param <T>
 *          The type of the elements this prompt is applicable to, i.e.
 *          FoodEntry or Meal.
 * @param <Op>
 *          The type of operation that this prompt applies to the element, can
 *          be FoodOperation, MealOperation or SurveyOperation.
 */
public interface Prompt<T, Op> {
	/**
	 * @param onComplete
	 *          Will be called when the user interaction is complete and there is
	 *          an operation to be applied to the element.
	 * @param updateIntermediateState
	 *          Will be called when the user interaction is not yet complete but
	 *          the state of the element needs to be updated (e.g. to synchronise
	 *          other UI elements).
	 * @return The UI elements wrapped into a SurveyStageInterface structure.
	 */
	public SurveyStageInterface getInterface(final Callback1<Op> onComplete, final Callback1<Function1<T, T>> updateIntermediateState);
}
