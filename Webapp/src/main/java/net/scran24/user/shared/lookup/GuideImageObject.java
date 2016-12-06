/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.shared.lookup;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GuideImageObject implements IsSerializable {
  public String description;
  public double weight;

  @Deprecated
  public GuideImageObject() {
  }

  public GuideImageObject(String description, double weight) {
    this.description = description;
    this.weight = weight;
  }
}
