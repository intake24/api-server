/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.shared.lookup;

import org.workcraft.gwt.imagechooser.shared.ImageDef;

import com.google.gwt.user.client.rpc.IsSerializable;

public class AsServedDef implements IsSerializable {
  public static class ImageInfo implements IsSerializable {
    public ImageDef def;

    public double weight;

    @Deprecated
    public ImageInfo() {
    }

    public ImageInfo(ImageDef def, double weight) {
      this.def = def;
      this.weight = weight;
    }
  }

  public ImageInfo[] images;

  @Deprecated
  public AsServedDef() {
  }

  public AsServedDef(ImageInfo[] images) {
    this.images = images;
  }
}
