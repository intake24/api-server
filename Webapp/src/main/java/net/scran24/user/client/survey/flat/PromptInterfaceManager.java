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

package net.scran24.user.client.survey.flat;

import java.util.Iterator;

import net.scran24.common.client.IEHack;
import net.scran24.user.client.survey.SurveyStageInterface;
import net.scran24.user.client.survey.SurveyStageInterface.Aligned;
import net.scran24.user.client.survey.SurveyStageInterface.Stretched;

import org.workcraft.gwt.shared.client.Callback;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

public class PromptInterfaceManager {
	private final Panel interfacePanel;
	private int animDuration = 400;

	public PromptInterfaceManager(Panel interfacePanel) {
		this.interfacePanel = interfacePanel;
		
		String duration = Storage.getSessionStorageIfSupported().getItem("intake24-prompt-anim-duration");
		
		if ( duration != null)
			animDuration = Integer.parseInt(duration);
	}
	
	public static void scrollPromptIntoView() {
		
	  if (Storage.getSessionStorageIfSupported().getItem("intake24-disable-auto-scroll") == null) 		
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			public native void scrollIntoView() /*-{
				
				var e = $wnd.$("#intake24-prompt-scroll-target");
				
				if (e.offset().top < $wnd.$('body').scrollTop()) {				
					$wnd.$('html, body').animate({
						scrollTop : e.offset().top
					}, 500);
				}
			}-*/;

			@Override
			public void execute() {
				scrollIntoView();
			}
		});
	}

	public void applyInterface(final Prompt<Survey, SurveyOperation> prompt, final Callback1<SurveyOperation> applyOperation,
			final Callback1<Function1<Survey, Survey>> applyIntermediateState) {
		
		final SurveyStageInterface interf = prompt.getInterface(applyOperation, applyIntermediateState);

		Iterator<Widget> i = interfacePanel.iterator();

		int h = 0;

		boolean previousPrompt = i.hasNext();
		
		final double animDirection = LocaleInfo.getCurrentLocale().isRTL() ? 1.0 : -1.0;
				
		while (i.hasNext()) {
			final Widget w = i.next();
			
			w.getElement().removeClassName("intake24-active-prompt");

			int wh = w.getElement().getClientHeight();
			if (wh > h)
				h = wh;
			
			w.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);

			Animation anim = new Animation() {
				@Override
				protected void onUpdate(double progress) {
					w.getElement().getStyle().setLeft(animDirection * progress * 105, Unit.PCT);
				}

				@Override
				protected void onComplete() {
					w.removeFromParent();
				}
			};

			anim.run(animDuration);
		}

		final FlowPanel newPrompt = new FlowPanel();

		newPrompt.setWidth("100%");
		newPrompt.getElement().addClassName("intake24-active-prompt");
		newPrompt.getElement().setId("intake24-prompt-scroll-target");

		interfacePanel.add(newPrompt);
		
		scrollPromptIntoView();
		
		newPrompt.getElement().getStyle().setPosition(Position.RELATIVE);
		newPrompt.getElement().getStyle().setProperty("verticalAlign", "top");
		newPrompt.getElement().getStyle().setProperty("whiteSpace", "normal");

		if (previousPrompt) {

			//newPrompt.getElement().getStyle().setLeft(100, Unit.PCT);
			newPrompt.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);

			Animation anim = new Animation() {
				@Override
				protected void onUpdate(double progress) {
					
					  
					
					newPrompt.getElement().getStyle().setLeft(animDirection * progress * 100, Unit.PCT);
				}

				@Override
				protected void onComplete() {
					newPrompt.getElement().getStyle().setLeft(0, Unit.PX);
					newPrompt.getElement().getStyle().clearDisplay();
					
					interf.onAnimationComplete.accept(new Option.SideEffectVisitor<Callback>() {
						@Override
						public void visitSome(Callback item) {
							item.call();
						}

						@Override
						public void visitNone() {
						}
					});
					
					IEHack.forceReflowDeferred();

				
				}
			};

			anim.run(animDuration);
		}

		interf.accept(new SurveyStageInterface.Visitor() {
			@Override
			public void visitStretched(Stretched ssi) {
				//newPrompt.setWidth("100%");
				// newPrompt.setHeight("100%");
				newPrompt.add(ssi.content);
			}

			@Override
			public void visitAligned(Aligned ssi) {
				//newPrompt.setWidth("100%");
				// newPrompt.setHeight("100%");

				// newPrompt.getElement().getStyle().clearWidth();
				// newPrompt.getElement().getStyle().clearHeight();
				newPrompt.add(ssi.content);
			}
		});
	}
}
