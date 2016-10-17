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

package net.scran24.user.client.survey.portionsize.experimental;

import static net.scran24.user.client.survey.flat.PromptUtil.map;

import java.util.List;

import net.scran24.user.client.survey.flat.SimplePrompt;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.client.survey.prompts.simple.AsServedPrompt;
import net.scran24.user.client.survey.prompts.simple.AsServedPromptDef;
import net.scran24.user.client.survey.prompts.simple.DrinkScalePrompt;
import net.scran24.user.client.survey.prompts.simple.DrinkScalePromptDef;
import net.scran24.user.client.survey.prompts.simple.GuidePrompt;
import net.scran24.user.client.survey.prompts.simple.OptionalFoodPrompt;
import net.scran24.user.client.survey.prompts.simple.OptionalFoodPromptDef;
import net.scran24.user.client.survey.prompts.simple.FractionalQuantityPrompt;
import net.scran24.user.client.survey.prompts.simple.StandardUnitPrompt;
import net.scran24.user.client.survey.prompts.simple.WeightTypeInPrompt;
import net.scran24.user.client.survey.prompts.simple.YesNoPrompt;
import net.scran24.user.shared.lookup.AsServedDef;
import net.scran24.user.shared.lookup.DrinkScaleDef;

import org.workcraft.gwt.imagechooser.shared.ImageDef;
import org.workcraft.gwt.imagemap.shared.ImageMapDefinition;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Panel;

public class PortionSizeScriptUtil {
	private static final PromptMessages messages = GWT.create(PromptMessages.class);

	public static final SafeHtml defaultServingSizePrompt(final String foodDescription) {
		return SafeHtmlUtils.fromSafeConstant(messages.asServed_servingPromptText(SafeHtmlUtils.htmlEscape(foodDescription.toLowerCase())));
	}

	public static final SafeHtml defaultLeftoversPrompt(final String foodDescription) {
		return SafeHtmlUtils.fromSafeConstant(messages.asServed_leftoversPromptText(SafeHtmlUtils.htmlEscape(foodDescription.toLowerCase())));
	}

	public static Option<SimplePrompt<UpdateFunc>> done() {
		return Option.none();
	}

	public static SimplePrompt<UpdateFunc> quantityPrompt(final SafeHtml promptHtml, final String confirmText, final String field) {
		return (map(new FractionalQuantityPrompt(promptHtml, confirmText), new Function1<Double, UpdateFunc>() {
			@Override
			public UpdateFunc apply(Double argument) {
				return new UpdateFunc().setField(field, Double.toString(argument));
			}
		}));
	}

	public static SimplePrompt<UpdateFunc> standardUnitChoicePrompt(final SafeHtml promptHtml, final String confirmText, List<StandardUnitDef> units,
			Function1<StandardUnitDef, String> label, final String field) {
		return (map(new StandardUnitPrompt(promptHtml, confirmText, units, label), new Function1<Integer, UpdateFunc>() {
			@Override
			public UpdateFunc apply(Integer argument) {
				return new UpdateFunc().setField(field, Integer.toString(argument));
			}
		}));
	}

	public static SimplePrompt<UpdateFunc> foodWeightPrompt(final SafeHtml promptHtml, final SafeHtml unitLabel, final String confirmText,
			final String field, final String leftoversField) {
		return (map(new WeightTypeInPrompt(promptHtml, unitLabel, confirmText), new Function1<Double, UpdateFunc>() {
			@Override
			public UpdateFunc apply(Double argument) {
				return new UpdateFunc().setField(field, Double.toString(argument)).setField(leftoversField, "0.0");
			}
		}));
	}

	public static SimplePrompt<UpdateFunc> optionalFoodPrompt(final String locale, final SafeHtml promptHtml, final String yesText,
			final String noText, final SafeHtml foodChoicePrompt, final String category, final String choiceField, final String codeField) {
		OptionalFoodPromptDef def = new OptionalFoodPromptDef(promptHtml, yesText, noText, foodChoicePrompt, category);

		return map(new OptionalFoodPrompt(locale, def), new Function1<Option<String>, UpdateFunc>() {
			@Override
			public UpdateFunc apply(Option<String> argument) {
				return argument.accept(new Option.Visitor<String, UpdateFunc>() {
					@Override
					public UpdateFunc visitSome(String item) {
						return new UpdateFunc().setField(choiceField, "true").setField(codeField, item);
					}

					@Override
					public UpdateFunc visitNone() {
						return new UpdateFunc().setField(choiceField, "false");
					}
				});
			}
		});
	}

