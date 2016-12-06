/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.gwt.imagemap.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class ImageMapDefinition implements IsSerializable {
  public static class Area implements IsSerializable {
    public Polygon shape;
    public String overlayUrl;
    public int id;

    /**
     * @deprecated This constructor is only for GWT serialisation support, don't
     *             use it in your code
     */
    @Deprecated
    public Area() {
    }

    public Area(Polygon shape, String overlayUrl, int id) {
      this.shape = shape;
      this.overlayUrl = overlayUrl;
      this.id = id;
    }
  }

  public String baseImageUrl;
  public Area[] areas;
  public int[][] navigation;

  /**
   * @deprecated This constructor is only for GWT serialisation support, don't
   *             use it in your code
   */
  @Deprecated
  public ImageMapDefinition() {
  }

  public ImageMapDefinition(String baseImageUrl, Area[] areas, int[][] navigation) {
    this.baseImageUrl = baseImageUrl;
    this.areas = areas;
    this.navigation = navigation;
  }
}