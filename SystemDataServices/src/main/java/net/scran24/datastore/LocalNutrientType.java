package net.scran24.datastore;

public class LocalNutrientType {
  public final long nutrientId;
  public final String localDescription;
  public final String unit;

  public LocalNutrientType(long nutrientId, String localDescription, String unit) {
    this.nutrientId = nutrientId;
    this.localDescription = localDescription;
    this.unit = unit;
  }
}
