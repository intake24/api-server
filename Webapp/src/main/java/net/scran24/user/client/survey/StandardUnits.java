package net.scran24.user.client.survey;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.ConstantsWithLookup;

public interface StandardUnits extends ConstantsWithLookup {

	public static class Util {
		private static StandardUnits instance = null;

		public static StandardUnits getInstance() {
			if (instance == null)
				instance = GWT.create(StandardUnits.class);
			return instance;
		}
	}

	public String one_inch_pieces_locative();

	public String one_inch_pieces_genitive();

	public String anchovies_locative();

	public String anchovies_genitive();

	public String apricots_locative();

	public String apricots_genitive();

	public String artichoke_hearts_locative();

	public String artichoke_hearts_genitive();

	public String aubergine_locative();

	public String aubergine_genitive();

	public String average_size_bags_locative();

	public String average_size_bags_genitive();

	public String average_sized_dim_sum_locative();

	public String average_sized_dim_sum_genitive();

	public String average_sized_flapjacks_locative();

	public String average_sized_flapjacks_genitive();

	public String average_spring_rolls_locative();

	public String average_spring_rolls_genitive();

	public String average_tubs___bags_locative();

	public String average_tubs___bags_genitive();

	public String bacon_and_cheese_grills_locative();

	public String bacon_and_cheese_grills_genitive();

	public String bagels_locative();

	public String bagels_genitive();

	public String bags_locative();

	public String bags_genitive();

	public String balls_locative();

	public String balls_genitive();

	public String bars_locative();

	public String bars_genitive();

	public String batons_locative();

	public String batons_genitive();

	public String beetroot_locative();

	public String beetroot_genitive();

	public String berries_locative();

	public String berries_genitive();

	public String biscuits_locative();

	public String biscuits_genitive();

	public String bites_locative();

	public String bites_genitive();

	public String blocks_locative();

	public String blocks_genitive();

	public String bunches_locative();

	public String bunches_genitive();

	public String burritos_locative();

	public String burritos_genitive();

	public String cakes_locative();

	public String cakes_genitive();

	public String cannelloni_tubes_locative();

	public String cannelloni_tubes_genitive();

	public String cartons_locative();

	public String cartons_genitive();

	public String cassavas_locative();

	public String cassavas_genitive();

	public String cherries_locative();

	public String cherries_genitive();

	public String chicken_livers_locative();

	public String chicken_livers_genitive();

	public String chillies_locative();

	public String chillies_genitive();

	public String chocolate_oranges_locative();

	public String chocolate_oranges_genitive();

	public String chocolates_locative();

	public String chocolates_genitive();

	public String cloves_locative();

	public String cloves_genitive();

	public String crackers_locative();

	public String crackers_genitive();

	public String cups_locative();

	public String cups_genitive();

	public String dates_locative();

	public String dates_genitive();

	public String double_measures_locative();

	public String double_measures_genitive();

	public String dough_balls_locative();

	public String dough_balls_genitive();

	public String dumplings_locative();

	public String dumplings_genitive();

	public String eggs_locative();

	public String eggs_genitive();

	public String enchiladas_locative();

	public String enchiladas_genitive();

	public String extra_large_bars_locative();

	public String extra_large_bars_genitive();

	public String extra_large_eggs_locative();

	public String extra_large_eggs_genitive();

	public String extra_large_triangles_locative();

	public String extra_large_triangles_genitive();

	public String fajitas_locative();

	public String fajitas_genitive();

	public String falafels_locative();

	public String falafels_genitive();

	public String fatty_cutties_locative();

	public String fatty_cutties_genitive();

	public String fillets_locative();

	public String fillets_genitive();

	public String fingers_locative();

	public String fingers_genitive();

	public String fritters_locative();

	public String fritters_genitive();

	public String fruits_locative();

	public String fruits_genitive();

	public String grapes_locative();

	public String grapes_genitive();

	public String handfuls_locative();

	public String handfuls_genitive();

	public String heaped_tablespoons_locative();

	public String heaped_tablespoons_genitive();

	public String heaped_teaspoons_locative();

	public String heaped_teaspoons_genitive();

	public String hot_pots_locative();

	public String hot_pots_genitive();

	public String ice_cream_sandwiches_locative();

	public String ice_cream_sandwiches_genitive();

	public String individual_packs_locative();

	public String individual_packs_genitive();

	public String individual_pies_locative();

	public String individual_pies_genitive();

	public String individual_pots_locative();

	public String individual_pots_genitive();

