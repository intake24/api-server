/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.client.animation;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.user.client.ui.Widget;

public class FadeIn extends Animation {
	private final Widget w;

	public FadeIn(final Widget w) {
		this.w = w;
	}

	@Override
	protected void onUpdate(double progress) {
		w.getElement().getStyle().setOpacity(progress);
	}
}
