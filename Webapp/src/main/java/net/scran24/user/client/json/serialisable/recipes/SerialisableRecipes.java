package net.scran24.user.client.json.serialisable.recipes;

import static org.workcraft.gwt.shared.client.CollectionUtils.map;

import java.util.List;

import net.scran24.user.client.survey.CompoundFoodTemplateManager;
import net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptManager;
import net.scran24.user.shared.Recipe;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Function1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SerialisableRecipes {
	
	@JsonProperty
	public final String version_id;
	@JsonProperty
	public final String scheme_id;
	@JsonProperty
	public final PVector<SerialisableRecipe> recipes;
	
	@JsonCreator
	public SerialisableRecipes(@JsonProperty("scheme_id") String scheme_id, @JsonProperty("version_id") String version_id, @JsonProperty("recipes") List<SerialisableRecipe> recipes) {
		this.scheme_id = scheme_id;
		this.version_id = version_id;
		this.recipes = TreePVector.from(recipes);
	}
	
	public SerialisableRecipes(String scheme_id, String version_id, PVector<Recipe> recipes) {
		this.scheme_id = scheme_id;
		this.version_id = version_id;
		this.recipes = map(recipes, new Function1<Recipe, SerialisableRecipe>() {
			@Override
			public SerialisableRecipe apply(Recipe argument) {
				return new SerialisableRecipe(argument);
			}
		});
	}
	
	public PVector<Recipe> toRecipes(final PortionSizeScriptManager scriptManager, final CompoundFoodTemplateManager templateManager) {
		return map(recipes, new Function1<SerialisableRecipe, Recipe>() {
			@Override
			public Recipe apply(SerialisableRecipe argument) {
				return argument.toRecipe(scriptManager, templateManager);
			}			
		});
	}
}