	public String individual_tarts_locative();

	public String individual_tarts_genitive();

	public String jars_locative();

	public String jars_genitive();

	public String kebabs_locative();

	public String kebabs_genitive();

	public String kingsize_pots_locative();

	public String kingsize_pots_genitive();

	public String large_aubergines_locative();

	public String large_aubergines_genitive();

	public String large_avocados_locative();

	public String large_avocados_genitive();

	public String large_bags_locative();

	public String large_bags_genitive();

	public String large_bananas_locative();

	public String large_bananas_genitive();

	public String large_bars_locative();

	public String large_bars_genitive();

	public String large_biscuits_locative();

	public String large_biscuits_genitive();

	public String large_bottles_locative();

	public String large_bottles_genitive();

	public String large_bowls_locative();

	public String large_bowls_genitive();

	public String large_carrots_locative();

	public String large_carrots_genitive();

	public String large_cartons_locative();

	public String large_cartons_genitive();

	public String large_chops_locative();

	public String large_chops_genitive();

	public String large_cobs_locative();

	public String large_cobs_genitive();

	public String large_crackers_locative();

	public String large_crackers_genitive();

	public String large_eggs_locative();

	public String large_eggs_genitive();

	public String large_fillets_locative();

	public String large_fillets_genitive();

	public String large_flapjacks_locative();

	public String large_flapjacks_genitive();

	public String large_gherkins_locative();

	public String large_gherkins_genitive();

	public String large_handfuls_locative();

	public String large_handfuls_genitive();

	public String large_hot_dogs_locative();

	public String large_hot_dogs_genitive();

	public String large_jars_locative();

	public String large_jars_genitive();

	public String large_kiwis_locative();

	public String large_kiwis_genitive();

	public String large_mushrooms_locative();

	public String large_mushrooms_genitive();

	public String large_naan_breads_locative();

	public String large_naan_breads_genitive();

	public String large_omelettes_4_eggs_locative();

	public String large_omelettes_4_eggs_genitive();

	public String large_onions_locative();

	public String large_onions_genitive();

	public String large_pancakes_locative();

	public String large_pancakes_genitive();

	public String large_parsnips_locative();

	public String large_parsnips_genitive();

	public String large_pastries_locative();

	public String large_pastries_genitive();

	public String large_pies_locative();

	public String large_pies_genitive();

	public String large_plantains_locative();

	public String large_plantains_genitive();

	public String large_portions_locative();

	public String large_portions_genitive();

	public String large_pots_locative();

	public String large_pots_genitive();

	public String large_scones_locative();

	public String large_scones_genitive();

	public String large_share_bags_locative();

	public String large_share_bags_genitive();

	public String large_skewers_locative();

	public String large_skewers_genitive();

	public String large_slices_locative();

	public String large_slices_genitive();

	public String large_spring_rolls_locative();

	public String large_spring_rolls_genitive();

	public String large_squares_locative();

	public String large_squares_genitive();

	public String large_steaks_locative();

	public String large_steaks_genitive();

	public String large_tarts_locative();

	public String large_tarts_genitive();

	public String large_truffles_locative();

	public String large_truffles_genitive();

	public String large_tubs_locative();

	public String large_tubs_genitive();

	public String leaves_locative();

	public String leaves_genitive();

	public String leeks_locative();

	public String leeks_genitive();

	public String level_tablespoons_locative();

	public String level_tablespoons_genitive();

	public String level_teaspoons_locative();

	public String level_teaspoons_genitive();

	public String lollipops_locative();

	public String lollipops_genitive();

	public String macaroons_locative();

	public String macaroons_genitive();

	public String mange_tout_locative();

	public String mange_tout_genitive();

	public String marshmallows_locative();

	public String marshmallows_genitive();

	public String meatballs_locative();

	public String meatballs_genitive();

	public String medium_aubergines_locative();

	public String medium_aubergines_genitive();

	public String medium_avocados_locative();

	public String medium_avocados_genitive();

	public String medium_bags_locative();

	public String medium_bags_genitive();

	public String medium_bananas_locative();

	public String medium_bananas_genitive();

	public String medium_bars_locative();

	public String medium_bars_genitive();

	public String medium_beetroot_locative();

	public String medium_beetroot_genitive();

	public String medium_biscuits_locative();

	public String medium_biscuits_genitive();

	public String medium_bowls_locative();

	public String medium_bowls_genitive();

	public String medium_carrots_locative();

	public String medium_carrots_genitive();

	public String medium_cartons_locative();

