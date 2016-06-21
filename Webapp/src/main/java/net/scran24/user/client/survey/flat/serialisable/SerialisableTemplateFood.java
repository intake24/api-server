/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
 */

package net.scran24.user.client.survey.flat.serialisable;

import static org.workcraft.gwt.shared.client.CollectionUtils.map;
import static org.workcraft.gwt.shared.client.CollectionUtils.mapValues;

import java.util.Map;
import java.util.Set;

import net.scran24.user.client.survey.CompoundFoodTemplateManager;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.UUID;

import org.pcollections.client.HashTreePMap;
import org.pcollections.client.HashTreePSet;
import org.pcollections.client.PMap;
import org.pcollections.client.PSet;
import org.workcraft.gwt.shared.client.Function1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("template")
public class SerialisableTemplateFood extends SerialisableFoodEntry {

	@JsonProperty
	public final String template_id;
	@JsonProperty
	public final PSet<Integer> markedAsComplete;
	@JsonProperty
	public final PMap<Integer, PSet<String>> components;
	@JsonProperty
	public final String description;
	@JsonProperty
	public final boolean isDrink;
	
	private static PMap<Integer, PSet<String>> mapComponents(Map<Integer, Set<String>> components) {
		PMap<Integer, PSet<String>> result = HashTreePMap.<Integer, PSet<String>> empty();

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
			@JsonProperty("components") Map<Integer, Set<String>> components,
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
		this.components = mapValues(food.components, new Function1<PSet<UUID>, PSet<String>>() {
			@Override
			public PSet<String> apply(PSet<UUID> argument) {
				return map(argument, new Function1<UUID, String>() {
					@Override
					public String apply(UUID argument) {
						return argument.value;
					}					
				});
			}			
		});
	}
	
	public TemplateFood toTemplateFood(CompoundFoodTemplateManager templateManager) {
		
		PMap<Integer, PSet<UUID>> tfComponents = mapValues(components, new Function1<PSet<String>, PSet<UUID>>() {
			@Override
			public PSet<UUID> apply(PSet<String> argument) {
				return map(argument, new Function1<String, UUID>() {
					@Override
					public UUID apply(String argument) {
						return new UUID(argument);
					}					
				});
			}
		});
		
		return new TemplateFood(link.toFoodLink(), description, isDrink, templateManager.getTemplate(template_id), markedAsComplete, tfComponents, flags, customData);
	}

	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visitTemplate(this);
	}

}