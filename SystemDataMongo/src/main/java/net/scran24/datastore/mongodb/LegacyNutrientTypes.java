package net.scran24.datastore.mongodb;

import java.util.HashMap;
import java.util.Map;

public class LegacyNutrientTypes {
  
  public static final Map<String, Long> legacyKeyToId;
  public static final Map<Long, String> idToLegacyKey;
  
  static {
    legacyKeyToId = new HashMap<>();
    
    legacyKeyToId.put("protein", 11l);
    legacyKeyToId.put("fat", 49l);
    legacyKeyToId.put("carbohydrate", 13l);
    legacyKeyToId.put("energy_kcal", 1l);
    legacyKeyToId.put("energy_kj", 2l);
    legacyKeyToId.put("alcohol", 20l);
    legacyKeyToId.put("total_sugars", 22l);
    legacyKeyToId.put("nmes", 23l);
    legacyKeyToId.put("satd_fa", 50l);
    legacyKeyToId.put("cholesterol", 59l);
    legacyKeyToId.put("vitamin_a", 120l);
    legacyKeyToId.put("vitamin_d", 122l);
    legacyKeyToId.put("vitamin_c", 129l);
    legacyKeyToId.put("vitamin_e", 130l);
    legacyKeyToId.put("folate", 134l);
    legacyKeyToId.put("sodium", 138l);
    legacyKeyToId.put("iron", 143l);
    legacyKeyToId.put("zinc", 147l);
    v
    
    
  }
  case object Calcium extends Nutrient { val key = "calcium"; val id = 17 }
  case object Iron extends Nutrient { val key = "iron"; val id = 18 }
  case object Zinc extends Nutrient { val key = "zinc"; val id = 19 }
  case object Selenium extends Nutrient { val key = "selenium"; val id = 20 }
  case object DietaryFiber extends Nutrient { val key = "dietary_fiber"; val id = 21 }
  case object TotalMonosaccharides extends Nutrient { val key = "total_monosac"; val id = 22 }
  case object OrganicAcids extends Nutrient { val key = "organic_acids"; val id = 23 }
  case object PolyunsaturatedFattyAcids extends Nutrient { val key = "pufa"; val id = 24 }
  case object NaCl extends Nutrient { val key = "nacl"; val id = 25 }
  case object Ash extends Nutrient { val key = "ash"; val id = 26
}
