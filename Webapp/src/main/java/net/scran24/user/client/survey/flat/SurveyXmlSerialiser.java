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

package net.scran24.user.client.survey.flat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.scran24.datastore.shared.CompletedPortionSize;
import net.scran24.datastore.shared.Time;
import net.scran24.user.client.survey.CompoundFoodTemplateManager;
import net.scran24.user.client.survey.flat.Selection.EmptySelection;
import net.scran24.user.client.survey.flat.Selection.SelectedFood;
import net.scran24.user.client.survey.flat.Selection.SelectedMeal;
import net.scran24.user.client.survey.portionsize.experimental.PortionSize;
import net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptManager;
import net.scran24.user.shared.CompoundFood;
import net.scran24.user.shared.EncodedFood;
import net.scran24.user.shared.FoodData;
import net.scran24.user.shared.FoodEntry;
import net.scran24.user.shared.FoodLink;
import net.scran24.user.shared.FoodPrompt;
import net.scran24.user.shared.Meal;
import net.scran24.user.shared.MissingFood;
import net.scran24.user.shared.MissingFoodDescription;
import net.scran24.user.shared.RawFood;
import net.scran24.user.shared.Recipe;
import net.scran24.user.shared.TemplateFood;
import net.scran24.user.shared.TemplateFoodData;
import net.scran24.user.shared.UUID;
import net.scran24.user.shared.lookup.PortionSizeMethod;

import org.pcollections.client.HashTreePMap;
import org.pcollections.client.HashTreePSet;
import org.pcollections.client.PMap;
import org.pcollections.client.PSet;
import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Either;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;

public class SurveyXmlSerialiser {

	private static final String BRAND_NAME_ATTR = "brand-name";
	private static final String BRAND_TAG = "brand";
	private static final String CALORIES_PER_100_G_ATTR = "calories-per-100-g";
	private static final String SAME_AS_BEFORE_OPTION_ATTR = "same-as-before-option";
	private static final String FOODS_TAG = "foods";
	private static final String ASK_IF_READY_MEAL_ATTR = "askIfReadyMeal";
	private static final String FLAG_TAG = "flag";
	private static final String VERSION_ID_ATTR = "version-id";
	private static final String PROMPT_LINK_AS_MAIN = "linkAsMain";
	private static final String IS_CATEGORY_CODE_ATTR = "isCategoryCode";
	private static final String LINKED_FOOD_TAG = "linked-food";
	private static final String COMPONENT_TAG = "component";
	private static final String MARKED_AS_COMPLETE_ATTR = "marked-as-complete";
	private static final String COMPOUND_FOOD_TAG = "compound-food";
	private static final String TEMPLATE_FOOD_TAG = "template-food";
	private static final String TEMPLATE_ID_ATTR = "template-id";
	private static final String LINKED_TO_ATTR = "linked-to";
	private static final String ID_ATTR = "id";
	private static final String SUPER_CATEGORIES_ATTR = "categories";
	private static final String PROMPT_TEXT_ATTR = "text";
	private static final String CATEGORY_ATTR = "category";
	private static final String IMAGE_URL_ATTR = "imageUrl";
	private static final String SCRIPT_NAME_ATTR = "scriptName";
	private static final String PORTION_SIZE_METHOD_TAG = "portion-size-method";
	private static final String SELECTED_PORTION_SIZE_METHOD = "selected-portion-size-method";
	private static final String COMPLETED_PORTION_SIZE_TAG = "completed-portion-size";
	private static final String DATA_TAG = "data";
	private static final String SEARCH_TERM_ATTR = "search-term";
	private static final String FOOD_INDEX_ATTR = "food-index";
	private static final String SELECTED_FOOD_TAG = "selected-food";
	private static final String MEAL_INDEX_ATTR = "meal-index";
	private static final String IS_AUTO_ATTR = "isAuto";
	private static final String INDEX_ATTR = "index";
	private static final String SELECTED_MEAL_TAG = "selected-meal";
	private static final String TIME_ATTR = "time";
	private static final String PORTION_SIZE_TAG = "portion-size";
	private static final String ENCODED_FOOD_TAG = "encoded-food";
	private static final String CODE_ATTR = "code";
	private static final String DESCRIPTION_ATTR = "description";
	private static final String ENGLISH_DESCRIPTION_ATTR = "english-description";
	private static final String LOCAL_DESCRIPTION_ATTR = "local-description";
	private static final String RAW_FOOD_TAG = "raw-food";
	private static final String MEAL_TAG = "meal";
	private static final String FALSE_LIT = "false";
	private static final String TRUE_LIT = "true";
	private static final String NAME_ATTR = "name";
	private static final String START_TIME_ATTR = "start-time";
	private static final String SURVEY_TAG = "survey";
	private static final String NUTRIENT_TABLE_CODE_TAG = "nutrient-table-codes";
	private static final String FOOD_PROMPT_TAG = "food-prompt";
	private static final String ENABLED_FOOD_PROMPT_TAG = "enabled-food-prompt";
	private static final String MISSING_FOOD_TAG = "missing-food";
	private static final String IS_DRINK_ATTR = "is-drink";
	private static final String MISSING_FOOD_DESCRIPTION_TAG = "missing-food-description";
	private static final String LEFTOVERS_ATTR = "leftovers";
	private static final String PORTION_SIZE_ATTR = "portion-size";
	private static final String PROMPT_GENERIC_NAME = "generic-name";
	private static final String USE_FOR_RECIPES_ATTR = "use-for-recipes";
	private static final String RECIPE_TAG = "recipe";
	private static final String RECIPES_TAG = "recipes";
	
