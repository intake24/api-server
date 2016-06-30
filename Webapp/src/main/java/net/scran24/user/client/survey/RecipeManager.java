/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
 */

package net.scran24.user.client.survey;

import static org.workcraft.gwt.shared.client.CollectionUtils.filter;

import java.util.ArrayList;
import java.util.logging.Logger;

import net.scran24.common.client.CurrentUser;
import net.scran24.user.client.json.serialisable.recipes.SerialisableRecipes;
import net.scran24.user.client.json.serialisable.recipes.SerialisableRecipesCodec;
import net.scran24.user.client.survey.flat.StateManagerUtil;
import net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptManager;
import net.scran24.user.shared.Recipe;
import net.scran24.user.shared.UUID;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.core.client.GWT;

public class RecipeManager {

	private final static SerialisableRecipesCodec recipesCodec = GWT.create(SerialisableRecipesCodec.class);
	private final static Logger log = Logger.getLogger(RecipeManager.class.getSimpleName());

	private final PortionSizeScriptManager scriptManager;
	private final CompoundFoodTemplateManager templateManager;
	private final String keyPrefix = "intake24-recipes-";

	private final String scheme_id;
	private final String version_id;

	public RecipeManager(String scheme_id, String version_id, PortionSizeScriptManager scriptManager, CompoundFoodTemplateManager templateManager) {
		this.scheme_id = scheme_id;
		this.version_id = version_id;
		this.scriptManager = scriptManager;
		this.templateManager = templateManager;
	}

	private String localStorageKey() {
		return keyPrefix + CurrentUser.userInfo.userName;
	}

	public PVector<Recipe> getSavedRecipes() {
		Option<String> serialisedRecipes = StateManagerUtil.getItem(localStorageKey());

		return serialisedRecipes.accept(new Option.Visitor<String, PVector<Recipe>>() {
			@Override
			public PVector<Recipe> visitSome(String item) {
				try {
					SerialisableRecipes decoded = recipesCodec.decode(item);

					if (decoded.scheme_id != scheme_id || decoded.version_id != version_id) {
						log.warning("Version mismatch for recipes: stored version is (" + decoded.scheme_id + ", " + decoded.version_id
								+ "), runtime version is (" + scheme_id + ", " + version_id + "). Ignoring stored recipes.");
						return TreePVector.empty();
					} else
						return decoded.toRecipes(scriptManager, templateManager);
				} catch (Throwable e) {
					e.printStackTrace();
					log.warning("Deserialisation failed for same as before: " + e.getClass().getName() + " (" + e.getMessage() + ")");
					return TreePVector.empty();
				}
			}

			@Override
			public PVector<Recipe> visitNone() {
				return TreePVector.empty();
			}
		});
	}

	public void saveRecipe(Recipe recipe) {
		PVector<Recipe> newRecipes = getSavedRecipes().plus(recipe);
		StateManagerUtil.setItem(localStorageKey(), recipesCodec.encode(new SerialisableRecipes(scheme_id, version_id, newRecipes)).toString());
	}

	public boolean deleteRecipe(final UUID mainFood) {
		PVector<Recipe> newRecipes = filter(getSavedRecipes(), new Function1<Recipe, Boolean>() {
			@Override
			public Boolean apply(Recipe argument) {
				return !argument.mainFood.link.id.equals(mainFood);
			}
		});
		StateManagerUtil.setItem(localStorageKey(), recipesCodec.encode(new SerialisableRecipes(scheme_id, version_id, newRecipes)).toString());

		return !newRecipes.isEmpty();
	}

	public PVector<Recipe> matchRecipes(String description) {
		String[] tokens = description.split("\\s+");

		final ArrayList<String> filteredTokens = new ArrayList<String>();

		for (String t : tokens) {
			if (t.length() > 2)
				filteredTokens.add(t);
		}

		PVector<Recipe> savedRecipes = getSavedRecipes();

		return filter(savedRecipes, new Function1<Recipe, Boolean>() {
			@Override
			public Boolean apply(Recipe argument) {
				for (String token : filteredTokens) {

					if (argument.mainFood.description.toLowerCase().contains(token.toLowerCase()))
						return true;
				}

				return false;
			}
		});
	}

	public boolean recipeRecordExists() {
		return !StateManagerUtil.getItem(localStorageKey()).isEmpty();
	}
}
