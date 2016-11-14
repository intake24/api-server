/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.shared.lookup;

import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

public class PortionSizeMethod implements IsSerializable {
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
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PortionSizeMethod other = (PortionSizeMethod) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (imageUrl == null) {
			if (other.imageUrl != null)
				return false;
		} else if (!imageUrl.equals(other.imageUrl))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (params == null) {
			if (other.params != null)
				return false;
		} else if (!params.equals(other.params))
			return false;
		if (useForRecipes != other.useForRecipes)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PortionSizeMethod [name=" + name + ", params=" + params + ", description=" + description + ", imageUrl=" + imageUrl
				+ ", useForRecipes=" + useForRecipes + "]";
	}
}