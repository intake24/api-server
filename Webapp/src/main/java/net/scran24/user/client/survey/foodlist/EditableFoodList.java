/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.foodlist;

import java.util.ArrayList;
import java.util.List;

import net.scran24.common.client.IEHack;
import net.scran24.common.client.UnorderedList;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.UUID;

import org.pcollections.client.PVector;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;

public class EditableFoodList extends Composite {
	private final UnorderedList<EditableFoodListItem> foodList;
	private final EditableFoodListItem newItem;
	private final boolean markAsDrink;
	private final Callback1<List<FoodEntry>> onChange;
	
	private boolean indentLinkedFoods = true;

	public EditableFoodList(PVector<FoodEntry> foods, Function1<FoodEntry, Boolean> filter, String addButtonText, final boolean markAsDrink,
			Callback1<List<FoodEntry>> onChange) {
		this(addButtonText, markAsDrink, onChange);

		for (FoodEntry food : foods) {
			if (filter.apply(food) && !food.link.isLinked()) {
				addItem(Option.some(food));

				for (FoodEntry linked : Meal.linkedFoods(foods, food)) {
					addItem(Option.some(linked));
				}
			}
		}
	}

	private void deselect() {
		for (EditableFoodListItem i : foodList.getItems()) {
			i.highlight(false);
		}
	}

	private void select(EditableFoodListItem item) {
		deselect();
		item.highlight(true);
	}

	private void acceptNewItem() {
		if (newItem.isEmpty())
				return;
		addItem(Option.some(newItem.mkFoodEntry(markAsDrink)));
		newItem.clearText();
		focusNew();
		onChange.call(getEnteredItems());
	}

	public EditableFoodList(String addButtonText, final boolean markAsDrink, Callback1<List<FoodEntry>> onChange) {
		this.markAsDrink = markAsDrink;
		this.onChange = onChange;

		FlowPanel contents = new FlowPanel();
		initWidget(contents);

		foodList = new UnorderedList<EditableFoodListItem>();

		foodList.sinkEvents(Event.ONMOUSEOUT);
		foodList.addHandler(new MouseOutHandler() {

			@Override
			public void onMouseOut(MouseOutEvent event) {
				deselect();
			}
		}, MouseOutEvent.getType());

		newItem = new EditableFoodListItem(Option.<FoodEntry> none());
		newItem.addStyleName("intake24-food-list-new-item");
		newItem.textBox.addStyleName("intake24-food-list-textbox-new-item");

		newItem.deleteButton.removeStyleName("intake24-food-list-delete-button");
		newItem.deleteButton.addStyleName("intake24-food-list-accept-button");
		newItem.deleteButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				acceptNewItem();
			}
		});
		
		newItem.textBox.addKeyUpHandler(new KeyUpHandler() {
			@Override
			public void onKeyUp(KeyUpEvent event) {
				if (newItem.isEmpty())
					newItem.deleteButton.getElement().getStyle().setVisibility(Visibility.HIDDEN);
				else
					newItem.deleteButton.getElement().getStyle().setVisibility(Visibility.VISIBLE);
			}
		});
		
		UnorderedList<EditableFoodListItem> newItemContainer = new UnorderedList<EditableFoodListItem>();
		newItemContainer.addItem(newItem);

		newItem.showPlaceholderText();

		newItem.textBox.addKeyUpHandler(new KeyUpHandler() {
			@Override
			public void onKeyUp(KeyUpEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER && newItem.isChanged()) {
					acceptNewItem();
				} else if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE) {
					newItem.clearText();
					newItem.textBox.setFocus(false);
				}
			}
		});

		newItem.textBox.addFocusHandler(new FocusHandler() {
			@Override
			public void onFocus(FocusEvent event) {
				if (newItem.isEmpty())
					newItem.clearText();
			}
		});

		newItem.textBox.addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				if (newItem.isEmpty())
					newItem.showPlaceholderText();
			}
		});

		contents.add(foodList);
		contents.add(newItemContainer);
	}

	public List<EditableFoodListItem> linkedItems(EditableFoodListItem item) {
		final ArrayList<EditableFoodListItem> result = new ArrayList<EditableFoodListItem>();

		item.init.accept(new Option.SideEffectVisitor<FoodEntry>() {
			@Override
			public void visitSome(final FoodEntry item) {
				for (final EditableFoodListItem other : foodList.getItems()) {
					other.init.accept(new Option.SideEffectVisitor<FoodEntry>() {
						@Override
						public void visitSome(FoodEntry item2) {
							item2.link.linkedTo.accept(new Option.SideEffectVisitor<UUID>() {
								@Override
								public void visitSome(UUID uuid) {
									if (uuid.equals(item.link.id))
										result.add(other);
								}

								@Override
								public void visitNone() {
								}
							});
						}

						@Override
						public void visitNone() {
						}
					});
				}
			}

			@Override
			public void visitNone() {
			}
		});

		return result;
	}

	public void removeItem(EditableFoodListItem item) {
		if (item == newItem)
			return;

		foodList.removeItem(item);

		for (EditableFoodListItem i : linkedItems(item))
			removeItem(i);

		IEHack.forceReflow();
	}

	public EditableFoodListItem addItem(Option<FoodEntry> init) {
		final EditableFoodListItem item = new EditableFoodListItem(init);
		
		if (!indentLinkedFoods)
			item.removeStyleName("intake24-food-list-linked-item");

		item.textBox.addKeyUpHandler(new KeyUpHandler() {
			@Override
			public void onKeyUp(KeyUpEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					item.textBox.setFocus(false);
				} else if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE) {
					item.textBox.setFocus(false);
				}
			}
		});

		item.textBox.addFocusHandler(new FocusHandler() {
			@Override
			public void onFocus(FocusEvent event) {
				select(item);
			}
		});

		item.textBox.addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				if (item.isEmpty()) {
					removeItem(item);
					onChange.call(getEnteredItems());
				} else if (item.isChanged()) {
					onChange.call(getEnteredItems());
				}
			}
		});

		item.sinkEvents(Event.ONMOUSEOVER);

		item.addHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				select(item);
			}
		}, MouseOverEvent.getType());

		item.deleteButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				removeItem(item);
				onChange.call(getEnteredItems());
			}
		});

		foodList.addItem(item);

		IEHack.forceReflowDeferred();

		return item;
	}
	
	public void disableLinkedFoodsIndentation() {
		indentLinkedFoods = false;
	}

	public List<FoodEntry> getEnteredItems() {
		final ArrayList<FoodEntry> result = new ArrayList<FoodEntry>();
		for (final EditableFoodListItem i : foodList.getItems()) {
			result.add(i.mkFoodEntry(markAsDrink));
		}

		if (!newItem.isEmpty())
			result.add(newItem.mkFoodEntry(markAsDrink));

		return result;
	}

	public void focusNew() {
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			public void execute() {
				// IEHack.forceReflow();
				newItem.textBox.setFocus(true);
			}
		});
	}
}
