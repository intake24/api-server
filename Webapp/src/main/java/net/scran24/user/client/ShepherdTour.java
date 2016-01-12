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

package net.scran24.user.client;

import net.scran24.common.client.GoogleAnalytics;
import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.survey.SurveyMessages;

import org.pcollections.client.PVector;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class ShepherdTour {
	
	private static final SurveyMessages messages = SurveyMessages.Util.getInstance(); 
	
	private static final String nextButtonLabel = messages.help_nextButtonLabel(); 
	private static final String backButtonLabel = messages.help_backButtonLabel();
	private static final String exitButtonLabel = messages.help_exitButtonLabel();
	private static final String doneButtonLabel = messages.help_doneButtonLabel();
	private static final String stillStuckButtonLabel = messages.help_stillStuckButtonLabel();
	
	public static class Step {
		public final String stepId;
		public final String attachTo;
		public final String title;
		public final String text;
		public final String attachment;
		public final String targetAttachment;
		public final boolean scrollTo;

		public Step(String stepId, String attachTo, String title, String text, String attachment, String targetAttachment, boolean scrollTo) {
			this.stepId = stepId;
			this.attachTo = attachTo;
			this.title = title;
			this.text = text;
			this.attachment = attachment;
			this.targetAttachment = targetAttachment;
			this.scrollTo = scrollTo;
		}

		public Step(String stepId, String attachTo, String title, String text, String attachment, String targetAttachment) {
			this(stepId, attachTo, title, text, attachment, targetAttachment, true);
		}

		public Step(String stepId, String attachTo, String title, String text) {
			this(stepId, attachTo, title, text, "top left", "bottom left");
		}

		public Step(String stepId, String attachTo, String title, String text, boolean scrollTo) {
			this(stepId, attachTo, title, text, "top left", "bottom left", scrollTo);

		}
	}

	private static void requestCallBack(String promptType) {
		GoogleAnalytics.trackHelpCallbackRequested(promptType);
		CallbackRequestForm.showPopup();
	}

	private static native JavaScriptObject createTour() /*-{
		var tour = new $wnd.Shepherd.Tour({
			defaults : {
				classes : 'shepherd-theme-arrows',
				showCancelLink : true,
				scrollTo : true
			}
		});

		tour.on("complete", function() {
			$wnd.$('.shepherd-step').remove();
			@net.scran24.user.client.ShepherdTour::onTourComplete()();
		});

		tour.on("cancel", function() {
			$wnd.$('.shepherd-step').remove();
			@net.scran24.user.client.ShepherdTour::onTourComplete()();
		});

		return tour;
	}-*/;

	private static native JavaScriptObject firstStepButtons(JavaScriptObject tour) /*-{
		return [ {
			text : @net.scran24.user.client.ShepherdTour::exitButtonLabel,
			classes : 'shepherd-button-secondary',
			action : tour.cancel
		}, {
			text : @net.scran24.user.client.ShepherdTour::nextButtonLabel,
			action : tour.next
		} ];

	}-*/;

	private static native JavaScriptObject lastStepButtons(JavaScriptObject tour) /*-{
		return [ {
			text : @net.scran24.user.client.ShepherdTour::stillStuckButtonLabel,
			classes : 'shepherd-button-red',
			action : function() {
				tour.cancel();
				@net.scran24.user.client.ShepherdTour::requestCallBack(Ljava/lang/String;)(tour.intake24_promptType);
			}
		}, {
			text : @net.scran24.user.client.ShepherdTour::backButtonLabel,
			action : tour.back
		}, {
			text : @net.scran24.user.client.ShepherdTour::doneButtonLabel,
			action : tour.next
		} ];
	}-*/;

	private static native JavaScriptObject singleStepButtons(JavaScriptObject tour) /*-{
		return [ {
			text : @net.scran24.user.client.ShepherdTour::stillStuckButtonLabel,
			classes : 'shepherd-button-red',
			action : function() {
				tour.cancel();
				@net.scran24.user.client.ShepherdTour::requestCallBack(Ljava/lang/String;)(tour.intake24_promptType);
			}
		}, {
			text : @net.scran24.user.client.ShepherdTour::doneButtonLabel,
			action : tour.next
		} ];
	}-*/;

	private static native JavaScriptObject standardButtons(JavaScriptObject tour) /*-{
		return [ {
			text : @net.scran24.user.client.ShepherdTour::exitButtonLabel,
			classes : 'shepherd-button-secondary',
			action : tour.cancel
		}, {
			text : @net.scran24.user.client.ShepherdTour::backButtonLabel,
			action : tour.back
		}, {
			text : @net.scran24.user.client.ShepherdTour::nextButtonLabel,
			action : tour.next
		} ];

	}-*/;

	private static native void addStep(JavaScriptObject tour, JavaScriptObject buttons, String id, String title, String text, String attachTo,
			String attachment, String targetAttachment, boolean scrollTo) /*-{
		tour.addStep(id, {
			text : text,
			title : title,
			attachTo : {
				element : attachTo
			},
			scrollTo : scrollTo,
			buttons : buttons,
			tetherOptions : {
				attachment : attachment,
				targetAttachment : targetAttachment
			}
		});
	}-*/;

	private static native void nativeStartTour(JavaScriptObject tour, String promptType) /*-{
		tour.intake24_promptType = promptType;
		tour.start();
	}-*/;

	private static native void nativeCancelTour(JavaScriptObject tour) /*-{
		tour.cancel();
	}-*/;

	private static boolean isShepherdElement(Element e) {
		if (e == null)
			return false;

		if (e.hasClassName("shepherd-step"))
			return true;
		else
			return isShepherdElement(e.getParentElement());
	}

	private static HandlerRegistration handler = null;

	public static void onTourComplete() {
		if (handler != null) {
			handler.removeHandler();
			handler = null;
		}
	}

	public static void startTour(PVector<Step> steps, String promptType) {
		final JavaScriptObject tour = createTour();

		if (steps.size() == 1) {
			Step step = steps.get(0);
			addStep(tour, singleStepButtons(tour), step.stepId, step.title, step.text, step.attachTo, step.attachment, step.targetAttachment,
					step.scrollTo);
		} else {
			for (int i = 0; i < steps.size(); i++) {
				Step step = steps.get(i);

				if (i == 0)
					addStep(tour, firstStepButtons(tour), step.stepId, step.title, step.text, step.attachTo, step.attachment, step.targetAttachment,
							step.scrollTo);
				else if (i == steps.size() - 1)
					addStep(tour, lastStepButtons(tour), step.stepId, step.title, step.text, step.attachTo, step.attachment, step.targetAttachment,
							step.scrollTo);
				else
					addStep(tour, standardButtons(tour), step.stepId, step.title, step.text, step.attachTo, step.attachment, step.targetAttachment,
							step.scrollTo);
			}
		}

		nativeStartTour(tour, promptType);

		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			@Override
			public void execute() {

				handler = RootPanel.get().addDomHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {

						Element target = Element.as(event.getNativeEvent().getEventTarget());

						if (!isShepherdElement(target)) {
							nativeCancelTour(tour);
						}
					}
				}, ClickEvent.getType());
			}
		});

	}

	public static Button createTourButton(final PVector<Step> steps, final String promptType) {
		return WidgetFactory.createHelpButton(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				GoogleAnalytics.trackHelpButtonClicked(promptType);
				startTour(steps, promptType);
			}
		});
	}

	public static void makeShepherdTarget(Widget... widgets) {
		for (Widget w : widgets)
			w.addStyleName("intake24-shepherd-target");
	}
}
