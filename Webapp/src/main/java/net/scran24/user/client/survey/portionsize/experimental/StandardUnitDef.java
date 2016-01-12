/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.portionsize.experimental;

public class StandardUnitDef {
	public final String name;
	public final boolean omitFoodDesc;
	public final double weight;
	
	public StandardUnitDef(String name, boolean omitFoodDesc, double weight) {
		this.name = name;
		this.omitFoodDesc = omitFoodDesc;
		this.weight = weight;
	}
}
