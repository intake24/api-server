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
*/

package net.scran24.datastore.shared;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.scran24.datastore.shared.SurveySchemeReference.Visitor;

public class DataSchemeMap {
	public static final CustomDataScheme DEFAULT = new CustomDataScheme() {
		@Override
		public List<CustomFieldDef> userCustomFields() {
			return Collections.emptyList();
		}

		@Override
		public List<CustomFieldDef> surveyCustomFields() {
			return Collections.emptyList();
		}

		@Override
		public List<CustomFieldDef> mealCustomFields() {
			return Collections.emptyList();
		}

		@Override
		public List<CustomFieldDef> foodCustomFields() {
			return Collections.emptyList();
		}
	};

	public static final CustomDataScheme YOUNG_SCOT_2014 = new CustomDataScheme() {
		@Override
		public List<CustomFieldDef> userCustomFields() {
			return Arrays.asList(new CustomFieldDef[] { 
					new CustomFieldDef("age", "Age"), 
					new CustomFieldDef("gender", "Gender") ,
					new CustomFieldDef("postCode", "Post code"),
					new CustomFieldDef("schoolName", "School"),
					new CustomFieldDef("townName", "Town")
			});
		}

		@Override
		public List<CustomFieldDef> surveyCustomFields() {
			return Arrays.asList(new CustomFieldDef[] { 
					new CustomFieldDef("lunchSpend", "Avg. lunch spending"), 
					new CustomFieldDef("shopFreq", "Shop frequency") ,
					new CustomFieldDef("packFreq", "Packed frequency"),
					new CustomFieldDef("schoolLunchFreq", "School frequency"),
					new CustomFieldDef("homeFreq", "Home/friend frequency"),
					new CustomFieldDef("skipFreq", "Skip frequency"),
					new CustomFieldDef("workFreq", "Work through frequency"),
					new CustomFieldDef("reason", "Reason"),
					new CustomFieldDef("freeMeals", "Free school meals")
			});
		}

		@Override
		public List<CustomFieldDef> mealCustomFields() {
			return Arrays.asList(new CustomFieldDef[] {
					new CustomFieldDef("mealLocation", "Meal location"),
					new CustomFieldDef("shopSpending", "Spending at shop/restaurant"),
					new CustomFieldDef("schoolSpending", "Spending at school")
			});
		}

		@Override
		public List<CustomFieldDef> foodCustomFields() {
			return Arrays.asList(new CustomFieldDef[] {
					new CustomFieldDef("foodSource", "Food source")
			});
		}
	};
	
	public static final CustomDataScheme SHES_JUN_2015 = new CustomDataScheme() {

		@Override
		public List<CustomFieldDef> userCustomFields() {
			return Collections.emptyList();
		}

		@Override
		public List<CustomFieldDef> surveyCustomFields() {
			return Arrays.asList(new CustomFieldDef[] { 
					new CustomFieldDef("dayOfWeek", "Day of week"),
					new CustomFieldDef("usualFoods", "Usual foods"),
					new CustomFieldDef("foodAmount", "Food amount"),
					new CustomFieldDef("supplements", "Supplements"),
					new CustomFieldDef("diet", "Diet")});					
		}

		@Override
		public List<CustomFieldDef> mealCustomFields() {
			return Collections.emptyList();
		}

		@Override
		public List<CustomFieldDef> foodCustomFields() {
			return Collections.emptyList();
		}
		
	};
	
	public static final CustomDataScheme CROWDFLOWER_NOV_2015 = new CustomDataScheme() {

		@Override
		public List<CustomFieldDef> userCustomFields() {
			return Collections.emptyList();
		}

		@Override
		public List<CustomFieldDef> surveyCustomFields() {
			return Arrays.asList(new CustomFieldDef[] {
					new CustomFieldDef("external-user-id", "Crowdflower ID")					
			});			
		}

		@Override
		public List<CustomFieldDef> mealCustomFields() {
			return Collections.emptyList();
		}

		@Override
		public List<CustomFieldDef> foodCustomFields() {
			return Collections.emptyList();
		}
		
	};

	public static CustomDataScheme dataSchemeFor(SurveySchemeReference ref) {
		return ref.accept(new Visitor<CustomDataScheme>() {
			@Override
			public CustomDataScheme visitDefault() {
				return DEFAULT;
			}

			@Override
			public CustomDataScheme visitYoungScot() {
				return YOUNG_SCOT_2014;
			}

			@Override
			public CustomDataScheme visitUclJan15() {
				return DEFAULT;
			}
			
			@Override
			public CustomDataScheme visitSHeSJun15() {
				return SHES_JUN_2015;
			}

			@Override
			public CustomDataScheme visitCrowdflowerNov15() {
				return CROWDFLOWER_NOV_2015;
			}
		});
	}
}