	public String medium_cartons_genitive();

	public String medium_chops_locative();

	public String medium_chops_genitive();

	public String medium_courgettes_locative();

	public String medium_courgettes_genitive();

	public String medium_eggs_locative();

	public String medium_eggs_genitive();

	public String medium_fillet_steaks_locative();

	public String medium_fillet_steaks_genitive();

	public String medium_fillets_locative();

	public String medium_fillets_genitive();

	public String medium_gherkins_locative();

	public String medium_gherkins_genitive();

	public String medium_handfuls_locative();

	public String medium_handfuls_genitive();

	public String medium_jars_locative();

	public String medium_jars_genitive();

	public String medium_kiwis_locative();

	public String medium_kiwis_genitive();

	public String medium_naans_locative();

	public String medium_naans_genitive();

	public String medium_omelettes_2_eggs_locative();

	public String medium_omelettes_2_eggs_genitive();

	public String medium_onions_locative();

	public String medium_onions_genitive();

	public String medium_parsnips_locative();

	public String medium_parsnips_genitive();

	public String medium_plantains_locative();

	public String medium_plantains_genitive();

	public String medium_portions_locative();

	public String medium_portions_genitive();

	public String medium_pots_locative();

	public String medium_pots_genitive();

	public String medium_scones_locative();

	public String medium_scones_genitive();

	public String medium_slices_locative();

	public String medium_slices_genitive();

	public String medium_steaks_locative();

	public String medium_steaks_genitive();

	public String medium_sundaes_locative();

	public String medium_sundaes_genitive();

	public String medium_tubs_locative();

	public String medium_tubs_genitive();

	public String meringues_locative();

	public String meringues_genitive();

	public String milles_feuilles_locative();

	public String milles_feuilles_genitive();

	public String mini_Oreos_locative();

	public String mini_Oreos_genitive();

	public String mini_bars_locative();

	public String mini_bars_genitive();

	public String mini_boxes_locative();

	public String mini_boxes_genitive();

	public String mini_churros_locative();

	public String mini_churros_genitive();

	public String mini_cobs_locative();

	public String mini_cobs_genitive();

	public String mini_eclairs_locative();

	public String mini_eclairs_genitive();

	public String mini_eggs_locative();

	public String mini_eggs_genitive();

	public String mini_fillets_locative();

	public String mini_fillets_genitive();

	public String mini_flapjacks_locative();

	public String mini_flapjacks_genitive();

	public String mini_macaroons_locative();

	public String mini_macaroons_genitive();

	public String mini_marshmallows_locative();

	public String mini_marshmallows_genitive();

	public String mini_pastries_locative();

	public String mini_pastries_genitive();

	public String mini_pots_locative();

	public String mini_pots_genitive();

	public String mini_skewers_locative();

	public String mini_skewers_genitive();

	public String mini_snack_packs_locative();

	public String mini_snack_packs_genitive();

	public String mini_spring_rolls_locative();

	public String mini_spring_rolls_genitive();

	public String mini_tubs_locative();

	public String mini_tubs_genitive();

	public String mints_locative();

	public String mints_genitive();

	public String mooncakes_locative();

	public String mooncakes_genitive();

	public String mozarella_balls_locative();

	public String mozarella_balls_genitive();

	public String mozzarella_sticks_locative();

	public String mozzarella_sticks_genitive();

	public String mugs_locative();

	public String mugs_genitive();

	public String multipack_bags_locative();

	public String multipack_bags_genitive();

	public String multipack_bottles_locative();

	public String multipack_bottles_genitive();

	public String mushrooms_locative();

	public String mushrooms_genitive();

	public String mussels_locative();

	public String mussels_genitive();

	public String nectarines_locative();

	public String nectarines_genitive();

	public String new_potatoes_locative();

	public String new_potatoes_genitive();

	public String nuts_locative();

	public String nuts_genitive();

	public String nuts_fruits_locative();

	public String nuts_fruits_genitive();

	public String olives_locative();

	public String olives_genitive();

	public String onion_rings_locative();

	public String onion_rings_genitive();

	public String onions_locative();

	public String onions_genitive();

	public String oysters_locative();

	public String oysters_genitive();

	public String packets_locative();

	public String packets_genitive();

	public String packs_locative();

	public String packs_genitive();

	public String pancakes_locative();

	public String pancakes_genitive();

	public String panna_cottas_locative();

	public String panna_cottas_genitive();

	public String pastries_locative();

	public String pastries_genitive();

	public String peaches_locative();

