package net.scran24.user.client.json.serialisable.sameasbefore;

import java.util.List;

import net.scran24.user.client.json.serialisable.SerialisableEncodedFood;
import net.scran24.user.client.json.serialisable.SerialisableFoodEntry;
import net.scran24.user.client.survey.CompoundFoodTemplateManager;
import net.scran24.user.client.survey.flat.SameAsBefore;
import net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptManager;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SerialisableSameAsBefore {
	@JsonProperty
	final public String scheme_id;
	@JsonProperty
	final public String version_id;
	@JsonProperty
	final public SerialisableEncodedFood mainFood;
	@JsonProperty
	final public PVector<SerialisableFoodEntry> linkedFoods;
	
	@JsonCreator 
	public SerialisableSameAsBefore(
			@JsonProperty("scheme_id") String scheme_id, @JsonProperty("version_id") String version_id, 
			@JsonProperty("mainFood") SerialisableEncodedFood mainFood, @JsonProperty("linkedFoods") List<SerialisableFoodEntry> linkedFoods) {
		this.scheme_id = scheme_id;
		this.version_id = version_id;
		this.mainFood = mainFood;
		this.linkedFoods = TreePVector.from(linkedFoods);
	}
	
	public SerialisableSameAsBefore(EncodedFood mainFood, PVector<FoodEntry> linkedFoods, String scheme_id, String version_id) {	
		this.scheme_id = scheme_id;
		this.version_id = version_id;
		this.mainFood = new SerialisableEncodedFood(mainFood);
		this.linkedFoods = SerialisableFoodEntry.toSerialisable(linkedFoods);
	}
	
	public SameAsBefore toSameAsBefore(PortionSizeScriptManager scriptManager, CompoundFoodTemplateManager templateManager) {
		return new SameAsBefore(mainFood.toEncodedFood(scriptManager), SerialisableFoodEntry.toRuntime(linkedFoods, scriptManager, templateManager));
	}	
}
