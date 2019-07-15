package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation

import org.rogach.scallop.ScallopConf
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.foodsql.admin.{FoodIndexDataAdminImpl, FoodsAdminImpl}
import uk.ac.ncl.openlab.intake24.foodsql.foodindex.FoodIndexDataImpl
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodsAdminService
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConfigurationOptions, DatabaseConnection, ErrorHandler, WarningMessage}

object IndianFoodsImport extends App with WarningMessage with ErrorHandler with DatabaseConnection {

  private val logger = LoggerFactory.getLogger(getClass)

  val options = new ScallopConf(args) with DatabaseConfigurationOptions

  options.verify()

  val dbConfig = chooseDatabaseConfiguration(options)

  val locale = "en_IN"

  displayWarningMessage(s"This will update $locale in ${dbConfig.host}/${dbConfig.database}. Are you sure?")

  val dataSource = getDataSource(dbConfig)

  val doNotUseCodes = Set("ARAB", "BLKB", "CHPF", "INDV", "CSTS", "CPSS", "CDNT", "BCTA", "ILVC", "KKCH", "LEMO", "MNTS", "ONSS", "PARM", "DRSS",
    "SAMS", "VSPC", "ABBS", "ADUK", "BUBB", "AERM", "EIGT", "AGAV", "AIRW", "ALSD", "APOP", "ALFR", "ALBU", "ALML", "CTAM", "ALSL", "APER", "AABL",
    "APBL", "APPB", "APPC", "ACNW", "APJC", "APSE", "APPS", "APTO", "EAPL", "EAPF", "APTZ", "APRI", "RAWA", "APRJ", "APRC", "ARRR", "SMOK", "ARCT",
    "STAU", "AUBG", "AUFR", "YCFB", "YCRR", "BNSF", "BNSG", "BCGR", "BAEP", "BABU", "BNSW", "BBTL", "BTRT", "BVGR", "BAMS", "BNRL", "BANB", "BNSC",
    "BANM", "BRBT", "BARN", "BNAS", "BWSQ", "BARB", "BTNB", "BABB", "BAVS", "BEBU", "BSNC", "BEAS", "BERS", "BESA", "BEBO", "BRIS", "BBUR", "BECA", "BFCA",
    "BFDH", "BEDR", "BENH", "BEFF", "BEFG", "BGSF", "BFHP", "BFJA", "BFKO", "BFMA", "BFOL", "BFPA", "BFPS", "RIBE", "RIBG", "BFRJ", "BRSF", "BRSG", "BESW",
    "BEST", "BSSF", "BSSG", "STCN", "BFSF", "BFST", "BTBO", "TBOG", "BFWE", "BERW", "BFSM", "BBFG", "BUBN", "LFBB", "BENC", "BTSA", "BELG", "BJIC", "BERL",
    "BBNC", "BICA", "BGMA", "BMWH", "BILB", "BIYO", "CBCF", "BITT", "BLAC", "BPDB", "BPBO", "BPFR", "BLAB", "CBLA", "BCJC", "BJNS", "BLKC", "CABL", "BLAN",
    "BLON", "BLOR", "BLUC", "BLEM", "BLUE", "BLJU", "BOBO", "FRSP", "BDSW", "BDSS", "WATE", "BKCH", "BSPS", "BPIE", "BOMP", "BOBE", "SMWB", "BNTY", "BOUR",
    "BSLI", "BVRL", "BRAE", "BRAS", "BRAN", "BRSN", "BRNS", "BRBN", "BRSA", "BRPI", "BRFR", "BRGR", "BRSC", "FFTY", "BRCA", "BRID", "BRIE", "BRIO", "BRDB",
    "FSBS", "BRCR", "BUFW", "BTBN", "BUTF", "BUTT", "BNSQ", "BNQM", "BUTN", "BUXT", "FUDG", "HERO", "CCDD", "CLFM", "CTSL", "LFCS", "CALA", "CALI", "CAME",
    "FLOS", "CNSP", "CANB", "CPRS", "CASE", "CASK", "CAWM", "CPSN", "CAEG", "CALO", "CRMS", "CARW", "CARA", "CBNA", "CANC", "CROR", "CARJ", "CARS", "OCFB",
    "OCRR", "OCRO", "CARF", "CART", "CASP", "CATC", "CCYC", "CCLT", "CCSS", "CAVI", "CELC", "CCSB", "CHCM", "CHFB", "CHFW", "CHAR", "CHAD", "LFHC", "CHED",
    "HOCD", "VECH", "CHDS", "CHRO", "CAOQ", "CTSC", "CBTA", "CSRF", "CSSF", "CHSC", "CSPL", "CSLF", "CHSW", "CTRO", "CTRF", "CHTW", "HOCC", "CSTR", "CHMA",
    "CHDI", "CHEB", "CHRY", "CACH", "CHBK", "CHCE", "CRYJ", "CHRD", "CHER", "CHES", "CHWC", "CHYB", "CCBA", "CCBH", "LMFT", "CDRW", "CCJA", "CHKB", "CCKO",
    "CCMA", "CCPA", "CCRJ", "LMLG", "CDSR", "CBSW", "PAEL", "CAPP", "CHRW", "CBGF", "CHCH", "CCVB", "CCDH", "CFFO", "CHFG", "CHHO", "KIEV", "CHLA", "CKNP",
    "CPFI", "CPTA", "CSEL", "CHKE", "CRNC", "CSLC", "CSPC", "CHTH", "CTSF", "CHFF", "CHLI", "CHOL", "CRFG", "CHIV", "HOCI", "LCHC", "CHCF", "CLBB", "CHMH",
    "CSBF", "CSBC", "CCFJ", "CSBW", "CHCO", "FING", "CCPR", "CHCC", "CHCR", "CREP", "WETO", "CHMR", "CHMS", "CHRC", "CHSL", "CHTC", "CFAF", "CHOM", "CHOR",
    "CHCA", "CHRI", "CLAF", "CLAP", "CLOO", "SKIT", "CALS", "COCO", "CPCB", "COSH", "COCM", "RFCM", "CBFO", "CBFB", "CBCG", "KOLA", "COLA", "COLC", "COMP",
    "CONS", "CORS", "CCAN", "CCIS", "COQA", "COSN", "CNBN", "CNBC", "CORN", "CFCB", "CNPT", "WAFR", "CCSF", "CPRF", "COUL", "COSP", "CORB", "CORF", "CRCA",
    "RITZ", "CRAN", "CRAF", "CARP", "RACS", "CNJC", "LOCJ", "CRNB", "CRAS", "CRSQ", "CCGH", "CRCR", "WCRC", "CRDS", "CRWB", "CRHO", "CMLI", "CRSO", "CREM",
    "CEGM", "CEGG", "CRFR", "HFFR", "CCSP", "EXLC", "CRBR", "BEEF", "CHKF", "SEAW", "CRON", "CRWI", "CRMP", "CRMI", "CRWH", "CRBC", "HOCR", "CRCB", "CNCB",
    "CRGI", "CUSK", "CUPI", "CPAS", "CURC", "CURS", "CWRL", "CRNT", "CNTB", "CUPA", "CRYS", "HOCS", "CCUS", "CUUS", "CUST", "FCUS", "CURF", "DADD", "DAIM",
    "DFCU", "DFDY", "NDIC", "DFSP", "DLDU", "DAMS", "DAMC", "DAMM", "DAND", "DANS", "DBLF", "DWLO", "DAUP", "DCCA", "DCCS", "DCCW", "DLSS", "DLAS", "DLSO",
    "DCLW", "DMSS", "DCMS", "DCMW", "DCMC", "DCTE", "DFMO", "DFMB", "DIAN", "DALC", "DINB", "HODI", "DGSC", "DILL", "DISC", "DOLE", "DOLM", "DCSP", "DBCR",
    "DEKR", "DBGL", "DGHB", "DVSL", "DRPR", "DRFR", "DRAP", "APRD", "DRBA", "DRIC", "DRCR", "GOJI", "DRMA", "DRMF", "DRPI", "DRAS", "DRST", "DRFT", "DCCN",
    "DWWN", "DNOP", "DCKC", "BDEG", "DUFA", "CDPN", "DDEL", "DUND", "ECRC", "ECLE", "ECFH", "EDAM", "EDRF", "EELS", "EMSF", "EGGW", "ELDE", "ELDC", "ELSH",
    "ELML", "ELMS", "EMME", "EMPA", "ETON", "EVAP", "EVEP", "EVIA", "EXLM", "ESTM", "FABL", "FCNI", "FCIC", "WHFF", "WMFF", "FARC", "FALS", "FTCT", "FETA",
    "FIGR", "FIGS", "FILO", "FROR", "FSBD", "FSBG", "FCBD", "FCBG", "FITN", "FLAK", "FLAL", "FLJB", "FLAT", "FLCU", "CFSY", "FLWA", "FLOR", "FLUM", "FOAM",
    "FOND", "FOPO", "FRGI", "FRAZ", "FRDO", "FRCA", "RDRC", "FRFF", "LFRC", "FDLF", "FRON", "FRCH", "OJCC", "OJCB", "PLCT", "WCRF", "LIMJ", "WBFD", "FRFP",
    "FRTT", "LFFP", "LFFT", "FSHR", "FRST", "FRCB", "BBFR", "BROF", "BSFR", "CAFR", "FRCR", "FRCU", "FRMS", "RBFR", "FBNS", "FBRD", "FNBN", "FRUT", "SCHO",
    "NAKD", "DIFC", "FRCI", "FSSF", "FCNW", "FFWH", "FMSE", "FRFO", "FRFR", "FGUM", "FRBD", "FLLI", "FRLP", "FRFB", "FPIL", "FPFI", "MDAP", "FTPI", "FTPS",
    "FSRF", "FRTO", "FRWI", "LFYP", "FYLP", "FYBO", "GALA", "GANS", "GALC", "GALX", "GAPI", "GSLI", "BGRF", "BGRG", "GASA", "GARP", "HPCG", "GAZP", "GRLS",
    "GHER", "GNGR", "GNSP", "SCON", "GLAC", "GLUC", "GFBT", "GFBE", "GFBM", "GFRB", "GFBS", "GFBR", "GFBB", "GBTS", "GFCT", "GFCS", "GFBC", "GFNT", "GFCN",
    "GFCB", "GFCC", "GFCO", "GFCH", "GFCM", "GFCI", "GFCP", "GFCK", "GFCR", "GFFF", "GFFC", "GFFJ", "GFFB", "GFFP", "GFIP", "GFFS", "GFGB", "GFLM", "GFLN",
    "GFMA", "GFMC", "GFML", "GFMP", "GFMS", "GFMU", "GFOB", "GFPN", "GFPP", "GFPT", "PBGF", "GFBG", "GFPC", "GFSN", "GFRC", "GFRR", "GFSE", "GFSB", "GFVL",
    "GFWA", "GFST", "GFBW", "GOCH", "GOYO", "GOLD", "GONS", "GOLG", "GNPF", "GSYP", "GSCA", "GOOD", "GOOF", "CAGO", "GORG", "GOUD", "GSLC", "GSMI", "GSNS",
    "GRPJ", "GPFT", "GFJU", "GFRU", "GFRS", "GRAO", "GYRE", "GRHM", "GRGR", "GRKY", "GNBL", "GNBP", "GRLE", "GPBO", "PPGF", "PGRW", "PPGR", "GRGA", "GREG",
    "CHBA", "SABM", "GGSR", "STBA", "GRIS", "GRAL", "GRUY", "GUAV", "HAAG", "HBCG", "HAKE", "HBST", "HALO", "HALL", "HHOC", "HCAN", "HAMB", "HBTA", "HBRL",
    "HARI", "HARB", "HAZO", "HOCB", "HCRS", "HCRY", "HBNS", "HBRS", "HPWW", "HMEL", "HMLI", "HELM", "HMPP", "HJCB", "HOIS", "HOKG", "HOME", "HMWN", "HNCF",
    "HNUT", "CRNP", "HYPF", "HOSH", "HNCB", "HPSC", "HORM", "HOSS", "HOSK", "HOSO", "HOWM", "HORS", "DCRM", "DCSO", "DCWA", "HCSK", "DCLS", "DCLC", "HCWH",
    "HOTS", "HPSA", "FTSC", "HUMB", "FICD", "ICRM", "HOIC", "ICEG", "IBUN", "IDNT", "ILSL", "ISTP", "INNO", "IDKM", "IMSH", "INWH", "IPAP", "IRSH", "IRNB",
    "ISOT", "JTWO", "JDSA", "JACB", "JFFA", "JPRC", "JBAB", "FPST", "JELS", "JERK", "JDCB", "JUNK", "KEDG", "KEFI", "KBIS", "KCPS", "ELEV", "KETT", "KLIP",
    "KBNO", "KIND", "KPBK", "KKPB", "CSNT", "KPNS", "KPPT", "KPPR", "KRAE", "KUMQ", "KWEN", "LFCR", "LSSM", "LFSM", "LFSC", "LFSP", "LFWM", "LFYO", "SSBC",
    "LABA", "LADH", "LAJA", "LAKO", "LAMA", "LAPA", "SHTO", "LANC", "LANG", "LARD", "LARC", "LASY", "LEIC", "CURD", "LMDR", "LNRT", "LENT", "SDCL", "LILT",
    "LISB", "LIMA", "LIMC", "LNBL", "LION", "LALS", "LOGA", "LOLL", "LORN", "LCSA", "LCSP", "LCNC", "LOWC", "WWFD", "LCYC", "LCZD", "LURP", "LPKU", "MCAP",
    "MKTS", "MAKP", "MKFR", "MKTB", "MKTO", "MDCK", "MALT", "MALM", "MAWH", "MLTR", "MANC", "MTFB", "MGTF", "MGTR", "MNGO", "MNGJ", "MANF", "MPCR", "MAPP",
    "MARJ", "MRSU", "MRMT", "MARB", "MARP", "MBMI", "IMRS", "MATC", "MATO", "MATZ", "MCNS", "MCBP", "WTPD", "MFCN", "GOUM", "MBCN", "MFSR", "MFSE", "MELI",
    "MWWN", "CANT", "GMLN", "HMLN", "WATR", "MMPP", "MENT", "MWCH", "MIKD", "CTOF", "OMLK", "MBTN", "MCRS", "CSHP", "MIKA", "MSOF", "SMSH", "MSSM", "KMSH",
    "WMSH", "MWAY", "MLFE", "MSHB", "MISB", "MSTN", "MNCD", "MKIE", "MNCR", "MINM", "MNMK", "MPKP", "MNST", "MIMP", "MISO", "MSOS", "MXBE", "MIXB", "PEMX",
    "MPBO", "MPFR", "MPRW", "MPRO", "MVCB", "MOSS", "MOSK", "MOWM", "MOWC", "MOCH", "MOON", "FMOZ", "MNAS", "MGST", "MULB", "MULW", "MUCR", "MGHP", "MGBC",
    "MUBU", "MNCH", "MURM", "MSMC", "MUSP", "CMSH", "CMSC", "MPCN", "MUSS", "GMST", "NYBI", "FFNT", "LFNP", "NECT", "NCTF", "PCHS", "NEEP", "NESQ", "NPTC",
    "NPTB", "NPBS", "NIKN", "NPCN", "ENDL", "MVGR", "NUTO", "NUTB", "OASI", "OATM", "OTBX", "OTBM", "OABR", "CHBN", "OPWW", "OKCU", "ORLS", "ONIO", "ONSB",
    "ONSF", "ORPE", "ORDR", "ORAD", "ORGS", "ORNS", "FCOB", "HOOC", "OXLI", "OXTO", "OXTL", "OXSC", "OXSP", "PALE", "PALO", "PAND", "PNCE", "PANT", "PANA",
    "PANN", "PAPY", "PARH", "PSNB", "PSNR", "PASA", "PAFR", "PAPO", "PABR", "PAVL", "PHAM", "FSPM", "PARC", "PEAF", "PCHA", "PCHJ", "CPCH", "PBAC", "PRCI",
    "PEDR", "PRJC", "EPRF", "PRSJ", "PSCS", "PSFB", "PEAS", "PSPC", "PSEP", "PECO", "PCRM", "PERI", "PFTU", "PHYS", "PCLI", "PIEG", "PGHR", "PCNC", "PIGS",
    "PILB", "PILO", "PILC", "PIMS", "PINE", "PNJB", "PNAJ", "GRAP", "PINK", "PINS", "PIDA", "PINT", "PLST", "PLMF", "CAPL", "POLY", "PMBR", "PONT", "TPOP", "PPDM",
    "PBLF", "PRPA", "SPKP", "PKSC", "BSTI", "POTO", "PORW", "PRKS", "PRSS", "PRSM", "PRSO", "PRDW", "PORT", "PODS", "PTSV", "POCA", "FRTF", "FRTG", "POTH",
    "PRBH", "PWNC", "PMSW", "PRMA", "PFIL", "PRCN", "PBLD", "PFRL", "PROB", "PROS", "PRNJ", "PRNC", "PPBU", "PUBR", "PUL", "QUAI", "QUAR", "QPHC", "QPTA",
    "QUAV", "QNOP", "QUES", "QLMT", "QUIC", "QUCP", "QUOM", "QUPI", "QUOP", "QQPO", "RAGU", "RAIN", "RTRO", "RAIT", "RASP", "RASF", "PSTM", "RDSS", "RDSM",
    "RDSO", "RDBW", "RDWM", "RCRI", "RANC", "REDB", "RBSN", "REDC", "REDD", "RENS", "RGPS", "RPBO", "RPTO", "REPD", "REDS", "RDWN", "RDCU", "CARC", "LOWF",
    "RSCA", "RECU", "REGE", "REVE", "RHCU", "RHCR", "RCOA", "RHUR", "RHUB", "RIBA", "RIBN", "FLRC", "RKCB", "CTRM", "RIML", "FLRP", "RPCC", "RSAL", "RISN",
    "RICT", "HORT", "RICI", "RICC", "RICR", "GROU", "PARR", "PHEA", "PIGE", "RPMF", "RABB", "ROCK", "ROCT", "RKTL", "RKYR", "ROLO", "ROOT", "ROQE", "ROWN",
    "ROSE", "RUMB", "RBFB", "RUST", "RCRB", "RYVM", "RTMI", "RYTH", "SUSW", "SADC", "SLDC", "SACR", "SMNB", "SMNO", "SOPI", "SMNP", "SPCR", "SRTM", "SBFO",
    "SRRF", "SAVO", "BRTH", "PANC", "SPIE", "SEBA", "SEAB", "SEAP", "SEAS", "SEGG", "CTSS", "SMLK", "SERR", "SESB", "SHAN", "SHAR", "SHYO", "SHER", "SHRY",
    "SHBR", "SHWH", "SHWB", "SHRD", "SIDE", "SSPO", "SIMN", "SNCR", "CTSM", "KMLK", "SKIP", "SKIR", "SLIM", "SMRT", "SMCH", "SHBO", "SMHC", "SHPM", "SHMB",
    "SHPW", "SMST", "ISNK", "SNOA", "SNOM", "SNOW", "SODB", "SMNT", "SLRO", "SORE", "SOUR", "SOUT", "SOCH", "SOYC", "SOYM", "SOYA", "SOYP", "SPRA", "PSTT",
    "WHSP", "WMSP", "SPAM", "SWWN", "SPFL", "SKMB", "SPLT", "SPEL", "SPNB", "SPRT", "SPIR", "SBCJ", "STSP", "SPOR", "SPTD", "SPRS", "SPRO", "SCSP", "SQSA",
    "SQUF", "STAR", "STKK", "APST", "F000", "SCLZ", "STIL", "STFS", "STKW", "STNC", "STOV", "STBR", "CAST", "FROS", "STSA", "STLI", "STFJ", "STVL", "STUF",
    "SGPF", "SNPR", "SNPB", "SULB", "SUMM", "TOSD", "SPHP", "SWDB", "SWDM", "SWCD", "SWSP", "MMPI", "SPKL", "SPCP", "SWPS", "SPWE", "SWRC", "WASW", "SWEW",
    "SCCH", "SCRL", "SCFB", "SYRP", "TABA", "TABL", "TACC", "TARA", "TARR", "TARS", "TATT", "TTCK", "TERR", "TCHO", "TCCP", "THOR", "THNT", "THIS", "TIGR",
    "TBOB", "GRNT", "TFFE", "TOPS", "TMPS", "TOMB", "STRL", "FTOM", "GTOM", "TTOM", "TNIC", "STON", "TOPC", "TRAI", "TRCL", "TRCT", "TRFL", "TRLF", "TRIF",
    "TROJ", "TRST", "TUCO", "TUCS", "TUNW", "TUNM", "TUNP", "TUPB", "TQUI", "TNCW", "TBSF", "TPBG", "TROL", "TSSF", "TSSG", "TSLC", "TURD", "TNPB", "TNPM",
    "TWIG", "TWRL", "TSTR", "TYRE", "TZKI", "VANI", "VAES", "CHLQ", "VMLS", "VGMT", "CYVT", "VCCC", "VCCP", "LECU", "GOUL", "VGJU", "VEGJ", "PLVG", "VPSC",
    "VEGB", "VECP", "VEHG", "VMSS", "VEMB", "VEPA", "SAUS", "VBGF", "VELV", "VENI", "VENS", "VLFP", "VIMS", "FIVI", "VIST", "VIDR", "VINE", "SKNY", "VITA",
    "VAUV", "VOLV", "WALD", "WAKS", "WALK", "FNFR", "WKLI", "WKSB", "WAGA", "WFSW", "WBSW", "WSWT", "TWTR", "WTRB", "WTCH", "WACR", "WCRS", "WTBX", "WTBM",
    "WTAF", "WTFR", "WELS", "WENS", "WERT", "EBLY", "WCRN", "WHBX", "WHTG", "WTGR", "WHEY", "WCRA", "WHMC", "WVGR", "WASB", "WCBB", "RACA", "WBTN", "WSHP",
    "CWHB", "CWHS", "WGPS", "WWLA", "WHST", "WMIH", "WMLK", "WBRP", "BSWW", "WSPO", "CHAP", "WHOO", "WOOP", "WIMU", "WIRI", "WISP", "AERO", "WTSP", "WRCT",
    "WOTS", "WGCG", "YEAS", "YPBO", "PPYF", "PPYR", "YCFP", "YCRA", "TWCR", "TWFT", "YOIE", "YPSS", "YPDF", "ALLS", "ANCT", "ARRW", "OPSK", "SDBB", "BSLM",
    "BMUS", "BFSW", "BWNS", "BUTS", "CAJN", "CARO", "CAOS", "PSTE", "CHPS", "CCCB", "RCDK", "RCLM", "CCTM", "CHMU", "CHBR", "CKCM", "CHGR", "SCJR", "CHLL",
    "CFCN", "CFCI", "CDDC", "CSCB", "HPCC", "CIAB", "CVGR", "CINS", "CPJB", "COCR", "COMA", "COTA", "CDFP", "COES", "CLWL", "CCFL", "CRPA", "PHSS", "FFCC",
    "CRTR", "CBPS", "OATC", "CURR", "CSTL", "DBSU", "DEMS", "DCSW", "DLMD", "KBAB", "FRNT", "DHRB", "EGCT", "MUFN", "ESWM", "FENL", "FOCC", "FHRB", "FFLA",
    "PACS", "PATS", "GARM", "GNNB", "GMUS", "GURF", "HAGB", "HABO", "HALV", "RFHU", "WFRS", "JAMN", "JUNI", "KALE", "SQUB", "KUNG", "LABU", "LABB", "LHOP",
    "LATM", "LEMT", "LSOB", "LEDA", "LIPI", "LFLP", "LFCP", "MNMS", "MACE", "MARR", "MPSC", "CDMS", "DIAB", "SHTH", "MFJD", "MXKB", "MXSL", "MXSP", "MCSP",
    "HOMF", "MRCA", "MUBH", "MUSA", "FRDN", "NRST", "PART", "PSTR", "PRMI", "PEPC", "PEPS", "PLUM", "POPT", "SAUF", "PTLK", "POTD", "PTSS", "PLAM", "PPOR",
    "ROIL", "RIGA", "SVRC", "FSFC", "SEPA", "MATT", "SAJK", "SCBD", "SORS", "PSRS", "SPTP", "SCBC", "SCNF", "SAAP", "SCHS", "NAAN", "TABB", "TAHI", "THOU",
    "BRWT", "WHTT", "VMPA", "VEGT", "VEGP", "WFRB", "CRST", "YLEN", "AMWA", "BADR", "BASI", "BAYL", "PBTR", "SRLL", "CARD", "CHNB", "CAPE", "CHPR", "CGMS",
    "CHSG", "CHIL", "CHOB", "CKSC", "CCCR", "CCMU", "CHSA", "CLOV", "COIL", "LFCC", "CNCH", "CNTC", "CUMI", "FTSH", "WHPA", "WMPA", "GPIZ", "GING", "GFWT",
    "GREK", "SMOG", "GUAC", "HCSW", "HMSW", "HAWP", "HOLS", "LAKB", "LEMP", "LEMM", "LEPE", "LETH", "MYOL", "LING", "LINO", "LISE", "LYCE", "MSCR", "MINT",
    "MXNT", "MTOF", "MONC", "MOZL", "MUPO", "NVCB", "OAFL", "OLTA", "OREG", "PAPR", "PARS", "PNBC", "PBNS", "PEPP", "PONS", "PCRE", "RSTO", "POPS", "POPO",
    "POMR", "PCHM", "PORC", "POKB", "POLA", "PRAL", "PRSH", "RPOI", "SMOR", "PPRF", "PPRR", "ROOG", "SAFF", "SADR", "SAGE", "SRFP", "SAPN", "SESA", "SEDS",
    "SRFL", "SRFW", "SESO", "SHRT", "SOYS", "STAN", "STFR", "STAB", "SMUS", "SUSE", "SSCK", "SBPK", "SSCS", "SPOP", "SCCD", "THYM", "TUME", "VECA", "VLAS",
    "WRLL", "WBUN", "WMKY", "WPPR", "WPTB", "SWSS", "WSUB", "WHRL", "WHBN", "WOPA", "WHSB", "YOGD", "ASPR", "AUBB", "BAKI", "BEBE", "BPPR", "WSLC", "BREC",
    "CALF", "RCLF", "CLRY", "CGUM", "CKPS", "CHIC", "BLOC", "CILA", "CINA", "COPO", "CFFB", "CNFL", "CCLF", "EGWR", "RWEG", "EGYR", "FOGR", "LEMJ", "FFFT",
    "GARL", "GCAR", "HUSM", "FSSI", "LMMB", "LBRW", "LNTL", "OPMM", "MRNG", "OLIV", "MONK", "DGSO", "OLVE", "PALM", "FRPA", "PIZB", "CSSP", "PLFL", "POMG",
    "PORB", "PGLF", "PILI", "PUMP", "RCBR", "DUCK", "SALT", "PRIB", "SPNR", "SNFL", "TKET", "VEAL", "WRCE", "WSGR", "WHBP", "WMFL", "BBQS")


