package net.scran24.user.client.json.serialisable.recipes;

import java.util.List;

import net.scran24.user.client.json.serialisable.SerialisableFoodEntry;
import net.scran24.user.client.json.serialisable.SerialisableTemplateFood;
import net.scran24.user.client.survey.CompoundFoodTemplateManager;
import net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptManager;
import net.scran24.user.shared.Recipe;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SerialisableRecipe {
	
	@JsonProperty
	public final SerialisableTemplateFood mainFood;
	@JsonProperty
	public final PVector<SerialisableFoodEntry> ingredients;
	
	@JsonCreator
	public SerialisableRecipe(@JsonProperty("mainFood") SerialisableTemplateFood mainFood, @JsonProperty("ingredients") List<SerialisableFoodEntry> ingredients) {
		this.mainFood = mainFood;
		this.ingredients = TreePVector.from(ingredients);
	}
	
	public SerialisableRecipe(Recipe recipe) {
		this.mainFood = new SerialisableTemplateFood(recipe.mainFood);
		this.ingredients = SerialisableFoodEntry.toSerialisable(recipe.ingredients);
	}
	
	public Recipe toRecipe(PortionSizeScriptManager scriptManager, CompoundFoodTemplateManager templateManager) {
		return new Recipe(mainFood.toTemplateFood(templateManager), SerialisableFoodEntry.toRuntime(ingredients, scriptManager, templateManager));
	}
}
