/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat.serialisable;

import java.util.Map;

import net.scran24.user.client.survey.portionsize.experimental.PortionSize;
import net.scran24.user.client.survey.portionsize.experimental.PortionSizeScript;
import net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptManager;

import org.pcollections.client.HashTreePMap;
import org.pcollections.client.PMap;
import org.workcraft.gwt.shared.client.Option;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SerialisablePortionSize {
	@JsonProperty
	public final String scriptName;
	@JsonProperty
	public final PMap<String, String> data;
	
	@JsonCreator
	public SerialisablePortionSize(@JsonProperty("scriptName") String scriptName, @JsonProperty("data") Map<String, String> data) {
		this.scriptName = scriptName;
		this.data = HashTreePMap.from(data);
	}
	
	public SerialisablePortionSize(PortionSize portionSize) {
		this(portionSize.scriptName, portionSize.data);		
	}
	
	public PortionSize toPortionSize(PortionSizeScriptManager scriptManager) {
		return new PortionSize(scriptName, data, scriptManager.getInstance(scriptName), Option.<PortionSizeScript>none());
	}
}