	public String peaches_genitive();

	public String peppers_locative();

	public String peppers_genitive();

	public String pieces_locative();

	public String pieces_genitive();

	public String pies_locative();

	public String pies_genitive();

	public String pigs_in_blankets_locative();

	public String pigs_in_blankets_genitive();

	public String pilchards_locative();

	public String pilchards_genitive();

	public String poppadums_locative();

	public String poppadums_genitive();

	public String portions_locative();

	public String portions_genitive();

	public String potato_skins_locative();

	public String potato_skins_genitive();

	public String potatoes_locative();

	public String potatoes_genitive();

	public String pots_locative();

	public String pots_genitive();

	public String pots_slices_locative();

	public String pots_slices_genitive();

	public String pouches_locative();

	public String pouches_genitive();

	public String prawns_locative();

	public String prawns_genitive();

	public String pretzels_locative();

	public String pretzels_genitive();

	public String profiteroles_locative();

	public String profiteroles_genitive();

	public String prunes_locative();

	public String prunes_genitive();

	public String punnets_locative();

	public String punnets_genitive();

	public String rashers_locative();

	public String rashers_genitive();

	public String regular_churros_locative();

	public String regular_churros_genitive();

	public String ribs_locative();

	public String ribs_genitive();

	public String rice_cakes_locative();

	public String rice_cakes_genitive();

	public String rings_locative();

	public String rings_genitive();

	public String rolls_locative();

	public String rolls_genitive();

	public String sachets_locative();

	public String sachets_genitive();

	public String sachets_made_up_with_milk_locative();

	public String sachets_made_up_with_milk_genitive();

	public String sandwiches_made_with_two_slices_of_bread_locative();

	public String sandwiches_made_with_two_slices_of_bread_genitive();

	public String sausages_locative();

	public String sausages_genitive();

	public String scallops_locative();

	public String scallops_genitive();

	public String scones_locative();

	public String scones_genitive();

	public String scoops_of_powder_locative();

	public String scoops_of_powder_genitive();

	public String segments_locative();

	public String segments_genitive();

	public String share_bags_locative();

	public String share_bags_genitive();

	public String sheets_locative();

	public String sheets_genitive();

	public String shots_locative();

	public String shots_genitive();

	public String shrimps_locative();

	public String shrimps_genitive();

	public String single_measures_locative();

	public String single_measures_genitive();

	public String slices_locative();

	public String slices_genitive();

	public String slices_one_12th_of_cake_locative();

	public String slices_one_12th_of_cake_genitive();

	public String slices_one_8th_of_cake_locative();

	public String slices_one_8th_of_cake_genitive();

	public String slices_one_8th_of_pie_locative();

	public String slices_one_8th_of_pie_genitive();

	public String slices_of_large_flatbread_locative();

	public String slices_of_large_flatbread_genitive();

	public String small_aubergines_locative();

	public String small_aubergines_genitive();

	public String small_avocados_locative();

	public String small_avocados_genitive();

	public String small_bags_locative();

	public String small_bags_genitive();

	public String small_bananas_locative();

	public String small_bananas_genitive();

	public String small_bars_locative();

	public String small_bars_genitive();

	public String small_beetroot_locative();

	public String small_beetroot_genitive();

	public String small_biscuits_locative();

	public String small_biscuits_genitive();

	public String small_bottles_locative();

	public String small_bottles_genitive();

	public String small_bowls_locative();

	public String small_bowls_genitive();

	public String small_carrots_locative();

	public String small_carrots_genitive();

	public String small_cartons_locative();

	public String small_cartons_genitive();

	public String small_chops_locative();

	public String small_chops_genitive();

	public String small_crepes_locative();

	public String small_crepes_genitive();

	public String small_eggs_locative();

	public String small_eggs_genitive();

	public String small_fillets_locative();

	public String small_fillets_genitive();

	public String small_flatbreads_locative();

	public String small_flatbreads_genitive();

	public String small_gherkins_locative();

	public String small_gherkins_genitive();

	public String small_handfuls_locative();

	public String small_handfuls_genitive();

	public String small_hot_dogs_locative();

	public String small_hot_dogs_genitive();

	public String small_individual_pavlovas_locative();

	public String small_individual_pavlovas_genitive();

	public String small_individual_tubs_locative();

	public String small_individual_tubs_genitive();

	public String small_jars_locative();

	public String small_jars_genitive();

	public String small_kiwis_locative();

	public String small_kiwis_genitive();

	public String small_mushrooms_locative();

