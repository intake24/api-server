/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey;

import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Callback2;

/**
 * Represents a survey page. A survey page updates the current survey state
 * based on user interaction with a set of UI elements.
 */
public interface SurveyStage<T> {

	/**
	 * @param onComplete
	 *          a callback that is called when the user completes the interaction
	 *          with the current page (e.g. clicks <b>Continue</b>).
	 * @param onIntermediateStateChange
	 *          a callback that is called when the user has not yet completed the
	 *          interaction but the survey state needs to be updated (e.g. to
	 *          synchronise another UI element).
	 * @return the definition of UI elements for this page.
	 */
	public SimpleSurveyStageInterface getInterface(Callback1<T> onComplete, Callback2<T, Boolean> onIntermediateStateChange);
}
