package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation

object PortugueseFoodsImport extends LocalFoodsImport("pt_PT", "Portuguese (Portugal)", "Português (Portugal)", "pt", "pt", "pt", "PT_INSA",
    new PortugueseRecodingTableParser(), new PortuguesePsmTableParser())
