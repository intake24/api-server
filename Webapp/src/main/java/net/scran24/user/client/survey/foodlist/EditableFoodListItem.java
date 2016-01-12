/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.foodlist;

import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.CompoundFood;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.FoodLink;
import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.RawFood;

import org.pcollections.client.HashTreePMap;
import org.pcollections.client.HashTreePSet;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;
import org.workcraft.gwt.shared.client.Option.Visitor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;

public class EditableFoodListItem extends Composite {
	public final PromptMessages messages = GWT.create(PromptMessages.class);
	public final Option<FoodEntry> init;
	
	final public TextBox textBox;
	final public Button deleteButton;
	
	public boolean isChanged () {
		return !textBox.getText().isEmpty() && !textBox.getText().equals(messages.editMeal_listItemPlaceholder())
				&& init.accept(new Visitor<FoodEntry, Boolean>() {
					@Override
					public Boolean visitSome(FoodEntry item) {
						return !item.description().equals(textBox.getText());
					}

					@Override
					public Boolean visitNone() {
						return true;
					}
				});
	}
	
	public boolean isEmpty() {
		return textBox.getText().isEmpty() || textBox.getText().equals(messages.editMeal_listItemPlaceholder());
	}
	
	public void clearText() {
		textBox.setText("");
		textBox.removeStyleName("intake24-food-list-textbox-placeholder");
	}
	
	public void showPlaceholderText() {
		textBox.setText(messages.editMeal_listItemPlaceholder());
		textBox.addStyleName("intake24-food-list-textbox-placeholder");
	}
	
	public FoodEntry mkFoodEntry(final boolean markAsDrink) {
		return init.accept(new Option.Visitor<FoodEntry, FoodEntry>() {
			@Override
			public FoodEntry visitSome(FoodEntry existing) {
				if (isChanged())
					return new RawFood(existing.link, textBox.getText(), markAsDrink ? HashTreePSet.<String> empty().plus(RawFood.FLAG_DRINK)
							: HashTreePSet.<String> empty(), HashTreePMap.<String, String> empty());
				else
					return existing;
			}

			@Override
			public FoodEntry visitNone() {
				if (isChanged())
					return new RawFood(FoodLink.newUnlinked(), textBox.getText(), markAsDrink ? HashTreePSet.<String> empty().plus(RawFood.FLAG_DRINK)
							: HashTreePSet.<String> empty(), HashTreePMap.<String, String> empty());
				else
					throw new RuntimeException ("Cannot make food entry from an empty text box");
			}
		});
	}
	
	public void highlight (boolean highlighted) {
		if (highlighted) {
			addStyleName("intake24-food-list-selected-item");
			//textBox.addStyleName("scran24-food-list-highlight");
			deleteButton.getElement().getStyle().setVisibility(Visibility.VISIBLE);
		} else {
			removeStyleName("intake24-food-list-selected-item");
			//textBox.removeStyleName("scran24-food-list-highlight");
			deleteButton.getElement().getStyle().setVisibility(Visibility.HIDDEN);
		}
	}
	
	public EditableFoodListItem(Option<FoodEntry> init) {
		this.init = init;
		
		FlowPanel contents = new FlowPanel();
		
		initWidget(contents);
		
		final boolean isLinked = init.map(new Function1<FoodEntry, Boolean>() {
			@Override
			public Boolean apply(FoodEntry argument) {
				return argument.link.isLinked();				
			}
		}).getOrElse(false);
		
		final boolean isCompound = init.map(new Function1<FoodEntry, Boolean>() {
			@Override
			public Boolean apply(FoodEntry argument) {
				return argument.accept(new FoodEntry.Visitor<Boolean>() {
					@Override
					public Boolean visitRaw(RawFood food) {
						return false;
					}

					@Override
					public Boolean visitEncoded(EncodedFood food) {
						return false;
					}

					@Override
					public Boolean visitTemplate(TemplateFood food) {
						return true;
					}

					@Override
					public Boolean visitMissing(MissingFood food) {
						return false;
					}

					@Override
					public Boolean visitCompound(CompoundFood food) {
						return true;
					}
				});
			}
		}).getOrElse(false);
		
		addStyleName("intake24-food-list-item");
		
		if (isLinked)
			addStyleName("intake24-food-list-linked-item");
				
		
		textBox = new TextBox();
		textBox.setEnabled(!isCompound);
		textBox.addStyleName("intake24-food-list-textbox");
		
		textBox.setText(init.map(new Function1<FoodEntry, String>() {
			@Override
			public String apply(FoodEntry argument) {
				return argument.description();
			}
		}).getOrElse(messages.editMeal_listItemPlaceholder()));
	
		FlowPanel textBoxContainer = new FlowPanel();
		textBoxContainer.addStyleName("intake24-food-list-textbox-container");
		textBoxContainer.add(textBox);
		
		deleteButton = WidgetFactory.createButton("");
		deleteButton.getElement().getStyle().setVisibility(Visibility.HIDDEN);
		deleteButton.addStyleName("intake24-food-list-delete-button");
		// deleteButton.setTitle(messages.editMeal_listItem_deleteButtonLabel());
		
		contents.add(deleteButton);
		contents.add(textBoxContainer);
		
		FlowPanel clearDiv = new FlowPanel();
		clearDiv.addStyleName("intake24-clear-floats");		
		
		contents.add(clearDiv);
	}
}
