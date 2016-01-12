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
import static net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptUtil.guidePromptEx;
import static net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptUtil.quantityPrompt;
import net.scran24.common.client.WidgetFactory;
import net.scran24.user.client.survey.flat.PromptUtil;
import net.scran24.user.client.survey.flat.SimplePrompt;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.FoodData;

import org.pcollections.client.PMap;
import org.workcraft.gwt.imagemap.shared.ImageMapDefinition;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Panel;

public class PizzaPortionSizeScript implements PortionSizeScript {
	public static final String name = "pizza";
	
	public final ImageMapDefinition pizzaTypeImageMap;
	public final ImageMapDefinition pizzaThicknessImageMap;
	public final PMap<Integer, ImageMapDefinition> sliceSizeImageMap;
	private final PromptMessages messages = GWT.create(PromptMessages.class);

	public PizzaPortionSizeScript(ImageMapDefinition pizzaTypeImageMap, ImageMapDefinition pizzaThicknessImageMap,
			PMap<Integer, ImageMapDefinition> sliceSizeImageMap) {
		this.pizzaTypeImageMap = pizzaTypeImageMap;
		this.pizzaThicknessImageMap = pizzaThicknessImageMap;
		this.sliceSizeImageMap = sliceSizeImageMap;
	}

	private final double[][] thicknessFactors = new double[][] { 
			{ 0.9, 1.0, 1.1, 1.4, 1.6 }, 
			{ 1.0, 1.2, 1.3, 1.7, 1.8 }, 
			{ 0.7, 0.8, 0.8, 1.1, 1.2 },
			{ 0.6, 0.7, 0.8, 1.0, 1.1 }, 
			{ 1.2, 1.4, 1.5, 2.0, 2.2 }, 
			{ 0.5, 0.6, 0.7, 0.9, 1.0 }, 
			{ 0.9, 1.1, 1.2, 1.5, 1.7 }, 
			{ 0.8, 0.9, 1.0, 1.3, 1.5 },
			{ 1.0, 1.2, 1.3, 1.7, 1.8 } 
			};
	
	private final double[][] sliceWeights = new double[][] {
			{ 335, 167.5, 83.8, 41.9 },
			{ 379, 189.5, 94.8, 47.4 },
			{ 390, 195.0, 97.5, 48.8 },
			{ 162, 81.0, 40.5, 20.3 },
			{ 68, 34.0, 17.0, 8.5 },
			{ 135, 67.5, 33.8 },
			{ 562, 281.0, 140.5, 70.3 },
			{ 288, 144.0, 72.0, 36.0 },
			{ 131, 65.5, 32.8, 16.4 }
	};
	
	private double sliceWeight(int pizzaType, int sliceType, int thickness) {
		// System.out.println ("Pizza type: " + pizzaType + ", slice type: " + sliceType + ", thickness: " + thickness);
		return sliceWeights[pizzaType - 1][sliceType] * thicknessFactors[pizzaType - 1][thickness - 1];
	}

	@Override
	public Option<SimplePrompt<UpdateFunc>> nextPrompt(PMap<String, String> data, final FoodData foodData) {
		if (!data.containsKey("pizzaType")) {
			return Option.some(withBackLink(guidePrompt(
					SafeHtmlUtils.fromSafeConstant(messages.pizza_typePromptText()),
					pizzaTypeImageMap, "pizzaType", "imageUrl")));
		} else if (!data.containsKey("pizzaThickness")) {
			return Option.some(
					withBackLink(guidePrompt(SafeHtmlUtils
							.fromSafeConstant(messages.pizza_thicknessPromptText()),
							pizzaThicknessImageMap, "pizzaThickness", "imageUrl")));
		}	else if (!data.containsKey("sliceType")) {
			Integer pizzaType = Integer.parseInt(data.get("pizzaType"));
			
			Function1<Callback1<Integer>, Panel> wholePizzaButton = new Function1<Callback1<Integer>, Panel>() {
				@Override
				public Panel apply(final Callback1<Integer> onComplete) {
					return WidgetFactory.createButtonsPanel(WidgetFactory.createButton(messages.pizza_sliceWholePizza(), new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {
							onComplete.call(0);							
						}
					}));
				}
			};

			SimplePrompt<UpdateFunc> portionSizePrompt = withBackLink(guidePromptEx(
					SafeHtmlUtils.fromSafeConstant(messages.pizza_sliceSizePromptText()), sliceSizeImageMap.get(pizzaType), "sliceType",
					"sliceImage", wholePizzaButton));
			return Option.some(portionSizePrompt);
		} else if (!data.containsKey("sliceQuantity")) {
			return Option.some(PromptUtil.map(withBackLink(quantityPrompt(SafeHtmlUtils.fromSafeConstant(messages.pizza_sliceQuantityPromptText()),
					messages.pizza_sliceQuantityContinueButtonLabel(), "sliceQuantity")),  new Function1<UpdateFunc, UpdateFunc>() {
				@Override
				public UpdateFunc apply(final UpdateFunc f) {
					return new UpdateFunc() {
						@Override
						public PMap<String, String> apply(PMap<String, String> argument) {
							PMap<String, String> a = f.apply(argument);
							return a.plus(
									"servingWeight",
									Double.toString(sliceWeight(Integer.parseInt(a.get("pizzaType")), Integer.parseInt(a.get("sliceType")),
											Integer.parseInt(a.get("pizzaThickness")))
											* Double.parseDouble(a.get("sliceQuantity")))).plus("leftoversWeight", Double.toString(0));
						}
					};
				}
			})); 		
		} else
			return done();
	}
}