	private static final String version_id = "548293aa-4ede-4709-8896-6de165edf4ab";
	
	private static List<Element> getChildElementsByTagName(Element parent, String tagName) {
		ArrayList<Element> result = new ArrayList<Element>();
		
		NodeList children = parent.getChildNodes();
		
		for (int i=0; i<children.getLength(); i++) {
			Node node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {				
				Element e = (Element)node;
				if (e.getTagName().equals(tagName))
					result.add(e);
			}
		}		
		return result;		
	}
	
	private static void appendFlags(Document doc, PSet<String> flags, Element e) {
		for (String s: flags) {
			final Element fe = doc.createElement(FLAG_TAG);
			fe.setAttribute(NAME_ATTR, s);
			e.appendChild(fe);
		}
	}
	
	private static PSet<String> getFlags(Element e) {
		PSet<String> flags = HashTreePSet.<String>empty();
		List<Element> flagElements = getChildElementsByTagName(e, FLAG_TAG);
		
		for (Element flagElement : flagElements) 
			flags = flags.plus(flagElement.getAttribute(NAME_ATTR));
		
		return flags;
	}
	
	public static Element foodToXml(final Document doc, final FoodEntry f) {
		Element e1 = f.accept(new FoodEntry.Visitor<Element>() {
			@Override
			public Element visitRaw(RawFood food) {
				Element e2 = doc.createElement(RAW_FOOD_TAG);
				e2.setAttribute(DESCRIPTION_ATTR, food.description);
				return e2;
			}

			@Override
			public Element visitEncoded(EncodedFood food) {
				final Element e2 = doc.createElement(ENCODED_FOOD_TAG);
				
				e2.setAttribute(LOCAL_DESCRIPTION_ATTR, food.data.localDescription);				
				e2.setAttribute(CODE_ATTR, food.data.code);
				e2.setAttribute(ASK_IF_READY_MEAL_ATTR, food.data.askIfReadyMeal ? TRUE_LIT : FALSE_LIT);
				e2.setAttribute(SAME_AS_BEFORE_OPTION_ATTR, food.data.sameAsBeforeOption ? TRUE_LIT : FALSE_LIT);
				e2.setAttribute(CALORIES_PER_100_G_ATTR, Double.toString(food.data.caloriesPer100g));
				
				/*for (String table_id : food.data.nutrientTableCodes.keySet()) {
					final Element e3 = doc.createElement(NUTRIENT_TABLE_CODE_TAG);
					e3.setAttribute(ID_ATTR, table_id);
					e3.setAttribute(CODE_ATTR, food.data.nutrientTableCodes.get(table_id));
					e2.appendChild(e3);
				}*/
				
				e2.setAttribute(SEARCH_TERM_ATTR, food.searchTerm);
				
				food.brand.accept(new Option.SideEffectVisitor<String>() {
					@Override
					public void visitSome(String item) {
						e2.setAttribute(BRAND_NAME_ATTR, item);
					}

					@Override
					public void visitNone() {
					}
				});

				StringBuilder sb = new StringBuilder();

				for (String code : food.data.categories) {
					if (!(sb.length() == 0))
						sb.append(",");
					sb.append(code);
				}

				e2.setAttribute(SUPER_CATEGORIES_ATTR, sb.toString());

				food.portionSize.accept(new Option.SideEffectVisitor<Either<PortionSize, CompletedPortionSize>>() {
					@Override
					public void visitSome(Either<PortionSize, CompletedPortionSize> item) {
						item.accept(new Either.SideEffectVisitor<PortionSize, CompletedPortionSize>() {
							@Override
							public void visitRight(CompletedPortionSize size) {
								e2.appendChild(createPortionSizeElement(doc, COMPLETED_PORTION_SIZE_TAG, size.scriptName, size.data));
							}

							@Override
							public void visitLeft(PortionSize size) {
								e2.appendChild(createPortionSizeElement(doc, PORTION_SIZE_TAG, size.scriptName, size.data));
							}
						});
					}

					@Override
					public void visitNone() {

					}
				});
				
				food.portionSizeMethodIndex.accept(new Option.SideEffectVisitor<Integer>() {
					@Override
					public void visitSome(Integer index) {
						Element psie = doc.createElement(SELECTED_PORTION_SIZE_METHOD);
						psie.setAttribute(INDEX_ATTR, Integer.toString(index));
						e2.appendChild(psie);
					}

					@Override
					public void visitNone() {
					}
				});

				for (PortionSizeMethod m : food.data.portionSizeMethods) {
					Element pse = createPortionSizeElement(doc, PORTION_SIZE_METHOD_TAG, m.name, m.params);
					pse.setAttribute(DESCRIPTION_ATTR, m.description);
					pse.setAttribute(IMAGE_URL_ATTR, m.imageUrl);
					pse.setAttribute(USE_FOR_RECIPES_ATTR, Boolean.toString(m.useForRecipes));
					e2.appendChild(pse);
				}

				for (FoodPrompt prompt : food.data.prompts) {
					Element pe = doc.createElement(FOOD_PROMPT_TAG);
					pe.setAttribute(CATEGORY_ATTR, prompt.code);
					pe.setAttribute(IS_CATEGORY_CODE_ATTR, Boolean.toString(prompt.isCategoryCode));
					pe.setAttribute(PROMPT_TEXT_ATTR, prompt.text);
					pe.setAttribute(PROMPT_LINK_AS_MAIN, Boolean.toString(prompt.linkAsMain));
					pe.setAttribute(PROMPT_GENERIC_NAME, prompt.genericName);
					e2.appendChild(pe);
				}
				
				for (String brand: food.data.brands) {
					Element be = doc.createElement(BRAND_TAG);
					be.setAttribute(NAME_ATTR, brand);
					e2.appendChild(be);
				}

				for (FoodPrompt prompt : food.enabledPrompts) {
					Element pe = doc.createElement(ENABLED_FOOD_PROMPT_TAG);
					pe.setAttribute(CATEGORY_ATTR, prompt.code);
					pe.setAttribute(IS_CATEGORY_CODE_ATTR, Boolean.toString(prompt.isCategoryCode));
					pe.setAttribute(PROMPT_TEXT_ATTR, prompt.text);
					pe.setAttribute(PROMPT_LINK_AS_MAIN, Boolean.toString(prompt.linkAsMain));
					pe.setAttribute(PROMPT_GENERIC_NAME, prompt.genericName);
					e2.appendChild(pe);
				}
				
				return e2;
			}

			@Override
			public Element visitTemplate(TemplateFood food) {
				final Element e2 = doc.createElement(TEMPLATE_FOOD_TAG);
				e2.setAttribute(TEMPLATE_ID_ATTR, food.data.template_id);
				e2.setAttribute(DESCRIPTION_ATTR, food.description);
				e2.setAttribute(IS_DRINK_ATTR, Boolean.toString(food.isDrink));

				StringBuilder sb = new StringBuilder();

				for (Integer i : food.markedAsComplete) {
					if (sb.length() > 0)
						sb.append(",");
					sb.append(i.toString());
				}
				
				e2.setAttribute(MARKED_AS_COMPLETE_ATTR, sb.toString());
				
				for (int i = 0; i < food.data.template.size(); i++) {
					final Element e3 = doc.createElement(COMPONENT_TAG);
					e3.setAttribute(INDEX_ATTR, Integer.toString(i));

					for (UUID id : food.components.get(i)) {
						final Element e4 = doc.createElement(LINKED_FOOD_TAG);
						e4.setAttribute(ID_ATTR, id.toString());
						e3.appendChild(e4);
					}

					e2.appendChild(e3);
				}

				return e2;
			}

			@Override
			public Element visitMissing(MissingFood food) {				
				final Element e2 = doc.createElement(MISSING_FOOD_TAG);
				e2.setAttribute(NAME_ATTR, food.name);
				e2.setAttribute(IS_DRINK_ATTR, Boolean.toString(food.isDrink));
								
				food.description.accept(new Option.SideEffectVisitor<MissingFoodDescription>() {
					@Override
					public void visitSome(MissingFoodDescription description) {
						final Element e3 = doc.createElement(MISSING_FOOD_DESCRIPTION_TAG);
						
						description.brand.accept(new Option.SideEffectVisitor<String>() {
							@Override
							public void visitSome(String item) {
								e3.setAttribute(BRAND_NAME_ATTR, item);								
							}
							@Override
							public void visitNone() {
								
							}
						});
						
						description.description.accept(new Option.SideEffectVisitor<String>() {
							@Override
							public void visitSome(String item) {
								e3.setAttribute(DESCRIPTION_ATTR, item);								
							}
							@Override
							public void visitNone() {
								
							}
						});
						
						description.portionSize.accept(new Option.SideEffectVisitor<String>() {
							@Override
							public void visitSome(String item) {
								e3.setAttribute(PORTION_SIZE_ATTR, item);								
							}
							@Override
							public void visitNone() {
								
							}
						});						
						
						description.leftovers.accept(new Option.SideEffectVisitor<String>() {
							@Override
							public void visitSome(String item) {
								e3.setAttribute(LEFTOVERS_ATTR, item);								
							}
							@Override
							public void visitNone() {
								
							}
						});
						
						e2.appendChild(e3);
					}

					@Override
					public void visitNone() {
					}
				});																				
				
				return e2;
				
			}

			@Override
			public Element visitCompound(CompoundFood food) {
				final Element e2 = doc.createElement(COMPOUND_FOOD_TAG);
				e2.setAttribute(DESCRIPTION_ATTR, food.description);
				e2.setAttribute(IS_DRINK_ATTR, Boolean.toString(food.isDrink));
				
				return e2;
			}
		});
		
		appendFlags(doc, f.flags, e1);
		appendData(doc, e1, f.customData);

		e1.setAttribute(ID_ATTR, f.link.id.toString());

		if (!f.link.linkedTo.isEmpty())
			e1.setAttribute(LINKED_TO_ATTR, f.link.linkedTo.getOrDie().toString());

		return e1;		
	}
	
