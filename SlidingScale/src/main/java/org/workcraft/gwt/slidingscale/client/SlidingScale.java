/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.gwt.slidingscale.client;

import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.slidingscale.shared.SlidingScaleDef;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.kiouri.sliderbar.client.event.BarValueChangedEvent;
import com.kiouri.sliderbar.client.event.BarValueChangedHandler;

public class SlidingScale extends Composite {
	public final SlidingScaleSlider sliderBar;
	public final Image image;
	public final FlowPanel overlayDiv;
	public final Label label;
	
	private final double limit;

	public double getValue() {
		return ((1000 - sliderBar.getValue()) / 1000.0) * limit;		
	}
	
	public void setValue(double value) {
		if (value > limit)
			value = limit;
		if (value < 0.0)
			value = 0.0;
		
		double a;
		
		if (limit == 0.0)
			a = 0;
		else
			a = value / limit;
		
		int b = (int)(a * 1000.0);
		
		sliderBar.setValue(1000 - b);
	}
	
	public SlidingScale(final SlidingScaleDef definition, double limit, double initialLevel, final Function1<Double, String> labelfunc) {
		if (limit > 1.0)
			limit = 1.0;
		if (limit < 0.0)
			limit = 0.0;
		this.limit = limit;
		
		HorizontalPanel p = new HorizontalPanel();
		p.getElement().getStyle().setBackgroundColor("#eeeeee");
		
		FlowPanel imageDiv = new FlowPanel();
		Style s = imageDiv.getElement().getStyle();
		s.setPosition(Position.RELATIVE);
		
		overlayDiv = new FlowPanel();
		overlayDiv.getElement().setId("intake24-sliding-scale-overlay");
		
		final Style overlayStyle = overlayDiv.getElement().getStyle();
		
		overlayStyle.setPosition(Position.ABSOLUTE);
		overlayStyle.setOverflow(Overflow.HIDDEN);
		overlayStyle.setWidth(definition.imageWidth, Unit.PX);
		overlayStyle.setHeight(definition.imageHeight, Unit.PX);
		overlayStyle.setTop(0, Unit.PX);
		
		Image overlayImage = new Image(definition.overlayUrl);
		
		final Style overlayImageStyle = overlayImage.getElement().getStyle();
		overlayImageStyle.setPosition(Position.ABSOLUTE);
		
		label = new Label();
		label.addStyleName("intake24-as-served-image-label");
		
		overlayDiv.add(overlayImage);
				
		image = new Image(definition.baseImageUrl);
		image.getElement().setId("intake24-sliding-scale-image");
		imageDiv.add(image);
		imageDiv.add(overlayDiv);
		imageDiv.add(label);

		final int scaleHeight = (int)((definition.fullLevel - definition.emptyLevel) * limit);
		final int limitOffset = (int)((1.0-limit) * (definition.fullLevel - definition.emptyLevel));
		sliderBar = new SlidingScaleSlider(scaleHeight + "px");
		sliderBar.getElement().setId("intake24-sliding-scale-slider");
		sliderBar.getElement().getStyle().setMarginTop(definition.imageHeight - (definition.fullLevel - limitOffset), Unit.PX);
		sliderBar.addBarValueChangedHandler(new BarValueChangedHandler() {
			@Override
			public void onBarValueChanged(BarValueChangedEvent event) {
				double v = event.getValue() / 1000.0;
				long offset = Math.round( v *  scaleHeight) + definition.imageHeight - definition.fullLevel + limitOffset;
				overlayStyle.setTop( offset , Unit.PX);
				overlayStyle.setHeight ( definition.imageHeight - offset , Unit.PX);
				overlayImageStyle.setTop( -offset, Unit.PX);
				label.setText(labelfunc.apply(getValue()));
			}
		});
		
		setValue(initialLevel);
		
		p.add(imageDiv);
		p.add(sliderBar);
		initWidget(p);
		
/*		imageDiv = new FlowPanel();

		imageDiv.getElement().getStyle().setPosition(Position.RELATIVE);
		imageDiv.getElement().setTabIndex(1);
		imageDiv.getElement().setTitle("Guide image: bananas");

		final Image baseImage = new Image(definition.baseImageUrl);
		imageDiv.add(baseImage);

		mouseMoveHandler = new MouseMoveHandler() {
			@Override
			public void onMouseMove(MouseMoveEvent event) {
				int mouseX = event.getRelativeX(baseImage.getElement());
				int mouseY = event.getRelativeY(baseImage.getElement());

				int mouseOverArea = -1;

				for (int i = 0; i < definition.areas.length; i++) {
					if (definition.areas[i].shape.isInside(new Point(mouseX, mouseY))) {
						mouseOverArea = i;
						break;
					}
				}

				if (mouseOverArea != activeArea) {
					if (hasFocus && mouseOverArea == -1) {
						setActiveArea(lastActiveArea);
					} else
						setActiveArea(mouseOverArea);
				}
			}
		};
		
		clickHandler = new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				handler.handleResult(definition.areas[activeArea].id);
			}
		};

		baseImage.addMouseMoveHandler(mouseMoveHandler);

		addDomHandler(new KeyDownHandler() {
			@Override
			public void onKeyDown(KeyDownEvent event) {
				// System.out.println (event.getCharCode());
				
				if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
					handler.handleResult(activeArea);
				}

				if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_LEFT) {
					prev();
					event.preventDefault();
				}
				if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_RIGHT) {
					next();
					event.preventDefault();
				}
				if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_UP) {
					next();
					event.preventDefault();
				}
				if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_DOWN) {
					prev();
					event.preventDefault();
				}
			}
		}, KeyDownEvent.getType());

		addDomHandler(new FocusHandler() {
			@Override
			public void onFocus(FocusEvent event) {
				hasFocus = true;
				if (activeArea == -1)
					setActiveArea(0);
			}
		}, FocusEvent.getType());

		addDomHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				hasFocus = false;
				setActiveArea(-1);
			}
		}, BlurEvent.getType());

		initWidget(imageDiv);
		
		prefetchImages();*/
	}


}