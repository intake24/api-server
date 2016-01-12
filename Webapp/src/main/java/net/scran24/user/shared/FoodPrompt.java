/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class FoodPrompt implements IsSerializable {
	public String code;
	public boolean isCategoryCode;
	public String text;
	public boolean linkAsMain;
	public String genericName;

	@Deprecated
	public FoodPrompt() { }
	
	public FoodPrompt(String code, boolean isCategoryCode, String text, boolean linkAsMain, String genericName) {
		this.isCategoryCode = isCategoryCode;
		this.code = code;
		this.text = text;
		this.linkAsMain = linkAsMain;
		this.genericName = genericName;
	}
}
