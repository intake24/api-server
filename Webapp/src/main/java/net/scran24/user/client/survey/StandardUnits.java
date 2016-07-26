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
	public String one_inch_pieces_estimate_in();
	public String one_inch_pieces_how_many();

	public String anchovies_estimate_in();
	public String anchovies_how_many();

	public String apricots_estimate_in();
	public String apricots_how_many();

	public String artichoke_hearts_estimate_in();
	public String artichoke_hearts_how_many();

	public String aubergine_estimate_in();
	public String aubergine_how_many();

	public String average_size_bags_estimate_in();
	public String average_size_bags_how_many();

	public String average_sized_dim_sum_estimate_in();
	public String average_sized_dim_sum_how_many();

	public String average_sized_flapjacks_estimate_in();
	public String average_sized_flapjacks_how_many();

	public String average_spring_rolls_estimate_in();
	public String average_spring_rolls_how_many();

	public String average_tubs___bags_estimate_in();
	public String average_tubs___bags_how_many();

	public String bacon_and_cheese_grills_estimate_in();
	public String bacon_and_cheese_grills_how_many();

	public String bagels_estimate_in();
	public String bagels_how_many();

	public String bags_estimate_in();
	public String bags_how_many();

	public String balls_estimate_in();
	public String balls_how_many();

	public String bars_estimate_in();
	public String bars_how_many();

	public String batons_estimate_in();
	public String batons_how_many();

	public String beetroot_estimate_in();
	public String beetroot_how_many();

	public String berries_estimate_in();
	public String berries_how_many();

	public String biscuits_estimate_in();
	public String biscuits_how_many();

	public String bites_estimate_in();
	public String bites_how_many();

	public String blocks_estimate_in();
	public String blocks_how_many();

	public String bunches_estimate_in();
	public String bunches_how_many();

	public String burritos_estimate_in();
	public String burritos_how_many();

	public String cakes_estimate_in();
	public String cakes_how_many();

	public String cannelloni_tubes_estimate_in();
	public String cannelloni_tubes_how_many();

	public String cartons_estimate_in();
	public String cartons_how_many();

	public String cassavas_estimate_in();
	public String cassavas_how_many();

	public String cherries_estimate_in();
	public String cherries_how_many();

	public String chicken_livers_estimate_in();
	public String chicken_livers_how_many();

	public String chillies_estimate_in();
	public String chillies_how_many();

	public String chocolate_oranges_estimate_in();
	public String chocolate_oranges_how_many();

	public String chocolates_estimate_in();
	public String chocolates_how_many();

	public String cloves_estimate_in();
	public String cloves_how_many();

	public String crackers_estimate_in();
	public String crackers_how_many();

	public String cups_estimate_in();
	public String cups_how_many();

	public String dates_estimate_in();
	public String dates_how_many();

	public String double_measures_estimate_in();
	public String double_measures_how_many();

	public String dough_balls_estimate_in();
	public String dough_balls_how_many();

	public String dumplings_estimate_in();
	public String dumplings_how_many();

	public String eggs_estimate_in();
	public String eggs_how_many();

	public String enchiladas_estimate_in();
	public String enchiladas_how_many();

	public String extra_large_bars_estimate_in();
	public String extra_large_bars_how_many();

	public String extra_large_eggs_estimate_in();
	public String extra_large_eggs_how_many();

	public String extra_large_triangles_estimate_in();
	public String extra_large_triangles_how_many();

	public String fajitas_estimate_in();
	public String fajitas_how_many();

	public String falafels_estimate_in();
	public String falafels_how_many();

	public String fatty_cutties_estimate_in();
	public String fatty_cutties_how_many();

	public String fillets_estimate_in();
	public String fillets_how_many();

	public String fingers_estimate_in();
	public String fingers_how_many();

	public String fritters_estimate_in();
	public String fritters_how_many();

	public String fruits_estimate_in();
	public String fruits_how_many();

	public String grapes_estimate_in();
	public String grapes_how_many();

	public String handfuls_estimate_in();
	public String handfuls_how_many();

	public String heaped_tablespoons_estimate_in();
	public String heaped_tablespoons_how_many();

	public String heaped_teaspoons_estimate_in();
	public String heaped_teaspoons_how_many();

	public String hot_pots_estimate_in();
	public String hot_pots_how_many();

	public String ice_cream_sandwiches_estimate_in();
	public String ice_cream_sandwiches_how_many();

	public String individual_packs_estimate_in();
	public String individual_packs_how_many();

	public String individual_pies_estimate_in();
	public String individual_pies_how_many();

	public String individual_pots_estimate_in();
	public String individual_pots_how_many();

	public String individual_tarts_estimate_in();
	public String individual_tarts_how_many();

	public String jars_estimate_in();
	public String jars_how_many();

	public String kebabs_estimate_in();
	public String kebabs_how_many();

	public String kingsize_pots_estimate_in();
	public String kingsize_pots_how_many();

	public String large_aubergines_estimate_in();
	public String large_aubergines_how_many();

	public String large_avocados_estimate_in();
	public String large_avocados_how_many();

	public String large_bags_estimate_in();
	public String large_bags_how_many();

	public String large_bananas_estimate_in();
	public String large_bananas_how_many();

	public String large_bars_estimate_in();
	public String large_bars_how_many();

	public String large_biscuits_estimate_in();
	public String large_biscuits_how_many();

	public String large_bottles_estimate_in();
	public String large_bottles_how_many();

	public String large_bowls_estimate_in();
	public String large_bowls_how_many();

	public String large_carrots_estimate_in();
	public String large_carrots_how_many();

	public String large_cartons_estimate_in();
	public String large_cartons_how_many();

	public String large_chops_estimate_in();
	public String large_chops_how_many();

	public String large_cobs_estimate_in();
	public String large_cobs_how_many();

	public String large_crackers_estimate_in();
	public String large_crackers_how_many();

	public String large_eggs_estimate_in();
	public String large_eggs_how_many();

	public String large_fillets_estimate_in();
	public String large_fillets_how_many();

	public String large_flapjacks_estimate_in();
	public String large_flapjacks_how_many();

	public String large_gherkins_estimate_in();
	public String large_gherkins_how_many();

	public String large_handfuls_estimate_in();
	public String large_handfuls_how_many();

	public String large_hot_dogs_estimate_in();
	public String large_hot_dogs_how_many();

	public String large_jars_estimate_in();
	public String large_jars_how_many();

	public String large_kiwis_estimate_in();
	public String large_kiwis_how_many();

	public String large_mushrooms_estimate_in();
	public String large_mushrooms_how_many();

	public String large_naan_breads_estimate_in();
	public String large_naan_breads_how_many();

	public String large_omelettes_4_eggs_estimate_in();
	public String large_omelettes_4_eggs_how_many();

	public String large_onions_estimate_in();
	public String large_onions_how_many();

	public String large_pancakes_estimate_in();
	public String large_pancakes_how_many();

	public String large_parsnips_estimate_in();
	public String large_parsnips_how_many();

	public String large_pastries_estimate_in();
	public String large_pastries_how_many();

	public String large_pies_estimate_in();
	public String large_pies_how_many();

	public String large_plantains_estimate_in();
	public String large_plantains_how_many();

	public String large_portions_estimate_in();
	public String large_portions_how_many();

	public String large_pots_estimate_in();
	public String large_pots_how_many();

	public String large_scones_estimate_in();
	public String large_scones_how_many();

	public String large_share_bags_estimate_in();
	public String large_share_bags_how_many();

	public String large_skewers_estimate_in();
	public String large_skewers_how_many();

	public String large_slices_estimate_in();
	public String large_slices_how_many();

	public String large_spring_rolls_estimate_in();
	public String large_spring_rolls_how_many();

	public String large_squares_estimate_in();
	public String large_squares_how_many();

	public String large_steaks_estimate_in();
	public String large_steaks_how_many();

	public String large_tarts_estimate_in();
	public String large_tarts_how_many();

	public String large_truffles_estimate_in();
	public String large_truffles_how_many();

	public String large_tubs_estimate_in();
	public String large_tubs_how_many();

	public String leaves_estimate_in();
	public String leaves_how_many();

	public String leeks_estimate_in();
	public String leeks_how_many();

	public String level_tablespoons_estimate_in();
	public String level_tablespoons_how_many();

	public String level_teaspoons_estimate_in();
	public String level_teaspoons_how_many();

	public String lollipops_estimate_in();
	public String lollipops_how_many();

	public String macaroons_estimate_in();
	public String macaroons_how_many();

	public String mange_tout_estimate_in();
	public String mange_tout_how_many();

	public String marshmallows_estimate_in();
	public String marshmallows_how_many();

	public String meatballs_estimate_in();
	public String meatballs_how_many();

	public String medium_aubergines_estimate_in();
	public String medium_aubergines_how_many();

	public String medium_avocados_estimate_in();
	public String medium_avocados_how_many();

	public String medium_bags_estimate_in();
	public String medium_bags_how_many();

	public String medium_bananas_estimate_in();
	public String medium_bananas_how_many();

	public String medium_bars_estimate_in();
	public String medium_bars_how_many();

	public String medium_beetroot_estimate_in();
	public String medium_beetroot_how_many();

	public String medium_biscuits_estimate_in();
	public String medium_biscuits_how_many();

	public String medium_bowls_estimate_in();
	public String medium_bowls_how_many();

	public String medium_carrots_estimate_in();
	public String medium_carrots_how_many();

	public String medium_cartons_estimate_in();
	public String medium_cartons_how_many();

	public String medium_chops_estimate_in();
	public String medium_chops_how_many();

	public String medium_courgettes_estimate_in();
	public String medium_courgettes_how_many();

	public String medium_eggs_estimate_in();
	public String medium_eggs_how_many();

	public String medium_fillet_steaks_estimate_in();
	public String medium_fillet_steaks_how_many();

	public String medium_fillets_estimate_in();
	public String medium_fillets_how_many();

	public String medium_gherkins_estimate_in();
	public String medium_gherkins_how_many();

	public String medium_handfuls_estimate_in();
	public String medium_handfuls_how_many();

	public String medium_jars_estimate_in();
	public String medium_jars_how_many();

	public String medium_kiwis_estimate_in();
	public String medium_kiwis_how_many();

	public String medium_naans_estimate_in();
	public String medium_naans_how_many();

	public String medium_omelettes_2_eggs_estimate_in();
	public String medium_omelettes_2_eggs_how_many();

	public String medium_onions_estimate_in();
	public String medium_onions_how_many();

	public String medium_parsnips_estimate_in();
	public String medium_parsnips_how_many();

	public String medium_plantains_estimate_in();
	public String medium_plantains_how_many();

	public String medium_portions_estimate_in();
	public String medium_portions_how_many();

	public String medium_pots_estimate_in();
	public String medium_pots_how_many();

	public String medium_scones_estimate_in();
	public String medium_scones_how_many();

	public String medium_slices_estimate_in();
	public String medium_slices_how_many();

	public String medium_steaks_estimate_in();
	public String medium_steaks_how_many();

	public String medium_sundaes_estimate_in();
	public String medium_sundaes_how_many();

	public String medium_tubs_estimate_in();
	public String medium_tubs_how_many();

	public String meringues_estimate_in();
	public String meringues_how_many();

	public String milles_feuilles_estimate_in();
	public String milles_feuilles_how_many();

	public String mini_Oreos_estimate_in();
	public String mini_Oreos_how_many();

	public String mini_bars_estimate_in();
	public String mini_bars_how_many();

	public String mini_boxes_estimate_in();
	public String mini_boxes_how_many();

	public String mini_churros_estimate_in();
	public String mini_churros_how_many();

	public String mini_cobs_estimate_in();
	public String mini_cobs_how_many();

	public String mini_eclairs_estimate_in();
	public String mini_eclairs_how_many();

	public String mini_eggs_estimate_in();
	public String mini_eggs_how_many();

	public String mini_fillets_estimate_in();
	public String mini_fillets_how_many();

	public String mini_flapjacks_estimate_in();
	public String mini_flapjacks_how_many();

	public String mini_macaroons_estimate_in();
	public String mini_macaroons_how_many();

	public String mini_marshmallows_estimate_in();
	public String mini_marshmallows_how_many();

	public String mini_pastries_estimate_in();
	public String mini_pastries_how_many();

	public String mini_pots_estimate_in();
	public String mini_pots_how_many();

	public String mini_skewers_estimate_in();
	public String mini_skewers_how_many();

	public String mini_snack_packs_estimate_in();
	public String mini_snack_packs_how_many();

	public String mini_spring_rolls_estimate_in();
	public String mini_spring_rolls_how_many();

	public String mini_tubs_estimate_in();
	public String mini_tubs_how_many();

	public String mints_estimate_in();
	public String mints_how_many();

	public String mooncakes_estimate_in();
	public String mooncakes_how_many();

	public String mozarella_balls_estimate_in();
	public String mozarella_balls_how_many();

	public String mozzarella_sticks_estimate_in();
	public String mozzarella_sticks_how_many();

	public String mugs_estimate_in();
	public String mugs_how_many();

	public String multipack_bags_estimate_in();
	public String multipack_bags_how_many();

	public String multipack_bottles_estimate_in();
	public String multipack_bottles_how_many();

	public String mushrooms_estimate_in();
	public String mushrooms_how_many();

	public String mussels_estimate_in();
	public String mussels_how_many();

	public String nectarines_estimate_in();
	public String nectarines_how_many();

	public String new_potatoes_estimate_in();
	public String new_potatoes_how_many();

	public String nuts_estimate_in();
	public String nuts_how_many();

	public String nuts_fruits_estimate_in();
	public String nuts_fruits_how_many();

	public String olives_estimate_in();
	public String olives_how_many();

	public String onion_rings_estimate_in();
	public String onion_rings_how_many();

	public String onions_estimate_in();
	public String onions_how_many();

	public String oysters_estimate_in();
	public String oysters_how_many();

	public String packets_estimate_in();
	public String packets_how_many();

	public String packs_estimate_in();
	public String packs_how_many();

	public String pancakes_estimate_in();
	public String pancakes_how_many();

	public String panna_cottas_estimate_in();
	public String panna_cottas_how_many();

	public String pastries_estimate_in();
	public String pastries_how_many();

	public String peaches_estimate_in();
	public String peaches_how_many();

	public String peppers_estimate_in();
	public String peppers_how_many();

	public String pieces_estimate_in();
	public String pieces_how_many();

	public String pies_estimate_in();
	public String pies_how_many();

	public String pigs_in_blankets_estimate_in();
	public String pigs_in_blankets_how_many();

	public String pilchards_estimate_in();
	public String pilchards_how_many();

	public String poppadums_estimate_in();
	public String poppadums_how_many();

	public String portions_estimate_in();
	public String portions_how_many();

	public String potato_skins_estimate_in();
	public String potato_skins_how_many();

	public String potatoes_estimate_in();
	public String potatoes_how_many();

	public String pots_estimate_in();
	public String pots_how_many();

	public String pots_slices_estimate_in();
	public String pots_slices_how_many();

	public String pouches_estimate_in();
	public String pouches_how_many();

	public String prawns_estimate_in();
	public String prawns_how_many();

	public String pretzels_estimate_in();
	public String pretzels_how_many();

	public String profiteroles_estimate_in();
	public String profiteroles_how_many();

	public String prunes_estimate_in();
	public String prunes_how_many();

	public String punnets_estimate_in();
	public String punnets_how_many();

	public String rashers_estimate_in();
	public String rashers_how_many();

	public String regular_churros_estimate_in();
	public String regular_churros_how_many();

	public String ribs_estimate_in();
	public String ribs_how_many();

	public String rice_cakes_estimate_in();
	public String rice_cakes_how_many();

	public String rings_estimate_in();
	public String rings_how_many();

	public String rolls_estimate_in();
	public String rolls_how_many();

	public String sachets_estimate_in();
	public String sachets_how_many();

	public String sachets_made_up_with_milk_estimate_in();
	public String sachets_made_up_with_milk_how_many();

	public String sandwiches_made_with_two_slices_of_bread_estimate_in();
	public String sandwiches_made_with_two_slices_of_bread_how_many();

	public String sausages_estimate_in();
	public String sausages_how_many();

	public String scallops_estimate_in();
	public String scallops_how_many();

	public String scones_estimate_in();
	public String scones_how_many();

	public String scoops_of_powder_estimate_in();
	public String scoops_of_powder_how_many();

	public String segments_estimate_in();
	public String segments_how_many();

	public String share_bags_estimate_in();
	public String share_bags_how_many();

	public String sheets_estimate_in();
	public String sheets_how_many();

	public String shots_estimate_in();
	public String shots_how_many();

	public String shrimps_estimate_in();
	public String shrimps_how_many();

	public String single_measures_estimate_in();
	public String single_measures_how_many();

	public String slices_estimate_in();
	public String slices_how_many();

	public String slices_1_12th_of_cake_estimate_in();
	public String slices_1_12th_of_cake_how_many();

	public String slices_1_8th_of_cake_estimate_in();
	public String slices_1_8th_of_cake_how_many();

	public String slices_1_8th_of_pie_estimate_in();
	public String slices_1_8th_of_pie_how_many();

	public String slices_of_large_flatbread_estimate_in();
	public String slices_of_large_flatbread_how_many();

	public String small_aubergines_estimate_in();
	public String small_aubergines_how_many();

	public String small_avocados_estimate_in();
	public String small_avocados_how_many();

	public String small_bags_estimate_in();
	public String small_bags_how_many();

	public String small_bananas_estimate_in();
	public String small_bananas_how_many();

	public String small_bars_estimate_in();
	public String small_bars_how_many();

	public String small_beetroot_estimate_in();
	public String small_beetroot_how_many();

	public String small_biscuits_estimate_in();
	public String small_biscuits_how_many();

	public String small_bottles_estimate_in();
	public String small_bottles_how_many();

	public String small_bowls_estimate_in();
	public String small_bowls_how_many();

	public String small_carrots_estimate_in();
	public String small_carrots_how_many();

	public String small_cartons_estimate_in();
	public String small_cartons_how_many();

	public String small_chops_estimate_in();
	public String small_chops_how_many();

	public String small_crepes_estimate_in();
	public String small_crepes_how_many();

	public String small_eggs_estimate_in();
	public String small_eggs_how_many();

	public String small_fillets_estimate_in();
	public String small_fillets_how_many();

	public String small_flatbreads_estimate_in();
	public String small_flatbreads_how_many();

	public String small_gherkins_estimate_in();
	public String small_gherkins_how_many();

	public String small_handfuls_estimate_in();
	public String small_handfuls_how_many();

	public String small_hot_dogs_estimate_in();
	public String small_hot_dogs_how_many();

	public String small_individual_pavlovas_estimate_in();
	public String small_individual_pavlovas_how_many();

	public String small_individual_tubs_estimate_in();
	public String small_individual_tubs_how_many();

	public String small_jars_estimate_in();
	public String small_jars_how_many();

	public String small_kiwis_estimate_in();
	public String small_kiwis_how_many();

	public String small_mushrooms_estimate_in();
	public String small_mushrooms_how_many();

	public String small_naans_estimate_in();
	public String small_naans_how_many();

	public String small_omelettes_1_egg_estimate_in();
	public String small_omelettes_1_egg_how_many();

	public String small_onions_estimate_in();
	public String small_onions_how_many();

	public String small_packets_estimate_in();
	public String small_packets_how_many();

	public String small_pancakes_estimate_in();
	public String small_pancakes_how_many();

	public String small_parsnips_estimate_in();
	public String small_parsnips_how_many();

	public String small_pies_estimate_in();
	public String small_pies_how_many();

	public String small_plantains_estimate_in();
	public String small_plantains_how_many();

	public String small_portions_estimate_in();
	public String small_portions_how_many();

	public String small_pots_estimate_in();
	public String small_pots_how_many();

	public String small_scones_estimate_in();
	public String small_scones_how_many();

	public String small_slices_estimate_in();
	public String small_slices_how_many();

	public String small_souffles_estimate_in();
	public String small_souffles_how_many();

	public String small_squares_estimate_in();
	public String small_squares_how_many();

	public String small_steaks_estimate_in();
	public String small_steaks_how_many();

	public String small_sticks_estimate_in();
	public String small_sticks_how_many();

	public String small_sundaes_estimate_in();
	public String small_sundaes_how_many();

	public String small_tins_estimate_in();
	public String small_tins_how_many();

	public String small_truffles_estimate_in();
	public String small_truffles_how_many();

	public String small_tubs_estimate_in();
	public String small_tubs_how_many();

	public String spears_estimate_in();
	public String spears_how_many();

	public String sprigs_estimate_in();
	public String sprigs_how_many();

	public String sprouts_estimate_in();
	public String sprouts_how_many();

	public String squares_estimate_in();
	public String squares_how_many();

	public String standard_bags_estimate_in();
	public String standard_bags_how_many();

	public String standard_bars_estimate_in();
	public String standard_bars_how_many();

	public String standard_boxes_estimate_in();
	public String standard_boxes_how_many();

	public String standard_packs_estimate_in();
	public String standard_packs_how_many();

	public String standard_size_bottles_estimate_in();
	public String standard_size_bottles_how_many();

	public String standard_size_sticks_estimate_in();
	public String standard_size_sticks_how_many();

	public String steaks_estimate_in();
	public String steaks_how_many();

	public String sticks_estimate_in();
	public String sticks_how_many();

	public String straws_estimate_in();
	public String straws_how_many();

	public String stuffed_peppers_half_a_pepper_estimate_in();
	public String stuffed_peppers_half_a_pepper_how_many();

	public String sweets_estimate_in();
	public String sweets_how_many();

	public String tablespoons_estimate_in();
	public String tablespoons_how_many();

	public String tablets_estimate_in();
	public String tablets_how_many();

	public String tarts_estimate_in();
	public String tarts_how_many();

	public String teaspoons_estimate_in();
	public String teaspoons_how_many();

	public String thick_slices_estimate_in();
	public String thick_slices_how_many();

	public String thin_slices_estimate_in();
	public String thin_slices_how_many();

	public String tins_estimate_in();
	public String tins_how_many();

	public String tomatoes_estimate_in();
	public String tomatoes_how_many();

	public String treble_measures_estimate_in();
	public String treble_measures_how_many();

	public String triangle_slices_half_a_piece_of_bread_estimate_in();
	public String triangle_slices_half_a_piece_of_bread_how_many();

	public String tubs_estimate_in();
	public String tubs_how_many();

	public String very_thick_slices_estimate_in();
	public String very_thick_slices_how_many();

	public String vine_leaves_estimate_in();
	public String vine_leaves_how_many();

	public String vol_au_vents_estimate_in();
	public String vol_au_vents_how_many();

	public String wafers_estimate_in();
	public String wafers_how_many();

	public String waffles_estimate_in();
	public String waffles_how_many();

	public String wedges_estimate_in();
	public String wedges_how_many();

	public String whole_cakes_estimate_in();
	public String whole_cakes_how_many();

	public String whole_camemberts_estimate_in();
	public String whole_camemberts_how_many();

	public String whole_large_pies_estimate_in();
	public String whole_large_pies_how_many();

	public String whole_large_quiches_estimate_in();
	public String whole_large_quiches_how_many();

	public String whole_large_tarts_estimate_in();
	public String whole_large_tarts_how_many();

	public String whole_radishes_estimate_in();
	public String whole_radishes_how_many();

	public String whole_rolls_estimate_in();
	public String whole_rolls_how_many();

	public String whole_sausages_estimate_in();
	public String whole_sausages_how_many();

	public String whole_small_quiches_estimate_in();
	public String whole_small_quiches_how_many();

	public String whole_stuffed_peppers_estimate_in();
	public String whole_stuffed_peppers_how_many();

	public String wings_estimate_in();
	public String wings_how_many();

	public String yams_estimate_in();
	public String yams_how_many();
}
