/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey;


import java.util.ArrayList;

import net.scran24.common.client.CurrentUser;
import net.scran24.user.client.survey.flat.StateManagerUtil;
import net.scran24.user.client.survey.flat.SurveyXmlSerialiser;
import net.scran24.user.client.survey.flat.VersionMismatchException;
import net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptManager;
import net.scran24.user.shared.Recipe;
import net.scran24.user.shared.UUID;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.core.client.GWT;

import static org.workcraft.gwt.shared.client.CollectionUtils.filter;

public class RecipeManager {
	
	private final PortionSizeScriptManager scriptManager;
	private final CompoundFoodTemplateManager templateManager;
	private final String keyPrefix = "intake24-recipes-";
	
	public RecipeManager(PortionSizeScriptManager scriptManager, CompoundFoodTemplateManager templateManager) {
		this.scriptManager = scriptManager;
		this.templateManager = templateManager;
	}
	
	private String localStorageKey() {
		return keyPrefix + CurrentUser.userInfo.userName;		
	}

	public PVector<Recipe> getSavedRecipes() {		
		Option<String> recipesXml = StateManagerUtil.getItem(localStorageKey());
		
		return recipesXml.accept(new Option.Visitor<String, PVector<Recipe>>() {
			@Override
			public PVector<Recipe> visitSome(String item) {
				try {
					return SurveyXmlSerialiser.recipesFromXml(scriptManager, templateManager, item);
				} catch (VersionMismatchException e) {
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
		StateManagerUtil.setItem(localStorageKey(), SurveyXmlSerialiser.recipesToXml(newRecipes));				
	}
	
	public boolean deleteRecipe(final UUID mainFood) {
		PVector<Recipe> newRecipes = filter(getSavedRecipes(), new Function1<Recipe, Boolean>() {
			@Override
			public Boolean apply(Recipe argument) {
				return !argument.mainFood.link.id.equals(mainFood);
			}
		});
		StateManagerUtil.setItem(localStorageKey(), SurveyXmlSerialiser.recipesToXml(newRecipes));
		
		return !newRecipes.isEmpty();
	}
	
	public PVector<Recipe> matchRecipes(String description) {
		String[] tokens = description.split("\\s+");
		
		final ArrayList<String> filteredTokens = new ArrayList<String>();
		
		for (String t: tokens) {
			if (t.length() > 2)
				filteredTokens.add(t);
		}
		
		PVector<Recipe> savedRecipes = getSavedRecipes();
		
		return filter(savedRecipes, new Function1<Recipe, Boolean>() {
			@Override
			public Boolean apply(Recipe argument) {
				for (String token: filteredTokens) {
					
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
