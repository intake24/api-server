/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.gwt.slidingscale.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Image;
import com.kiouri.sliderbar.client.view.SliderBarVertical;

public class SlidingScaleSlider extends SliderBarVertical {
	Images images = GWT.create(Images.class);

	public SlidingScaleSlider(String height) {
		Image scale = new Image(images.scalev());
		scale.getElement().getStyle().setProperty("backgroundRepeat", "repeat-y");
		setScaleWidget(scale, 16);
		setDragWidget(new Image(images.drag()));
		setHeight(height);
		setMaxValue(1000);
		removeMarks();
	}
	
	interface Images extends ClientBundle{
		@Source("x.png")
		ImageResource drag();
		@Source("scale.png")
		ImageResource scalev();				
	}

	@Override
	public int getStep() {
		return 50;
	}
}
