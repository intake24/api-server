/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey;

import org.workcraft.gwt.shared.client.Callback;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;
import com.google.gwt.user.client.ui.Panel;

public abstract class SurveyStageInterface {
	public static final Options DEFAULT_OPTIONS = new Options() {
		@Override
		public boolean skipAnimation() {
			return false;
		}
	};
		
	public final Option<Callback> onAnimationComplete;
	
	public SurveyStageInterface(Option<Callback> onAnimationComplete) {
		this.onAnimationComplete = onAnimationComplete;
	}

	public abstract void accept (Visitor visitor);
	
	public interface Visitor {
		void visitStretched(Stretched ssi);
		void visitAligned(Aligned ssi);
	}
	
	public interface Options {
		boolean skipAnimation();
	}
	
	public static class Stretched extends SurveyStageInterface {
		public final Panel content;
		public final Options options;
		
		public Stretched (Panel content, Options options) {
			this (content, options, Option.<Callback>none());
		}
		
		public Stretched (Panel content, Options options, Option<Callback> onAnimationComplete) {
			super(onAnimationComplete);
			this.content = content;
			this.options = options;
			content.addStyleName("scran24-stretched-survey-page-content");
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.visitStretched(this);
		}
	}
	
	public static class Aligned extends SurveyStageInterface {
		public final Panel content;
		public final VerticalAlignmentConstant verticalAlignment;
		public final HorizontalAlignmentConstant horizontalAlignment;
		public final Options options;

		public Aligned(Panel content, HorizontalAlignmentConstant horizontalAlignment, VerticalAlignmentConstant verticalAlignment, Options options) {
			this(content, horizontalAlignment, verticalAlignment, options, Option.<Callback>none());		
		}
		
		public Aligned(Panel content, HorizontalAlignmentConstant horizontalAlignment, VerticalAlignmentConstant verticalAlignment, Options options, Option<Callback> onAnimationComplete) {
			super(onAnimationComplete);
			this.content = content;
			this.horizontalAlignment = horizontalAlignment;
			this.verticalAlignment = verticalAlignment;
			this.options = options;
			content.addStyleName("intake24-prompt-container");
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.visitAligned(this);
		}
	}
}