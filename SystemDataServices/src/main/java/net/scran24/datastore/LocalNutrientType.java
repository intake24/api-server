package net.scran24.datastore;

public class LocalNutrientType {
  public final long nutrientTypeId;
  public final String localDescription;
  public final String unit;

  public LocalNutrientType(long nutrientTypeId, String localDescription, String unit) {
    this.nutrientTypeId = nutrientTypeId;
    this.localDescription = localDescription;
    this.unit = unit;
  }
}
