/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
 */

package net.scran24.user.client.survey.flat.serialisable;

import java.util.Map;
import java.util.Set;

import net.scran24.user.client.survey.CompoundFoodTemplateManager;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.UUID;

import org.pcollections.client.HashTreePMap;
import org.pcollections.client.HashTreePSet;
import org.pcollections.client.PMap;
import org.pcollections.client.PSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SerialisableTemplateFood extends SerialisableFoodEntry {

	@JsonProperty
	public final String template_id;
	@JsonProperty
	public final PSet<Integer> markedAsComplete;
	@JsonProperty
	public final PMap<Integer, PSet<UUID>> components;
	@JsonProperty
	public final String description;
	@JsonProperty
	public final boolean isDrink;
	
	private static PMap<Integer, PSet<UUID>> mapComponents(Map<Integer, Set<UUID>> components) {
		PMap<Integer, PSet<UUID>> result = HashTreePMap.<Integer, PSet<UUID>> empty();

		for (Integer key : components.keySet())
			result = result.plus(key, HashTreePSet.from(components.get(key)));

		return result;
	}

	@JsonCreator
	public SerialisableTemplateFood(
			@JsonProperty("link") SerialisableFoodLink link, 
			@JsonProperty("description") String description,
			@JsonProperty("isDrink") boolean isDrink, 
			@JsonProperty("template_id") String template_id,
			@JsonProperty("markedAsComplete") Set<Integer> markedAsComplete, 
			@JsonProperty("components") Map<Integer, Set<UUID>> components,
			@JsonProperty("flags") Set<String> flags, 
			@JsonProperty("customData") Map<String, String> customData) {
		super(link, HashTreePSet.from(flags), HashTreePMap.from(customData));

		this.description = description;
		this.template_id = template_id;
		this.isDrink = isDrink;
		this.markedAsComplete = HashTreePSet.from(markedAsComplete);
		this.components =  mapComponents(components);
	}
	
	public SerialisableTemplateFood(TemplateFood food) {
		super(new SerialisableFoodLink(food.link), food.flags, food.customData);
		this.description = food.description;
		this.template_id = food.data.template_id;
		this.isDrink = food.isDrink;
		this.markedAsComplete = food.markedAsComplete;
		this.components = food.components;
	}
	
	public TemplateFood toTemplateFood(CompoundFoodTemplateManager templateManager) {
		return new TemplateFood(link.toFoodLink(), description, isDrink, templateManager.getTemplate(template_id));
	}

	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visitTemplate(this);
	}

}