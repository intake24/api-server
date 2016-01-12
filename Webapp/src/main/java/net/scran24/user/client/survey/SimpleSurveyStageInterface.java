/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey;

import org.workcraft.gwt.shared.client.Callback;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.user.client.ui.Panel;

public class SimpleSurveyStageInterface {
	public final Option<Callback> onAnimationComplete;
	public final Panel content;

	public SimpleSurveyStageInterface(final Panel content) {
		this(content, Option.<Callback> none());
	}

	/**
	 * @param content
	 *          GWT container with the UI elements.
	 * @param onAnimationComplete
	 *          a callback that is called when the UI elements are presented to
	 *          the user (e.g. following a transition animation)
	 */
	public SimpleSurveyStageInterface(final Panel content, final Option<Callback> onAnimationComplete) {
		this.onAnimationComplete = onAnimationComplete;
		this.content = content;
	}
}