	public static String foodsToXml(PVector<FoodEntry> foods) {
		final Document doc = XMLParser.createDocument();
		
		Element e = doc.createElement (FOODS_TAG);
		e.setAttribute(VERSION_ID_ATTR, version_id);
		
		for (FoodEntry f: foods)
			e.appendChild(foodToXml(doc, f));
		
		doc.appendChild(e);
		return doc.toString();
	}
	
	public static PVector<FoodEntry> foodsFromXml(PortionSizeScriptManager scriptManager, CompoundFoodTemplateManager templateManager, String xml) throws VersionMismatchException {
		Document doc = XMLParser.parse(xml);
		Element e = doc.getDocumentElement();
		if (!e.hasAttribute(VERSION_ID_ATTR) || !e.getAttribute(VERSION_ID_ATTR).equals(version_id))
			throw new VersionMismatchException();
		else return parseFoods(scriptManager, templateManager, e);
	}
	
	public static PVector<Recipe> recipesFromXml(PortionSizeScriptManager scriptManager, CompoundFoodTemplateManager templateManager, String xml) throws VersionMismatchException {
		Document doc = XMLParser.parse(xml);
		Element e = doc.getDocumentElement();
		
		if (!e.getTagName().equals(RECIPES_TAG) || !e.hasAttribute(VERSION_ID_ATTR) || !e.getAttribute(VERSION_ID_ATTR).equals(version_id))
			throw new VersionMismatchException();
		else {
			List<Element> recipeElements = getChildElementsByTagName(e, RECIPE_TAG);
			
			PVector<Recipe> recipes = TreePVector.empty();
			
			for (Element re: recipeElements) {
				PVector<FoodEntry> foods = parseFoods(scriptManager, templateManager, re);
				
				TemplateFood mainFood = (TemplateFood)foods.get(0);
				PVector<FoodEntry> ingredients = foods.minus(0);
				
				recipes = recipes.plus(new Recipe(mainFood, ingredients));
			}
			
			return recipes;			
		}			
	}
	
