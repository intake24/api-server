/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client;

import net.scran24.user.client.survey.SimpleSurveyStageInterface;
import net.scran24.user.client.survey.SurveyStage;
import net.scran24.user.client.survey.flat.Survey;

import org.workcraft.gwt.shared.client.Callback;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Callback2;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.user.client.ui.Panel;

/**
 * Controls the presentation of top-level survey pages,
 * and transitions between the pages.
 */
public class SurveyInterfaceManager {
	final private Panel container;
	
	private Callback1<Survey> onComplete;
	private Callback2<Survey, Boolean> onIntermediateStateChange;

	/**
	 * @param container
	 * The container Panel that will contain survey page UI elements.
	 * Usually wraps a content container div in the host HTML page.
	 */
	public SurveyInterfaceManager(Panel container) {
		this.container = container;
	}
	
	/**
	 * @param onComplete
	 * Called when user has finished interacting with the current survey page
	 * (e.g. by clicking continue). The Survey parameter is the updated Survey state.
	 * @param onIntermediateStateChange
	 * Called when the user has not finished interacting with the current page,
	 * but has made some changes to the data that could also be displayed elsewhere.
	 * 
	 * The main purpose is to synchronise UI elements.
	 */
	public void setCallbacks(Callback1<Survey> onComplete, Callback2<Survey, Boolean> onIntermediateStateChange) {
		this.onComplete = onComplete;
		this.onIntermediateStateChange = onIntermediateStateChange;
	}
	
	/**
	 * Obtains the UI elements from the survey stage and 
	 * adds those elements to the container element on the page.
	 * 
	 * Any existing UI elements in the container will be removed.
	 */
	public void show(SurveyStage<Survey> stage) {
		SimpleSurveyStageInterface interf = stage.getInterface(onComplete, onIntermediateStateChange);
		
		container.clear();
		container.add(interf.content);
		
		interf.onAnimationComplete.accept(new Option.SideEffectVisitor<Callback>() {
			@Override
			public void visitSome(Callback callback) {
				callback.call();
			}

			@Override
			public void visitNone() {
			}
		});
	}
}