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

import org.pcollections.client.PVector;
import org.workcraft.gwt.shared.client.Function0;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.user.client.ui.Widget;

public class TemplateFoodData {
	public static enum ComponentOccurence {
		Single, Multiple
	}

	public static enum ComponentType {
		Optional, Required
	}

	public static class ComponentDef {
		public final String name;
		public final String primaryInstancePrompt;
		public final String secondaryInstancePrompt;
		public final String primarySkipButtonLabel;
		public final String secondarySkipButtonLabel;
		public final String foodsLabel;
		public final String dataSetLabel; 
		public final String categoriesLabel;
		public final String categoryCode;
		public final ComponentOccurence occurence;
		public final ComponentType type;
		public final Option<Function0<Widget>> headerConstructor;

		public ComponentDef(String name, String primaryInstancePrompt, String secondaryInstancePrompt, String primarySkipButtonLabel, String secondarySkipButtonLabel, String foodsLabel, String categoriesLabel,  String datasetLabel, String categoryCode, Option<Function0<Widget>> headerConstructor, ComponentOccurence occurence, ComponentType type) {			
			this.name = name;
			this.primaryInstancePrompt = primaryInstancePrompt;
			this.secondaryInstancePrompt = secondaryInstancePrompt;
			this.primarySkipButtonLabel = primarySkipButtonLabel;
			this.secondarySkipButtonLabel = secondarySkipButtonLabel;
			this.dataSetLabel = datasetLabel;
			this.foodsLabel = foodsLabel;
			this.categoriesLabel = categoriesLabel;
			this.categoryCode = categoryCode;
			this.headerConstructor = headerConstructor;
			this.occurence = occurence;
			this.type = type;
		}
	}

	public final String template_id;
	public final PVector<ComponentDef> template;

	public TemplateFoodData(String template_id, PVector<ComponentDef> template) {
		this.template_id = template_id;
		this.template = template;
	}
}