	public static Element recipeToXml(Document doc, Recipe recipe) {
		Element re = doc.createElement(RECIPE_TAG);
		
		re.appendChild(foodToXml(doc, recipe.mainFood));
		
		for (FoodEntry f: recipe.ingredients)
			re.appendChild(foodToXml(doc, f));
		
		return re;
	}
	
	public static String recipesToXml(PVector<Recipe> recipes) {
		final Document doc = XMLParser.createDocument();
		
		Element e = doc.createElement(RECIPES_TAG);
		e.setAttribute(VERSION_ID_ATTR, version_id);
		
		for (Recipe r: recipes)
			e.appendChild(recipeToXml(doc, r));
		
		doc.appendChild(e);
		
		return doc.toString();
	}
	
	public static PVector<FoodEntry> parseFoods (PortionSizeScriptManager scriptManager, CompoundFoodTemplateManager templateManager, Element parent) {
		PVector<FoodEntry> foods = TreePVector.empty();

		NodeList foodNodes = parent.getChildNodes();

		for (int j = 0; j < foodNodes.getLength(); j++) {
			Element fe = (Element) foodNodes.item(j);

			UUID id = UUID.fromString(fe.getAttribute(ID_ATTR));
			Option<UUID> linkedTo = Option.<UUID> none();

			if (fe.hasAttribute(LINKED_TO_ATTR))
				linkedTo = Option.<UUID> some(UUID.fromString(fe.getAttribute(LINKED_TO_ATTR)));

			final FoodLink link = new FoodLink(id, linkedTo);
			final PSet<String> flags = getFlags(fe);
			final PMap<String, String> customData = getData(fe);
			
			if (fe.getTagName().equals(RAW_FOOD_TAG)) {
				RawFood rf = new RawFood(link, fe.getAttribute(DESCRIPTION_ATTR), flags, customData);
				foods = foods.plus(rf);
			} else if (fe.getTagName().equals(ENCODED_FOOD_TAG)) {

				List<Element> portionSizeMethodElements = getChildElementsByTagName(fe, PORTION_SIZE_METHOD_TAG);
				List<PortionSizeMethod> portionSizeMethods = new ArrayList<PortionSizeMethod>();

				for (Element elem: portionSizeMethodElements) {
					portionSizeMethods.add(new PortionSizeMethod(elem.getAttribute(SCRIPT_NAME_ATTR), elem.getAttribute(DESCRIPTION_ATTR), elem
							.getAttribute(IMAGE_URL_ATTR), Boolean.parseBoolean(elem.getAttribute(USE_FOR_RECIPES_ATTR)), getData(elem)));
				}

				Option<Either<PortionSize, CompletedPortionSize>> portionSize;

				if (!getChildElementsByTagName(fe, PORTION_SIZE_TAG).isEmpty()) {
					Element elem = getChildElementsByTagName(fe, PORTION_SIZE_TAG).get(0);

					String name = elem.getAttribute(SCRIPT_NAME_ATTR);
					Map<String, String> data = getData(elem);

					portionSize = Option.some(PortionSize.incomplete(new PortionSize(name, HashTreePMap.<String, String> empty().plusAll(data), scriptManager
							.getInstance(name))));
				} else if (!getChildElementsByTagName(fe, COMPLETED_PORTION_SIZE_TAG).isEmpty()) {
					Element elem = getChildElementsByTagName(fe, COMPLETED_PORTION_SIZE_TAG).get(0); 

					String name = elem.getAttribute(SCRIPT_NAME_ATTR);
					Map<String, String> data = getData(elem);

					portionSize = Option.some(PortionSize.complete(new CompletedPortionSize(name, HashTreePMap.<String, String> empty().plusAll(data))));
				} else
					portionSize = Option.none();

				Option<Integer> selectedPortionSizeMethodIndex;
				
				List<Element> selectedPortionSizeMethodElements = getChildElementsByTagName(fe, SELECTED_PORTION_SIZE_METHOD);
				
				if (!selectedPortionSizeMethodElements.isEmpty())
					selectedPortionSizeMethodIndex = Option.some(Integer.parseInt(selectedPortionSizeMethodElements.get(0).getAttribute(INDEX_ATTR)));
				/*else if (portionSizeMethods.size() == 1)
					selectedPortionSizeMethodIndex = Option.some(0);*/
				else
					selectedPortionSizeMethodIndex = Option.none();

				List<Element> foodPromptElements = getChildElementsByTagName(fe, FOOD_PROMPT_TAG);
				ArrayList<FoodPrompt> prompts = new ArrayList<FoodPrompt>();

				for (Element elem: foodPromptElements) {
					prompts.add(new FoodPrompt(elem.getAttribute(CATEGORY_ATTR), elem.getAttribute(IS_CATEGORY_CODE_ATTR).equals("true"), elem.getAttribute(PROMPT_TEXT_ATTR),
							elem.getAttribute(PROMPT_LINK_AS_MAIN).equals("true"), elem.getAttribute(PROMPT_GENERIC_NAME)));
				}

				List<Element> enabledFoodPromptElements = getChildElementsByTagName(fe, ENABLED_FOOD_PROMPT_TAG);
				ArrayList<FoodPrompt> enabledPrompts = new ArrayList<FoodPrompt>();

				for (Element elem: enabledFoodPromptElements) {
					enabledPrompts.add(new FoodPrompt(elem.getAttribute(CATEGORY_ATTR), elem.getAttribute(IS_CATEGORY_CODE_ATTR).equals("true"), elem.getAttribute(PROMPT_TEXT_ATTR),
							elem.getAttribute(PROMPT_LINK_AS_MAIN).equals("true"), elem.getAttribute(PROMPT_GENERIC_NAME)));
				}
				
				List<Element> brandElements = getChildElementsByTagName(fe, BRAND_TAG);

				ArrayList<String> brands = new ArrayList<String>();

				for (Element elem: brandElements) {
					brands.add (elem.getAttribute(NAME_ATTR));
				}				

				List<String> categories = Arrays.asList(fe.getAttribute(SUPER_CATEGORIES_ATTR).split(","));
				
				Option<String> brand = Option.none();
				
				if (fe.hasAttribute(BRAND_NAME_ATTR))
					brand = Option.some(fe.getAttribute(BRAND_NAME_ATTR));
				
				/* List<Element> nutrientTableCodeElements = getChildElementsByTagName(fe, NUTRIENT_TABLE_CODE_TAG);
				
				Map<String, String> nutrientTableCodes = new HashMap<String, String>();
				
				for (Element e: nutrientTableCodeElements) 
					nutrientTableCodes.put(e.getAttribute(ID_ATTR), e.getAttribute(CODE_ATTR)); */
				
				EncodedFood ef = new EncodedFood(new FoodData(fe.getAttribute(CODE_ATTR),
						fe.getAttribute(ASK_IF_READY_MEAL_ATTR).equals(TRUE_LIT),
						fe.getAttribute(SAME_AS_BEFORE_OPTION_ATTR).equals(TRUE_LIT),
						Double.parseDouble(fe.getAttribute(CALORIES_PER_100_G_ATTR)),
						fe.getAttribute(LOCAL_DESCRIPTION_ATTR), portionSizeMethods, prompts, brands, categories), link, selectedPortionSizeMethodIndex, portionSize, brand,
						fe.getAttribute(SEARCH_TERM_ATTR), TreePVector.<FoodPrompt> empty().plusAll(enabledPrompts), flags, customData);

				foods = foods.plus(ef);
			} else if (fe.getTagName().equals(TEMPLATE_FOOD_TAG)) {
				TemplateFoodData template = templateManager.getTemplate(fe.getAttribute(TEMPLATE_ID_ATTR));

				HashSet<Integer> set = new HashSet<Integer>();

				String markedAsCompleteAttr = fe.getAttribute(MARKED_AS_COMPLETE_ATTR);
				
				String description = fe.getAttribute(DESCRIPTION_ATTR);
				
				boolean isDrink = Boolean.parseBoolean(fe.getAttribute(IS_DRINK_ATTR));

				if (!markedAsCompleteAttr.isEmpty())
					for (String s : markedAsCompleteAttr.split(","))
						set.add(Integer.parseInt(s));

				PSet<Integer> markedAsComplete = HashTreePSet.<Integer> empty().plusAll(set);

				List<Element> componentElements = getChildElementsByTagName(fe, COMPONENT_TAG);
				PMap<Integer, PSet<UUID>> components = HashTreePMap.<Integer, PSet<UUID>> empty();

				for (int k = 0; k < template.template.size(); k++)
					components = components.plus(k, HashTreePSet.<UUID> empty());

				for (Element elem: componentElements) {
					int index = Integer.parseInt(elem.getAttribute(INDEX_ATTR));

					List<Element> linkedFoodElements = getChildElementsByTagName(elem, LINKED_FOOD_TAG);

					for (Element elem2: linkedFoodElements) {
						UUID linked_id = UUID.fromString(elem2.getAttribute(ID_ATTR));
						components = components.plus(index, components.get(index).plus(linked_id));
					}
				}

				TemplateFood cf = new TemplateFood(link, description, isDrink, template, markedAsComplete, components, flags, customData);

				foods = foods.plus(cf);
			} else if (fe.getTagName().equals(MISSING_FOOD_TAG)) {
				
				String name = fe.getAttribute(NAME_ATTR);
				boolean isDrink = Boolean.parseBoolean(fe.getAttribute(IS_DRINK_ATTR));
				
				List<Element> missingFoodDescriptionElements = getChildElementsByTagName(fe, MISSING_FOOD_DESCRIPTION_TAG);
				
				if (missingFoodDescriptionElements.size() > 0) {
					Element el = missingFoodDescriptionElements.get(0);
					
					Option<String> description = Option.none();
					Option<String> brand = Option.none();
					Option<String> portionSize = Option.none();
					Option<String> leftovers = Option.none();
					
					if (el.hasAttribute(DESCRIPTION_ATTR))
						description = Option.some(el.getAttribute(DESCRIPTION_ATTR));
					
					if (el.hasAttribute(BRAND_NAME_ATTR))
						brand = Option.some(el.getAttribute(BRAND_NAME_ATTR));

					if (el.hasAttribute(PORTION_SIZE_ATTR))
						portionSize = Option.some(el.getAttribute(PORTION_SIZE_ATTR));

					if (el.hasAttribute(LEFTOVERS_ATTR))
						leftovers = Option.some(el.getAttribute(LEFTOVERS_ATTR));
						
					foods = foods.plus(new MissingFood(link, name, isDrink, Option.some(new MissingFoodDescription(brand, description, portionSize, leftovers)), flags, customData));
				} else {
					foods = foods.plus(new MissingFood(link, name, isDrink, Option.<MissingFoodDescription>none(), flags, customData));
				}				
			} else if (fe.getTagName().equals(COMPOUND_FOOD_TAG)) {
				String description = fe.getAttribute(DESCRIPTION_ATTR);
				boolean isDrink = Boolean.parseBoolean(fe.getAttribute(IS_DRINK_ATTR));
				
				foods = foods.plus(new CompoundFood(link, description, isDrink, flags, customData));
			}
		}
		return foods;
	} 
	
