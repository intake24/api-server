/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.gwt.imagechooser.client;

import org.workcraft.gwt.imagechooser.shared.ImageDef;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

public class ImageChooser extends Composite {
	public static interface ResultHandler {
		public void handleResult (int index);
	}
	
	FlowPanel[] images;
	Image[] thumbs;

	int index;
	
	boolean animation = false;
	
	public final Button nextButton;
	public final Button prevButton;
	public final Button confirmButton;
	public final FlowPanel imageContainer;
	public final FlowPanel thumbsContainer;

	private void transition(final int newIndex) {
		if (animation || index == newIndex)
			return;
		
		animation = true;
		
		if (newIndex == 0)
			prevButton.setEnabled(false);
		else
			prevButton.setEnabled(true);
		
		if (newIndex == images.length-1)
			nextButton.setEnabled(false);
		else
			nextButton.setEnabled(true);
		
		images[newIndex].getElement().getStyle().clearDisplay();
		images[newIndex].getElement().getStyle().setOpacity(0);
		images[newIndex].getElement().getStyle().setZIndex(400);
		images[newIndex].addStyleName("intake24-as-served-image-overlay");
		
		thumbs[newIndex].addStyleName("intake24-as-served-thumbnail-selected");
		
		images[index].getElement().getStyle().setZIndex(399);
		
		thumbs[index].removeStyleName("intake24-as-served-thumbnail-selected");
		
		Animation fadeIn = new Animation() {
			@Override
			protected void onUpdate(double progress) {
				images[newIndex].getElement().getStyle().setOpacity(progress);
			}
			
			@Override
			protected void onComplete() {
				images[newIndex].getElement().getStyle().setOpacity(1);
				images[newIndex].removeStyleName("intake24-as-served-image-overlay");
				
				images[index].getElement().getStyle().setDisplay(Display.NONE);
				images[index].getElement().getStyle().setZIndex(0);
				
				index = newIndex;
				animation = false;
			}
		};
		
		fadeIn.run(400);
	}
	
	private void less() {
		int newIndex = index;
		if (newIndex > 0) {
			newIndex--;
			transition (newIndex);
		}
	}

	private void next() {
		int newIndex = index;
		if (newIndex < images.length - 1) {
			newIndex++;
			transition (newIndex);
		}
	}

	public ImageChooser(final ImageDef[] imageDefs, String prevLabel, String nextLabel, String confirmLabel, int startIndex, final ResultHandler handler) {
		for (ImageDef img : imageDefs)
			Image.prefetch(img.url);
		
		FlowPanel contents = new FlowPanel();
		contents.addStyleName("intake24-as-served-image-chooser");

		imageContainer = new FlowPanel();
		imageContainer.addStyleName("intake24-as-served-image-container");
		imageContainer.getElement().setId("intake24-as-served-image-container");

		nextButton = new Button(nextLabel);
		
		nextButton.setStyleName("intake24-button");
		nextButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				next();
			}
		});
		nextButton.getElement().setId("intake24-as-served-next-button");
		
		confirmButton = new Button(confirmLabel);
		
		confirmButton.addStyleName("intake24-green-button");
		confirmButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				handler.handleResult(index);
			}
		});
		confirmButton.getElement().setId("intake24-as-served-confirm-button");
		
		prevButton = new Button(prevLabel);
		
		prevButton.addStyleName("intake24-button");
		prevButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				less();
			}
		});
		prevButton.getElement().setId("intake24-as-served-prev-button");
		
		thumbsContainer = new FlowPanel();
		thumbsContainer.addStyleName("intake24-as-served-thumbs-container");
		thumbsContainer.getElement().setId("intake24-as-served-thumbs-container");
				
		FlowPanel buttons = new FlowPanel();
		buttons.addStyleName("intake24-buttons-panel");
		buttons.add(prevButton);
		buttons.add(nextButton);
		buttons.add(confirmButton);

		contents.add(imageContainer);
		contents.add(thumbsContainer);
		contents.add(buttons);

		images = new FlowPanel[imageDefs.length];
		thumbs = new Image[imageDefs.length];

		for (int i = 0; i < imageDefs.length; i++) {
			images[i] = new FlowPanel();
   	  //images[i].addStyleName("intake24-as-served-image");
		  //images[i].getElement().getStyle().setPosition(Position.ABSOLUTE);
			images[i].getElement().getStyle().setDisplay(Display.NONE);
			
			Image image = new Image(imageDefs[i].url);
			image.addStyleName("intake24-as-served-image");
			images[i].add(image);
			
			Label label = new Label(imageDefs[i].label);
			
			label.addStyleName("intake24-as-served-image-label");
			label.getElement().setId("intake24-as-served-image-label");
			
			images[i].add(label);
			
			imageContainer.add(images[i]);
			
			thumbs[i] = new Image(imageDefs[i].thumbnailUrl);
			
			final int k = i;
			
			thumbs[i].addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					transition(k);					
				};
			});
			
			thumbs[i].addStyleName("intake24-as-served-thumbnail");
			thumbsContainer.add(thumbs[i]);			
		}

		images[startIndex].getElement().getStyle().clearDisplay();
		thumbs[startIndex].addStyleName("intake24-as-served-thumbnail-selected");
		
		index = startIndex;

		initWidget(contents);
	}
}