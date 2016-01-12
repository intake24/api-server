/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts;

import java.util.ArrayList;

import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.ShepherdTour;
import net.scran24.user.client.survey.RecipeManager;
import net.scran24.user.client.survey.prompts.messages.HelpMessages;
import net.scran24.user.shared.Recipe;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Callback1;

import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;

public class RecipeBrowser extends Composite {

	private static final HelpMessages helpMessages = HelpMessages.Util.getInstance();

	private static class RecipeButton {

		public final FlowPanel container;
		public final Label link;
		public final Button deleteButton;

		public RecipeButton(String recipeName) {
			container = new FlowPanel();

			link = new Label(SafeHtmlUtils.htmlEscape(recipeName));
			link.addStyleName("intake24-food-browser-recipe");

			deleteButton = new Button();
			deleteButton.getElement().addClassName("intake24-delete-recipe-button");

			container.add(deleteButton);
			container.add(link);

			hideDeleteButton();
		}

		public void showDeleteButton() {
			deleteButton.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
		}

		public void hideDeleteButton() {
			deleteButton.getElement().getStyle().setDisplay(Display.NONE);
		}
	}

	final private Callback1<Recipe> onRecipeChosen;

	private final FlowPanel contents = new FlowPanel();
	private final RecipeManager manager;
	private final Button showAllRecipesButton;
	private final Button deleteRecipesButton;
	private boolean deleting = false;

	private FlowPanel recipesPanel;
	private boolean noRecipes;

	private final ArrayList<RecipeButton> recipeButtons = new ArrayList<RecipeButton>();

	public RecipeBrowser(final Callback1<Recipe> onRecipeChosen, final RecipeManager manager) {
		this.onRecipeChosen = onRecipeChosen;
		this.manager = manager;

		contents.addStyleName("intake24-food-browser");

		showAllRecipesButton = WidgetFactory.createButton("Show all saved recipes", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				showRecipes(manager.getSavedRecipes());
				showAllRecipesButton.setVisible(false);
			}
		});

		showAllRecipesButton.getElement().setId("intake24-recipe-browser-show-all-button");

		deleteRecipesButton = WidgetFactory.createButton("Delete recipes", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (deleting) {
					deleteRecipesButton.setText("Delete recipes");
					deleting = false;

					for (RecipeButton r : recipeButtons)
						r.hideDeleteButton();
				} else {
					deleteRecipesButton.setText("Done");
					deleting = true;

					for (RecipeButton r : recipeButtons)
						r.showDeleteButton();
				}
			}
		});

		deleteRecipesButton.getElement().getStyle().setFloat(com.google.gwt.dom.client.Style.Float.RIGHT);
		deleteRecipesButton.getElement().setId("intake24-recipe-browser-delete-button");

		initWidget(contents);
	}

	private void showRecipes(PVector<Recipe> recipes) {
		contents.clear();
		recipeButtons.clear();

		showAllRecipesButton.setVisible(true);

		if (!manager.getSavedRecipes().isEmpty()) {
			contents.add(showAllRecipesButton);
			contents.add(deleteRecipesButton);
			noRecipes = false;
		} else
			noRecipes = true;

		if (!recipes.isEmpty()) {
			showAllRecipesButton.getElement().getStyle().setFloat(com.google.gwt.dom.client.Style.Float.RIGHT);

			recipesPanel = new FlowPanel();
			recipesPanel.addStyleName("intake24-food-browser-foods-container");
			recipesPanel.getElement().setId("intake24-recipe-browser-recipes-panel");

			HTMLPanel header = new HTMLPanel("h2", "Your recipes");
			recipesPanel.add(header);

			for (final Recipe recipe : recipes) {
				final RecipeButton recipeBlock = new RecipeButton(recipe.mainFood.description);

				recipeBlock.link.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						onRecipeChosen.call(recipe);
					}
				});

				recipeBlock.deleteButton.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						recipeButtons.remove(recipeBlock);
						recipesPanel.remove(recipeBlock.container);
						if (!manager.deleteRecipe(recipe.mainFood.link.id))
							contents.clear();
					}
				});

				recipesPanel.add(recipeBlock.container);
				recipeButtons.add(recipeBlock);
			}

			contents.add(recipesPanel);
		} else {
			recipesPanel = null;
			showAllRecipesButton.getElement().getStyle().setFloat(com.google.gwt.dom.client.Style.Float.NONE);
		}
	}

	public void lookup(String description) {
		showRecipes(manager.matchRecipes(description));
	}

	public PVector<ShepherdTour.Step> getShepherdTourSteps() {

		PVector<ShepherdTour.Step> result = TreePVector.<ShepherdTour.Step> empty();

		if (noRecipes)
			return result;

		if (recipesPanel != null) {
			ShepherdTour.makeShepherdTarget(recipesPanel);
			result = result.plus(new ShepherdTour.Step("recipeBrowser_recipes", "#intake24-recipe-browser-recipes-panel", helpMessages
					.recipeBrowser_recipesTitle(), helpMessages.recipeBrowser_recipesDescription()));

			ShepherdTour.makeShepherdTarget(deleteRecipesButton);
			result = result.plus(new ShepherdTour.Step("recipeBrowser_deleteButton", "#intake24-recipe-browser-delete-button", helpMessages
					.recipeBrowser_deleteButtonTitle(), helpMessages.recipeBrowser_deleteButtonDescription(), "top right", "bottom right"));
		}

		if (showAllRecipesButton.isVisible()) {
			ShepherdTour.makeShepherdTarget(showAllRecipesButton);
			result = result.plus(new ShepherdTour.Step("recipeBrowser_showAllButton", "#intake24-recipe-browser-show-all-button", helpMessages
					.recipeBrowser_showAllButtonTitle(), helpMessages.recipeBrowser_showAllButtonDescription(), "top right", "bottom right"));

		}

		return result;
	}
}
