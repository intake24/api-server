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

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FoodPrompt other = (FoodPrompt) obj;
		if (code == null) {
			if (other.code != null)
				return false;
		} else if (!code.equals(other.code))
			return false;
		if (genericName == null) {
			if (other.genericName != null)
				return false;
		} else if (!genericName.equals(other.genericName))
			return false;
		if (isCategoryCode != other.isCategoryCode)
			return false;
		if (linkAsMain != other.linkAsMain)
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FoodPrompt [code=" + code + ", isCategoryCode=" + isCategoryCode + ", text=" + text + ", linkAsMain=" + linkAsMain + ", genericName="
				+ genericName + "]";
	}
}