	public static SimplePrompt<UpdateFunc> yesNoPrompt(final SafeHtml promptHtml, final String yesText, final String noText, final SafeHtml backText,
			final String field) {
		return (map(new YesNoPrompt(promptHtml, yesText, noText), new Function1<Boolean, UpdateFunc>() {
			@Override
			public UpdateFunc apply(Boolean argument) {
				return new UpdateFunc().setField(field, Boolean.toString(argument));
			}
		}));
	}

	public static SimplePrompt<UpdateFunc> yesNoPromptZeroField(final SafeHtml promptHtml, final String yesText, final String noText,
			final String field, final String zeroField) {
		return map(new YesNoPrompt(promptHtml, yesText, noText), new Function1<Boolean, UpdateFunc>() {
			@Override
			public UpdateFunc apply(Boolean argument) {
				UpdateFunc setField = new UpdateFunc().setField(field, Boolean.toString(argument));
				if (!argument)
					return setField.setField(zeroField, "0");
				else
					return setField;
			}
		});
	}

	public static SimplePrompt<UpdateFunc> asServedPrompt(final AsServedDef asServedDef, final String lessText, final String moreText,
			final String confirmText, final String indexField, final String imageUrlField, final String weightField, SafeHtml promptText) {

		final ImageDef[] defs = new ImageDef[asServedDef.images.length];

		final NumberFormat nf = NumberFormat.getDecimalFormat();

		for (int i = 0; i < asServedDef.images.length; i++) {
			defs[i] = asServedDef.images[i].def;
			defs[i].label = nf.format(Math.round(asServedDef.images[i].weight)) + " " + messages.asServed_weightUnitLabel();
		}

		AsServedPromptDef def = new AsServedPromptDef(promptText, defs, moreText, lessText, confirmText);

		return map(new AsServedPrompt(def), new Function1<Integer, UpdateFunc>() {
			@Override
			public UpdateFunc apply(Integer choice) {
				return new UpdateFunc().setField(indexField, choice.toString())
						.setField(weightField, Double.toString(asServedDef.images[choice].weight)).setField(imageUrlField, defs[choice].url);
			}
		});
	}

	public static SimplePrompt<UpdateFunc> guidePromptEx(final SafeHtml promptText, final ImageMapDefinition imageMap, final String indexField,
			final String imageUrlField, final Function1<Callback1<Integer>, Panel> additionalControlsCtor) {
		return map(new GuidePrompt(promptText, imageMap, additionalControlsCtor), new Function1<Integer, UpdateFunc>() {
			@Override
			public UpdateFunc apply(Integer argument) {
				return new UpdateFunc().setField(indexField, argument.toString()).setField(imageUrlField, imageMap.baseImageUrl);
			}
		});
	}

	public static SimplePrompt<UpdateFunc> guidePrompt(final SafeHtml promptText, final ImageMapDefinition imageMap, final String indexField,
			final String imageUrlField) {
		return map(new GuidePrompt(promptText, imageMap), new Function1<Integer, UpdateFunc>() {
			@Override
			public UpdateFunc apply(Integer argument) {
				return new UpdateFunc().setField(indexField, argument.toString()).setField(imageUrlField, imageMap.baseImageUrl);
			}
		});
	}

	public static SimplePrompt<UpdateFunc> drinkScalePrompt(final SafeHtml promptText, final DrinkScaleDef scaleDef, final String lessText,
			final String moreText, final String confirmText, final double limit, final double initialLevel, final String levelField) {

		DrinkScalePromptDef promptDef = new DrinkScalePromptDef(scaleDef, promptText, lessText, moreText, confirmText, limit, initialLevel);

		return map(new DrinkScalePrompt(promptDef), new Function1<Double, UpdateFunc>() {
			@Override
			public UpdateFunc apply(Double argument) {
				return new UpdateFunc().setField(levelField, Double.toString(argument));
			}
		});
	}
}
