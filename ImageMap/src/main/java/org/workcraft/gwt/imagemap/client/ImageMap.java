/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.gwt.imagemap.client;

import java.util.logging.Logger;

import org.workcraft.gwt.imagemap.shared.ImageMapDefinition;
import org.workcraft.gwt.imagemap.shared.ImageMapDefinition.Area;
import org.workcraft.gwt.imagemap.shared.Point;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;

public class ImageMap extends Composite {
	public static interface ResultHandler {
		public void handleResult (int choice);		
	}
	
	private final ImageMapDefinition definition;
	private final FlowPanel imageDiv;
	private final MouseMoveHandler mouseMoveHandler;
	private final ClickHandler clickHandler;

	Image activeOverlay = null;
	int activeArea = -1;
	int lastActiveArea = -1;
	boolean hasFocus = false;

	private void clearOverlay() {
		if (activeOverlay != null) {
			activeOverlay.removeFromParent();
			activeOverlay = null;
		}
	}

	private void setActiveArea(int index) {
		clearOverlay();

		if (index != -1) {
			final Image overlay = new Image(definition.areas[index].overlayUrl);
			overlay.addStyleName("imagemap-overlay");
			
			//overlay.getElement().setTitle("Banana " + Integer.toString(index));
			//overlay.getElement().setTabIndex(100);

			imageDiv.add(overlay);
			//overlay.getElement().focus();
			
			overlay.addMouseMoveHandler(mouseMoveHandler);
			overlay.addClickHandler(clickHandler);

			activeOverlay = overlay;

			lastActiveArea = index;
		}

		activeArea = index;
	}

		
	private void next() {
		if (activeArea != -1) {
			int nextActive = activeArea + 1;
			if (nextActive == definition.areas.length)
				nextActive = 0;
			setActiveArea(nextActive);
		}
	}

	private void prev() {
		if (activeArea != -1) {
			int nextActive = activeArea - 1;
			if (nextActive == -1)
				nextActive = definition.areas.length - 1;
			setActiveArea(nextActive);
		}
	}
	
	private void prefetchImages() {
		for (Area a: definition.areas) {
			Image.prefetch(a.overlayUrl);
		}
	}
	
	public ImageMap(final ImageMapDefinition definition, final ResultHandler handler) {
		this.definition = definition;

		imageDiv = new FlowPanel();
		imageDiv.addStyleName("imagemap-container");
		
		imageDiv.getElement().setTabIndex(1);
		
		final Image baseImage = new Image(definition.baseImageUrl);
		baseImage.addStyleName("imagemap-base-image");
		
		imageDiv.add(baseImage);
		
		mouseMoveHandler = new MouseMoveHandler() {
			@Override
			public void onMouseMove(MouseMoveEvent event) {
				
				//FIXME: hard-coded width hack
				double scale = 654.0 / baseImage.getOffsetWidth();
				
				int mouseX = (int)(event.getRelativeX(baseImage.getElement()) * scale);
				int mouseY = (int)(event.getRelativeY(baseImage.getElement()) * scale);

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
		
		prefetchImages();
	}


}