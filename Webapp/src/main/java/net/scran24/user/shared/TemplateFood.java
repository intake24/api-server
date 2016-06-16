/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
 */

package net.scran24.user.shared;

import static org.workcraft.gwt.shared.client.CollectionUtils.filter;
import static org.workcraft.gwt.shared.client.CollectionUtils.indexOf;
import static org.workcraft.gwt.shared.client.CollectionUtils.indices;
import static org.workcraft.gwt.shared.client.CollectionUtils.mapValues;
import static org.workcraft.gwt.shared.client.CollectionUtils.map;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.scran24.user.shared.TemplateFoodData.ComponentDef;
import net.scran24.user.shared.TemplateFoodData.ComponentOccurence;
import net.scran24.user.shared.TemplateFoodData.ComponentType;

import org.pcollections.client.HashTreePMap;
import org.pcollections.client.HashTreePSet;
import org.pcollections.client.PMap;
import org.pcollections.client.PSet;
import org.pcollections.client.PVector;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;
import org.workcraft.gwt.shared.client.Pair;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TemplateFood extends FoodEntry {

	@JsonProperty
	public final TemplateFoodData data;
	@JsonProperty
	public final PSet<Integer> markedAsComplete;
	@JsonProperty
	public final PMap<Integer, PSet<UUID>> components;
	@JsonProperty
	public final String description;
	@JsonProperty
	public final boolean isDrink;

	public TemplateFood(FoodLink link, String description, boolean isDrink, TemplateFoodData data) {
		super(link, HashTreePSet.<String> empty(), HashTreePMap.<String, String> empty());
		this.data = data;
		this.description = description;
		this.isDrink = isDrink;
		this.markedAsComplete = HashTreePSet.empty();

		PMap<Integer, PSet<UUID>> comp = HashTreePMap.empty();

		for (int i = 0; i < data.template.size(); i++)
			comp = comp.plus(i, HashTreePSet.<UUID> empty());

		this.components = comp;
	}

	private static PMap<Integer, PSet<UUID>> mapComponents(Map<Integer, Set<UUID>> components) {
		PMap<Integer, PSet<UUID>> result = HashTreePMap.<Integer, PSet<UUID>> empty();

		for (Integer key : components.keySet())
			result = result.plus(key, HashTreePSet.from(components.get(key)));

		return result;
	}

	@JsonCreator
	@Deprecated
	public TemplateFood(@JsonProperty("link") FoodLink link, @JsonProperty("description") String description,
			@JsonProperty("isDrink") boolean isDrink, @JsonProperty("data") TemplateFoodData data,
			@JsonProperty("completeComponents") Set<Integer> completeComponents, @JsonProperty("components") Map<Integer, Set<UUID>> components,
			@JsonProperty("flags") Set<String> flags, @JsonProperty("customData") Map<String, String> customData) {
		this(link, description, isDrink, data, HashTreePSet.from(completeComponents), mapComponents(components), HashTreePSet.from(flags),
				HashTreePMap.from(customData));
	}

	public TemplateFood(FoodLink link, String description, boolean isDrink, TemplateFoodData data, PSet<Integer> completeComponents,
			PMap<Integer, PSet<UUID>> components, PSet<String> flags, PMap<String, String> customData) {
		super(link, flags, customData);
		this.data = data;
		this.description = description;
		this.isDrink = isDrink;
		this.markedAsComplete = completeComponents;
		this.components = components;
	}

	@Override
	public String description() {
		return description;
	}

	@Override
	public boolean isDrink() {
		return isDrink;
	}

	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visitTemplate(this);
	}

	private boolean isComponentComplete(int index) {
		ComponentDef def = data.template.get(index);

		int count = components.get(index).size();

		if (def.type == ComponentType.Optional && markedAsComplete.contains(index)) {
			return true;
		} else {
			if (def.occurence == ComponentOccurence.Single)
				return count == 1;
			else
				return (count > 0) && markedAsComplete.contains(index);
		}
	}

	public Option<Integer> nextComponentIndex() {
		PVector<Integer> indices = indices(data.template);

		int index = indexOf(indices, new Function1<Integer, Boolean>() {
			@Override
			public Boolean apply(Integer argument) {
				return !isComponentComplete(argument);
			}
		});

		if (index == -1)
			return Option.none();
		else
			return Option.some(indices.get(index));
	}

	public boolean isTemplateComplete() {
		return nextComponentIndex().isEmpty();
	}

	public TemplateFood markAllComponentsComplete() {

		PSet<Integer> complete = markedAsComplete;

		for (int i = 0; i < data.template.size(); i++)
			complete = complete.plus(i);

		return new TemplateFood(link, description, isDrink, data, complete, components, flags, customData);
	}

	public TemplateFood markComponentComplete(int componentIndex) {
		return new TemplateFood(link, description, isDrink, data, markedAsComplete.plus(componentIndex), components, flags, customData);
	}

	public TemplateFood addComponent(int componentIndex, UUID linkedFood) {
		PSet<UUID> s = components.get(componentIndex);
		return new TemplateFood(link, description, isDrink, data, markedAsComplete, components.plus(componentIndex, s.plus(linkedFood)), flags,
				customData);
	}

	public TemplateFood removeComponent(final UUID linkedFood) {
		final PMap<Integer, PSet<UUID>> removed = mapValues(components, new Function1<PSet<UUID>, PSet<UUID>>() {
			@Override
			public PSet<UUID> apply(PSet<UUID> argument) {
				return argument.minus(linkedFood);
			}
		});

		final PMap<Integer, Boolean> affectedMap = mapValues(components, new Function1<PSet<UUID>, Boolean>() {
			@Override
			public Boolean apply(PSet<UUID> argument) {
				return argument.contains(linkedFood);
			}
		});

		PSet<Integer> affected = filter(HashTreePSet.<Integer> empty().plusAll(affectedMap.keySet()), new Function1<Integer, Boolean>() {
			@Override
			public Boolean apply(Integer argument) {
				return affectedMap.get(argument);
			}
		});

		return new TemplateFood(link, description, isDrink, data, markedAsComplete.minusAll(affected), removed, flags, customData);
	}

	@Override
	public TemplateFood relink(FoodLink link) {
		return new TemplateFood(link, description, isDrink, data, markedAsComplete, components, flags, customData);
	}

	@Override
	public TemplateFood withFlag(String flag) {
		return withFlags(flags.plus(flag));
	}

	@Override
	public TemplateFood withFlags(PSet<String> new_flags) {
		return new TemplateFood(link, description, isDrink, data, markedAsComplete, components, new_flags, customData);
	}

	@Override
	public TemplateFood withCustomDataField(String key, String value) {
		return new TemplateFood(link, description, isDrink, data, markedAsComplete, components, flags, customData.plus(key, value));
	}

	public TemplateFood minusCustomDataField(String key) {
		return new TemplateFood(link, description, isDrink, data, markedAsComplete, components, flags, customData.minus(key));
	}

	public TemplateFood withDescription(String description) {
		return new TemplateFood(link, description, isDrink, data, markedAsComplete, components, flags, customData);
	}

	public static Pair<TemplateFood, PVector<FoodEntry>> clone(Pair<TemplateFood, PVector<FoodEntry>> f) {

		final FoodLink newCompoundLink = FoodLink.newUnlinked();
		final HashMap<UUID, UUID> rename = new HashMap<UUID, UUID>();

		PVector<FoodEntry> renamed = map(f.right, new Function1<FoodEntry, FoodEntry>() {
			@Override
			public FoodEntry apply(FoodEntry argument) {
				FoodLink newLink = FoodLink.newLinked(newCompoundLink.id);
				rename.put(argument.link.id, newLink.id);
				return argument.relink(newLink);
			}
		});

		PMap<Integer, PSet<UUID>> newComponents = mapValues(f.left.components, new Function1<PSet<UUID>, PSet<UUID>>() {
			@Override
			public PSet<UUID> apply(PSet<UUID> argument) {
				return map(argument, new Function1<UUID, UUID>() {
					@Override
					public UUID apply(UUID argument) {
						return rename.get(argument);
					}
				});
			}
		});

		return Pair.create(new TemplateFood(newCompoundLink, f.left.description, f.left.isDrink, f.left.data, f.left.markedAsComplete, newComponents,
				f.left.flags, f.left.customData), renamed);
	}
}