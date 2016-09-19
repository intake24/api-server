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

package net.scran24.user.client.survey;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.ConstantsWithLookup;

public interface PortionDescriptions extends ConstantsWithLookup {

	public static class Util {
		private static PortionDescriptions instance = null;

		public static PortionDescriptions getInstance() {
			if (instance == null)
				instance = GWT.create(PortionDescriptions.class);
			return instance;
		}
	}

	public String grated();
	public String in_a_bag();
	public String in_a_bottle();
	public String in_a_bowl();
	public String in_a_can();
	public String in_a_carton();
	public String in_a_glass();
	public String in_a_mug();
	public String in_a_pot();
	public String in_a_takeaway_cup();
	public String in_baby_carrots();
	public String in_bars();
	public String in_batons();
	public String in_berries();
	public String in_burgers();
	public String in_chopped_fruit();
	public String in_crinkle_cut_chips();
	public String in_cubes();
	public String in_curly_fries();
	public String in_dollops();
	public String in_french_fries();
	public String in_individual_cakes();
	public String in_individual_packs();
	public String in_individual_puddings();
	public String in_individual_sweets();
	public String in_slices();
	public String in_spoonfuls();
	public String in_straight_cut_chips();
	public String in_thick_cut_chips();
	public String in_unwrapped_bars();
	public String in_whole_fruit_vegetables();
	public String in_wrapped_bars();
	public String on_a_knife();
	public String on_a_plate();
	public String slice_from_a_large_cake();
	public String slice_from_a_large_pudding();
	public String spread_on_a_cracker();
	public String spread_on_a_scone();
	public String spread_on_bread();
	public String use_a_standard_measure();
	public String use_a_standard_portion();
	public String use_an_image();
	public String use_these_crisps_in_a_bag();
	public String use_tortilla_chips_in_a_bowl();
	public String weight();
}
