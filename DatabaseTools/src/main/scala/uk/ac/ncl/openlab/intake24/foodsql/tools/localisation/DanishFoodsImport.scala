package uk.ac.ncl.openlab.intake24.foodsql.tools.localisation

object DanishFoodsImport extends LocalFoodsImport("da_DK", "Danish (Denmark)", "Dansk (Danmark)", "da", "da", "dk", "DK_DTU",
    new DanishRecodingTableParser(), new DanishPsmTableParser())