  val recode = Map("BSRC" -> "IND130", "CCSH" -> "IND129", "HASS" -> "IND128", "VGBJ" -> "IND131", "TSPC" -> "IND127", "VSPC" -> "IND126")

  val service = new FoodsAdminImpl(dataSource)

  val index = new FoodIndexDataImpl(dataSource)

  val allFoods = throwOnError(index.indexableFoods("en_GB")).map(_.code)

  val total = allFoods.size

  var processed = 1

  allFoods.foreach {
    code =>
      logger.info(s"Processing $code ($processed of $total)")

      processed += 1

      if (doNotUseCodes.contains(code)) {
        logger.info("  setting do not use")

        throwOnError(for (r <- service.getFoodRecord(code, locale);
                          _ <- service.updateLocalFoodRecord(code, r.local.toUpdate, locale)) yield ())
      } else if (recode.contains(code)) {
        logger.info("  copying local description and setting IND code")
        throwOnError(for (r <- service.getFoodRecord(code, locale);
                          _ <- service.updateLocalFoodRecord(code, r.local.toUpdate.copy(localDescription = Some(r.main.englishDescription), nutrientTableCodes = Map("INDIA" -> recode(code))), locale)) yield ())
      } else {
        logger.info("  copying local description")
        throwOnError(for (r <- service.getFoodRecord(code, locale);
                          _ <- service.updateLocalFoodRecord(code, r.local.toUpdate.copy(localDescription = Some(r.main.englishDescription)), locale)) yield ())
      }

  }

  logger.info(s"Done!")
}