	public String small_mushrooms_genitive();

	public String small_naans_locative();

	public String small_naans_genitive();

	public String small_omelettes_1_egg_locative();

	public String small_omelettes_1_egg_genitive();

	public String small_onions_locative();

	public String small_onions_genitive();

	public String small_packets_locative();

	public String small_packets_genitive();

	public String small_pancakes_locative();

	public String small_pancakes_genitive();

	public String small_parsnips_locative();

	public String small_parsnips_genitive();

	public String small_pies_locative();

	public String small_pies_genitive();

	public String small_plantains_locative();

	public String small_plantains_genitive();

	public String small_portions_locative();

	public String small_portions_genitive();

	public String small_pots_locative();

	public String small_pots_genitive();

	public String small_scones_locative();

	public String small_scones_genitive();

	public String small_slices_locative();

	public String small_slices_genitive();

	public String small_souffles_locative();

	public String small_souffles_genitive();

	public String small_squares_locative();

	public String small_squares_genitive();

	public String small_steaks_locative();

	public String small_steaks_genitive();

	public String small_sticks_locative();

	public String small_sticks_genitive();

	public String small_sundaes_locative();

	public String small_sundaes_genitive();

	public String small_tins_locative();

	public String small_tins_genitive();

	public String small_truffles_locative();

	public String small_truffles_genitive();

	public String small_tubs_locative();

	public String small_tubs_genitive();

	public String spears_locative();

	public String spears_genitive();

	public String sprigs_locative();

	public String sprigs_genitive();

	public String sprouts_locative();

	public String sprouts_genitive();

	public String squares_locative();

	public String squares_genitive();

	public String standard_bags_locative();

	public String standard_bags_genitive();

	public String standard_bars_locative();

	public String standard_bars_genitive();

	public String standard_boxes_locative();

	public String standard_boxes_genitive();

	public String standard_packs_locative();

	public String standard_packs_genitive();

	public String standard_size_bottles_locative();

	public String standard_size_bottles_genitive();

	public String standard_size_sticks_locative();

	public String standard_size_sticks_genitive();

	public String steaks_locative();

	public String steaks_genitive();

	public String sticks_locative();

	public String sticks_genitive();

	public String straws_locative();

	public String straws_genitive();

	public String stuffed_peppers_half_a_pepper_locative();

	public String stuffed_peppers_half_a_pepper_genitive();

	public String sweets_locative();

	public String sweets_genitive();

	public String tablespoons_locative();

	public String tablespoons_genitive();

	public String tablets_locative();

	public String tablets_genitive();

	public String tarts_locative();

	public String tarts_genitive();

	public String teaspoons_locative();

	public String teaspoons_genitive();

	public String thick_slices_locative();

	public String thick_slices_genitive();

	public String thin_slices_locative();

	public String thin_slices_genitive();

	public String tins_locative();

	public String tins_genitive();

	public String tomatoes_locative();

	public String tomatoes_genitive();

	public String treble_measures_locative();

	public String treble_measures_genitive();

	public String triangle_slices_half_a_piece_of_bread_locative();

	public String triangle_slices_half_a_piece_of_bread_genitive();

	public String tubs_locative();

	public String tubs_genitive();

	public String very_thick_slices_locative();

	public String very_thick_slices_genitive();

	public String vine_leaves_locative();

	public String vine_leaves_genitive();

	public String vol_au_vents_locative();

	public String vol_au_vents_genitive();

	public String wafers_locative();

	public String wafers_genitive();

	public String waffles_locative();

	public String waffles_genitive();

	public String wedges_locative();

	public String wedges_genitive();

	public String whole_cakes_locative();

	public String whole_cakes_genitive();

	public String whole_camemberts_locative();

	public String whole_camemberts_genitive();

	public String whole_large_pies_locative();

	public String whole_large_pies_genitive();

	public String whole_large_quiches_locative();

	public String whole_large_quiches_genitive();

	public String whole_large_tarts_locative();

	public String whole_large_tarts_genitive();

	public String whole_radishes_locative();

	public String whole_radishes_genitive();

	public String whole_rolls_locative();

	public String whole_rolls_genitive();

	public String whole_sausages_locative();

	public String whole_sausages_genitive();

	public String whole_small_quiches_locative();

	public String whole_small_quiches_genitive();

	public String whole_stuffed_peppers_locative();

	public String whole_stuffed_peppers_genitive();

	public String wings_locative();

	public String wings_genitive();

	public String yams_locative();

	public String yams_genitive();

}