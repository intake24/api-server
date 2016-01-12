/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.shared.lookup;

import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

public  class PortionSizeMethod implements IsSerializable {
	public String name;
	public Map<String, String> params;
	public String description;
	public String imageUrl;
	public boolean useForRecipes;
	
	@Deprecated
	public PortionSizeMethod() { }
	
	public PortionSizeMethod(String name, String description, String imageUrl, boolean useForRecipes, Map<String, String> params) {
		this.name = name;
		this.description = description;
		this.imageUrl = imageUrl;
		this.useForRecipes = useForRecipes;
		this.params = params;
	}
}