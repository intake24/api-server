/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.shared.lookup;

import java.util.Map;

import org.workcraft.gwt.imagemap.shared.ImageMapDefinition;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GuideDef implements IsSerializable {
  public String description;
  public ImageMapDefinition imageMap;
  public Map<Integer, Double> weights;

  @Deprecated
  public GuideDef() {
  }

  public GuideDef(String description, ImageMapDefinition imageMap, Map<Integer, Double> weights) {
    this.description = description;
    this.imageMap = imageMap;
    this.weights = weights;
  }

  @Override
  public String toString() {
    return "GuideDef [description=" + description + ", imageMap=" + imageMap + ", weights=" + weights + "]";
  }

}