	public static String toXml(Survey survey) {
		final Document doc = XMLParser.createDocument();

		Element se = doc.createElement(SURVEY_TAG);
		
		appendFlags(doc, survey.flags, se);

		se.setAttribute(START_TIME_ATTR, Long.toString(survey.startTime));
		se.setAttribute(VERSION_ID_ATTR, version_id);

		for (Meal m : survey.meals) {
			final Element e = doc.createElement(MEAL_TAG);
			e.setAttribute(NAME_ATTR, m.name);
			
			appendFlags(doc, m.flags, e);
			appendData(doc, e, m.customData);

			m.time.accept(new Option.SideEffectVisitor<Time>() {
				@Override
				public void visitSome(Time item) {
					e.setAttribute(TIME_ATTR, Integer.toString(item.hours) + ":" + Integer.toString(item.minutes));
				}

				@Override
				public void visitNone() {
				}
			});

			for (FoodEntry f : m.foods)
				e.appendChild(foodToXml(doc, f));		

			se.appendChild(e);
		}

		Element sele = survey.selectedElement.accept(new Selection.Visitor<Element>() {
			@Override
			public Element visitMeal(SelectedMeal meal) {
				Element e = doc.createElement(SELECTED_MEAL_TAG);
				e.setAttribute(INDEX_ATTR, Integer.toString(meal.mealIndex));
				e.setAttribute(IS_AUTO_ATTR, (meal.selectionMode == SelectionMode.AUTO_SELECTION) ? TRUE_LIT : FALSE_LIT);
				return e;
			}

			@Override
			public Element visitFood(SelectedFood food) {
				Element e = doc.createElement(SELECTED_FOOD_TAG);
				e.setAttribute(MEAL_INDEX_ATTR, Integer.toString(food.mealIndex));
				e.setAttribute(FOOD_INDEX_ATTR, Integer.toString(food.foodIndex));
				e.setAttribute(IS_AUTO_ATTR, (food.selectionMode == SelectionMode.AUTO_SELECTION) ? TRUE_LIT : FALSE_LIT);
				return e;
			}

			@Override
			public Element visitNothing(EmptySelection selection) {
				return null;
			}
		});

		if (sele != null)
			se.appendChild(sele);
		
		appendData(doc, se, survey.customData);
		
		doc.appendChild(se);
		
		return doc.toString();
	}

