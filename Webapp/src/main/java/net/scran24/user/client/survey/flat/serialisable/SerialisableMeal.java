/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

This file is based on Intake24 v1.0.

© Crown copyright, 2012, 2013, 2014

Licensed under the Open Government Licence 3.0: 

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat.serialisable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.scran24.common.client.LocaleUtil;
import net.scran24.datastore.shared.Time;
import net.scran24.user.shared.CompoundFood;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.RawFood;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.UUID;

import org.pcollections.client.HashTreePMap;
import org.pcollections.client.HashTreePSet;
import org.pcollections.client.PMap;
import org.pcollections.client.PSet;
import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.CollectionUtils;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;


public class SerialisableMeal {
	
	@JsonProperty
	public final String name;
	@JsonProperty
	public final PVector<SerialisableFoodEntry> foods;
	@JsonProperty
	public final Option<Time> time;
	@JsonProperty
	public final PSet<String> flags;
	@JsonProperty
	public final PMap<String, String> customData;
	
	@JsonCreator
	public SerialisableMeal(
			@JsonProperty("name") String name,
			@JsonProperty("foods") List<SerialisableFoodEntry> foods,
			@JsonProperty("time") time) {
		// TODO Auto-generated constructor stub
	}
	
	
}