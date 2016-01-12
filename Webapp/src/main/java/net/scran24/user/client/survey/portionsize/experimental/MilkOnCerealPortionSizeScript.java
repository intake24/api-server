/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.portionsize.experimental;

import static net.scran24.user.client.survey.flat.PromptUtil.withBackLink;
import static net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptUtil.done;
import static net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptUtil.guidePrompt;
import net.scran24.user.client.survey.flat.PromptUtil;
import net.scran24.user.client.survey.flat.SimplePrompt;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.FoodData;

import org.pcollections.client.HashTreePMap;
import org.pcollections.client.PMap;
import org.workcraft.gwt.imagemap.shared.ImageMapDefinition;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

public class MilkOnCerealPortionSizeScript implements PortionSizeScript {
	public static final String name = "milk-on-cereal";
	public static final String milkLevelImageMapPrefix = "milkbowl";

	public final ImageMapDefinition bowlImageMap;
	public final PMap<String, ImageMapDefinition> milkLevelImageMaps;
	private final PromptMessages messages = GWT.create(PromptMessages.class);

	/*
	 * Volume of water 
	 * 1cm 2cm 3cm 4cm 5cm 6cm 
	 * BOWL A 52.3 100.0 172.0 267.7 389.3 522.3 
	 * BOWL B 62.7 138.0 249.0 385.7 
	 * BOWL C 49.0 121.3 233.3 358.7 481.0 622.3 
	 * BOWL D 18.3 36.0 70.3 126.7 195.3 287.3 
	 * BOWL E 38.0 103.7 197.0 305.7 428.0 559.3 
	 * BOWL F 49.3 104.7 187.7 295.3 420.0 570.3
	 */

	private final PMap<String, Double[]> volumeTable = HashTreePMap.<String, Double[]> empty()
			.plus("A", new Double[] { 52.3, 100.0, 172.0, 267.7, 389.3, 522.3 })
			.plus("B", new Double[] { 62.7, 138.0, 249.0, 385.7 })
			.plus("C", new Double[] { 49.0, 121.3, 233.3, 358.7, 481.0, 622.3 })
			.plus("D", new Double[] { 18.3, 36.0, 70.3, 126.7, 195.3, 287.3 })
			.plus("E", new Double[] { 38.0, 103.7, 197.0, 305.7, 428.0, 559.3 })
			.plus("F", new Double[] { 49.3, 104.7, 187.7, 295.3, 420.0, 570.3 });
	
	/*private final PMap<String, Double> milkDensityTable = HashTreePMap.<String, Double> empty()
			.plus ("CSML", 1.034)
			.plus ("CSOY", 1.03)
			.plus ("CRIM", 1.03) // FIXME: put actual value
			.plus ("CWML", 1.031)
			.plus ("CKML", 1.036); */

	public MilkOnCerealPortionSizeScript(ImageMapDefinition bowlImageMap, PMap<String, ImageMapDefinition> milkLevelImageMaps) {
		this.bowlImageMap = bowlImageMap;
		this.milkLevelImageMaps = milkLevelImageMaps;
	}

	private double milkWeight(String bowl_id, int level) {
		return volumeTable.get(bowl_id)[level-1] * 1.032; // approximate average density for milk		
	}

	@Override
	public Option<SimplePrompt<UpdateFunc>> nextPrompt(PMap<String, String> data, final FoodData foodData) {
		if (!data.containsKey("bowl")) {
			return Option.some(PromptUtil.map(
					withBackLink(guidePrompt(
							SafeHtmlUtils.fromSafeConstant(messages.cereal_bowlPromptText()),
							bowlImageMap, "bowlIndex", "imageUrl")), new Function1<UpdateFunc, UpdateFunc>() {
						@Override
						public UpdateFunc apply(final UpdateFunc f) {
							return new UpdateFunc() {
								@Override
								public PMap<String, String> apply(PMap<String, String> argument) {
									PMap<String, String> a = f.apply(argument);
									return a.plus("bowl", CerealPortionSizeScript.bowlCodes.get(Integer.parseInt(a.get("bowlIndex")) - 1));
								}
							};
						}
					}));
		}
		if (!data.containsKey("servingWeight")) {
			String imageMapId = milkLevelImageMapPrefix + data.get("bowl");

			SimplePrompt<UpdateFunc> portionSizePrompt = PromptUtil.map(
					withBackLink(guidePrompt(SafeHtmlUtils.fromSafeConstant(messages.cereal_milkLevelPromptText()),
							milkLevelImageMaps.get(imageMapId), "milkLevelChoice", "milkLevelImage")), new Function1<UpdateFunc, UpdateFunc>() {
						@Override
						public UpdateFunc apply(final UpdateFunc f) {
							return new UpdateFunc() {
								public PMap<String, String> apply(PMap<String, String> argument) {
									PMap<String, String> a = f.apply(argument);
									return a.plus("servingWeight", Double.toString(milkWeight(a.get("bowl"), Integer.parseInt(a.get("milkLevelChoice")))))
											.plus("leftoversWeight", "0");
								}
							};
						}
					});

			return Option.some(portionSizePrompt);
		}
		return done();
	}
}