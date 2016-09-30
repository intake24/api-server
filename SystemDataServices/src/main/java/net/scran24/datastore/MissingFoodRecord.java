/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.datastore;

import com.google.gwt.user.client.rpc.IsSerializable;

public class MissingFoodRecord implements IsSerializable {
	
	public static final String KEY_ASSOC_FOOD_NAME = "assocFoodName";
	public static final String KEY_PROMPT_TEXT = "promptText";
	public static final String KEY_ASSOC_FOOD_CATEGORY = "assocFoodCategory";
	public static final String KEY_DESCRIPTION = "missingFoodDescription";
	public static final String KEY_PORTION_SIZE = "missingFoodPortionSize";
	public static final String KEY_LEFTOVERS = "missingFoodLeftovers";

	public long submittedAt;
	public String surveyId;
	public String userName;
	public String name;
	public String brand;
	public String description;
	public String portionSize;
	public String leftovers;
	
	@Deprecated
	public MissingFoodRecord() { }

	public MissingFoodRecord(long submittedAt, String surveyId, String userName, String name, String brand, String description, String portionSize, String leftovers) {
		this.submittedAt = submittedAt;
		this.name = name;
		this.brand = brand;
		this.description = description;
		this.portionSize = portionSize;
		this.leftovers = leftovers;
		this.surveyId = surveyId;
		this.userName = userName;
	}
}
