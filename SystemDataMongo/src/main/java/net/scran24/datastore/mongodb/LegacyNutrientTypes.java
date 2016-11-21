package net.scran24.datastore.mongodb;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
    legacyKeyToId.put("selenium", 152l);
    legacyKeyToId.put("dietary_fiber", 17l);
    legacyKeyToId.put("total_monosac", 35l); 
    legacyKeyToId.put("organic_acids", 47l);
    legacyKeyToId.put("pufa", 52l);
    legacyKeyToId.put("nacl", 154l);
    legacyKeyToId.put("ash", 157l);
    
    idToLegacyKey = new HashMap<>();
    
    for (Entry<String,Long> e: legacyKeyToId.entrySet()) {
      idToLegacyKey.put(e.getValue(), e.getKey());
    }
  }
}