	private static Element createPortionSizeElement(Document doc, String tag, String scriptName, Map<String, String> data) {
		final Element e3 = doc.createElement(tag);
		e3.setAttribute(SCRIPT_NAME_ATTR, scriptName);
		appendData(doc, e3, data);
		return e3;
	}
	
	private static void appendData (Document doc, Element elem, Map<String, String> data) {
		for (String k : data.keySet()) {
			final Element datae = doc.createElement(DATA_TAG);
			datae.setAttribute("name", k);
			datae.setAttribute("value", data.get(k));
			elem.appendChild(datae);
		}
	}

	private static PMap<String, String> getData(Element elem) {
		PMap<String, String> data = HashTreePMap.<String, String>empty();

		List<Element> dataElements = getChildElementsByTagName(elem, DATA_TAG);

		for (Element elem2: dataElements) {
			data = data.plus(elem2.getAttribute("name"), elem2.getAttribute("value"));
		}
		return data;
	}

	public static Survey fromXml(String xml, PortionSizeScriptManager scriptManager, CompoundFoodTemplateManager templateManager) throws VersionMismatchException {
		Document doc = XMLParser.parse(xml);

		final Element se = doc.getDocumentElement();
		
		if (se.getAttribute(VERSION_ID_ATTR) == null)
			throw new VersionMismatchException();
		
		if (!se.getAttribute(VERSION_ID_ATTR).equals(version_id))
			throw new VersionMismatchException();
			

		long startTime = Long.parseLong(se.getAttribute(START_TIME_ATTR));
		
		
		PVector<Meal> meals = TreePVector.empty();

		List<Element> mealElements = getChildElementsByTagName(se, MEAL_TAG);
		
		for (Element me: mealElements) {
			Option<Time> time;

			if (me.hasAttribute(TIME_ATTR)) {
				String[] t = me.getAttribute(TIME_ATTR).split(":");
				time = Option.some(new Time(Integer.parseInt(t[0]), Integer.parseInt(t[1])));
			} else {
				time = Option.none();
			}

			PVector<FoodEntry> foods = parseFoods(scriptManager, templateManager, me);		

			String name = me.getAttribute(NAME_ATTR);
			PSet<String> flags = getFlags(me);
			PMap<String, String> customData = getData(me);
			Meal meal = new Meal(name, foods, time, flags, customData);

			meals = meals.plus(meal);
		}

		List<Element> selectedMealElements = getChildElementsByTagName(se, SELECTED_MEAL_TAG);

		Selection selection;

		if (selectedMealElements.size() == 0) {
			List<Element> selectedFoodElements = getChildElementsByTagName(se, SELECTED_FOOD_TAG);

			if (selectedFoodElements.size() == 0) {
				selection = new Selection.EmptySelection(SelectionMode.AUTO_SELECTION);
			} else {
				Element selfe = selectedFoodElements.get(0);

				selection = new Selection.SelectedFood(Integer.parseInt(selfe.getAttribute(MEAL_INDEX_ATTR)), Integer.parseInt(selfe
						.getAttribute(FOOD_INDEX_ATTR)), selfe.getAttribute(IS_AUTO_ATTR).equals(TRUE_LIT) ? SelectionMode.AUTO_SELECTION
						: SelectionMode.MANUAL_SELECTION);

			}
		} else {
			Element selme = selectedMealElements.get(0);

			selection = new Selection.SelectedMeal(Integer.parseInt(selme.getAttribute(INDEX_ATTR)),
					selme.getAttribute(IS_AUTO_ATTR).equals(TRUE_LIT) ? SelectionMode.AUTO_SELECTION : SelectionMode.MANUAL_SELECTION);
		}

		return new Survey(meals, selection, startTime, getFlags(se), getData(se));
	}
}