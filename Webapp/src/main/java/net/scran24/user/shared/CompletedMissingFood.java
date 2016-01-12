/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class CompletedMissingFood implements IsSerializable {

	public String name;
	public String brand;
	public String description;
	public String portionSize;
	public String leftovers;
	
	@Deprecated
	public CompletedMissingFood() { }

	public CompletedMissingFood(String name, String brand, String description, String portionSize, String leftovers) {
		this.name = name;
		this.brand = brand;
		this.description = description;
		this.portionSize = portionSize;
		this.leftovers = leftovers;
	}
}
