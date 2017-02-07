






SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

SET search_path = public, pg_catalog;





INSERT INTO locales VALUES ('en_GB', 'English (United Kingdom)', 'English (United Kingdom)', 'en_GB', 'en', 'gb', NULL);
INSERT INTO locales VALUES ('da_DK', 'Danish (Denmark)', 'Dansk (Danmark)', 'da', 'da', 'dk', 'en_GB');
INSERT INTO locales VALUES ('pt_PT', 'Portuguese (Portugal)', 'Português (Portugal)', 'pt', 'pt', 'pt', 'en_GB');
INSERT INTO locales VALUES ('en_NZ', 'English (New Zealand)', 'English (New Zealand)', 'en_NZ', 'en', 'nz', 'en_GB');






INSERT INTO nutrient_units VALUES (1, 'Gram', 'g');
INSERT INTO nutrient_units VALUES (2, 'Milligram', 'mg');
INSERT INTO nutrient_units VALUES (3, 'Microgram', 'µg');
INSERT INTO nutrient_units VALUES (4, 'Kilocalorie', 'kcal');
INSERT INTO nutrient_units VALUES (5, 'Kilojoule', 'kJ');






INSERT INTO nutrient_types VALUES (1, 'Energy (kcal)', 4);
INSERT INTO nutrient_types VALUES (2, 'Energy (kJ)', 5);
INSERT INTO nutrient_types VALUES (3, 'Energy, total metabolisable (kcal, including dietary fibre)', 4);
INSERT INTO nutrient_types VALUES (4, 'Energy, total metabolisable, carbohydrate by difference, FSANZ (kcal)', 4);
INSERT INTO nutrient_types VALUES (5, 'Energy, total metabolisable, carbohydrate by difference, FSANZ (kJ)', 5);
INSERT INTO nutrient_types VALUES (6, 'Energy, total metabolisable, carbohydrate by difference, FSANZ (kcal)', 4);
INSERT INTO nutrient_types VALUES (7, 'Energy, total metabolisable, carbohydrate by difference, FSANZ (kJ)', 5);
INSERT INTO nutrient_types VALUES (8, 'Water', 1);
INSERT INTO nutrient_types VALUES (9, 'Total nitrogen', 1);
INSERT INTO nutrient_types VALUES (10, 'Nitrogen conversion factor', 1);
INSERT INTO nutrient_types VALUES (11, 'Protein', 1);
INSERT INTO nutrient_types VALUES (12, 'Protein labeling', 1);
INSERT INTO nutrient_types VALUES (13, 'Carbohydrate', 1);
INSERT INTO nutrient_types VALUES (14, 'Available carbohydrates in monosaccharide equivalent', 1);
INSERT INTO nutrient_types VALUES (15, 'Englyst fibre', 1);
INSERT INTO nutrient_types VALUES (16, 'Southgate fibre', 1);
INSERT INTO nutrient_types VALUES (17, 'Dietary fibre (g)     ', 1);
INSERT INTO nutrient_types VALUES (18, 'Fibre, water-insoluble', 1);
INSERT INTO nutrient_types VALUES (19, 'Fibre, water-soluble', 1);
INSERT INTO nutrient_types VALUES (20, 'Alcohol', 1);
INSERT INTO nutrient_types VALUES (21, 'Starch', 1);
INSERT INTO nutrient_types VALUES (22, 'Total sugars', 1);
INSERT INTO nutrient_types VALUES (23, 'Non-milk extrinsic sugars', 1);
INSERT INTO nutrient_types VALUES (24, 'Intrinsic and milk sugars', 1);
INSERT INTO nutrient_types VALUES (25, 'Glucose', 1);
INSERT INTO nutrient_types VALUES (26, 'Fructose', 1);
INSERT INTO nutrient_types VALUES (27, 'Sucrose', 1);
INSERT INTO nutrient_types VALUES (28, 'Maltose', 1);
INSERT INTO nutrient_types VALUES (29, 'Lactose', 1);
INSERT INTO nutrient_types VALUES (30, 'Other sugars (UK)', 1);
INSERT INTO nutrient_types VALUES (31, 'Other Sugars (DK)', 1);
INSERT INTO nutrient_types VALUES (32, 'Total CH expressed as monosaccarides (g)', 1);
INSERT INTO nutrient_types VALUES (33, 'Mono + disaccarides (g)', 1);
INSERT INTO nutrient_types VALUES (34, 'Galactose', 1);
INSERT INTO nutrient_types VALUES (35, 'Monosaccharider, total', 1);
INSERT INTO nutrient_types VALUES (36, 'Disaccharider, total', 1);
INSERT INTO nutrient_types VALUES (37, 'Raffinose', 1);
INSERT INTO nutrient_types VALUES (38, 'Sorbitol', 1);
INSERT INTO nutrient_types VALUES (39, 'Sugar alcohols, total', 1);
INSERT INTO nutrient_types VALUES (40, '(a)Hexoses', 1);
INSERT INTO nutrient_types VALUES (41, '(b)Pentoses', 1);
INSERT INTO nutrient_types VALUES (42, '(c)UronicAcids', 1);
INSERT INTO nutrient_types VALUES (43, 'Cellulose', 1);
INSERT INTO nutrient_types VALUES (44, 'Lignin', 1);
INSERT INTO nutrient_types VALUES (45, 'Crude fibre', 1);
INSERT INTO nutrient_types VALUES (46, 'Neutr.Det.Fibre (NDF)', 1);
INSERT INTO nutrient_types VALUES (47, 'Organic acids (g)', 1);
INSERT INTO nutrient_types VALUES (48, 'Oligosaccarides (g)', 1);
INSERT INTO nutrient_types VALUES (49, 'Fat', 1);
INSERT INTO nutrient_types VALUES (50, 'Satd FA', 1);
INSERT INTO nutrient_types VALUES (51, 'Monounsaturated fatty acids (g)', 1);
INSERT INTO nutrient_types VALUES (52, 'Polyunsaturated fatty acids (g)', 1);
INSERT INTO nutrient_types VALUES (53, 'Sum n-3 fatty acids', 1);
INSERT INTO nutrient_types VALUES (54, 'Sum n-6 fatty acids', 1);
INSERT INTO nutrient_types VALUES (55, 'Cis-Mon FA', 1);
INSERT INTO nutrient_types VALUES (56, 'Cis-n3 FA', 1);
INSERT INTO nutrient_types VALUES (57, 'Cis-n6 FA', 1);
INSERT INTO nutrient_types VALUES (58, 'Trans FA', 1);
INSERT INTO nutrient_types VALUES (59, 'Cholesterol', 1);
INSERT INTO nutrient_types VALUES (60, 'Fatty acid 18:3 omega-3', 1);
INSERT INTO nutrient_types VALUES (61, 'Fatty acid 20:4 omega-3', 1);
INSERT INTO nutrient_types VALUES (62, 'Fatty acid 20:5 omega-3', 1);
INSERT INTO nutrient_types VALUES (63, 'Fatty acid 22:5 omega-3', 1);
INSERT INTO nutrient_types VALUES (64, 'Fatty acid 22:6 omega-3', 1);
INSERT INTO nutrient_types VALUES (65, 'Fatty acid cis, trans 18:2 omega-9, 11', 1);
INSERT INTO nutrient_types VALUES (66, 'Fatty acid cis,cis 18:2 omega-6', 1);
INSERT INTO nutrient_types VALUES (67, 'Fatty acids, total long chain polyunsaturated omega-3', 1);
INSERT INTO nutrient_types VALUES (68, 'Fatty acids, total polyunsaturated omega-3', 1);
INSERT INTO nutrient_types VALUES (69, 'Fatty acids, total polyunsaturated omega-6', 1);
INSERT INTO nutrient_types VALUES (70, 'C4:0', 1);
INSERT INTO nutrient_types VALUES (71, 'C6:0', 1);
INSERT INTO nutrient_types VALUES (72, 'C8:0', 1);
INSERT INTO nutrient_types VALUES (73, 'C10:0', 1);
INSERT INTO nutrient_types VALUES (74, 'C12:0', 1);
INSERT INTO nutrient_types VALUES (75, 'C14:0', 1);
INSERT INTO nutrient_types VALUES (76, 'C15:0', 1);
INSERT INTO nutrient_types VALUES (77, 'C16:0', 1);
INSERT INTO nutrient_types VALUES (78, 'C17:0', 1);
INSERT INTO nutrient_types VALUES (79, 'C18:0', 1);
INSERT INTO nutrient_types VALUES (80, 'C20:0', 1);
INSERT INTO nutrient_types VALUES (81, 'C22:0', 1);
INSERT INTO nutrient_types VALUES (82, 'C24:0', 1);
INSERT INTO nutrient_types VALUES (83, 'Saturated F.A., other', 1);
INSERT INTO nutrient_types VALUES (84, 'C14:1,n-5', 1);
INSERT INTO nutrient_types VALUES (85, 'C15:1', 1);
INSERT INTO nutrient_types VALUES (86, 'C16:1,n-7', 1);
INSERT INTO nutrient_types VALUES (87, 'C16:1,trans', 1);
INSERT INTO nutrient_types VALUES (88, 'C17:1,n-7', 1);
INSERT INTO nutrient_types VALUES (89, 'C18:1,n-9', 1);
INSERT INTO nutrient_types VALUES (90, 'C18:1,n-7', 1);
INSERT INTO nutrient_types VALUES (91, 'C18:1,trans n-9', 1);
INSERT INTO nutrient_types VALUES (92, 'C20:1,n-9', 1);
INSERT INTO nutrient_types VALUES (93, 'C20:1,n-11', 1);
INSERT INTO nutrient_types VALUES (94, 'C20:1,trans', 1);
INSERT INTO nutrient_types VALUES (95, 'C22:1,n-9', 1);
INSERT INTO nutrient_types VALUES (96, 'C22:1,n-11', 1);
INSERT INTO nutrient_types VALUES (97, 'C22:1,trans', 1);
INSERT INTO nutrient_types VALUES (98, 'C24:1,n-9', 1);
INSERT INTO nutrient_types VALUES (99, 'Other monounsaturated.', 1);
INSERT INTO nutrient_types VALUES (100, 'C18:2,n-6', 1);
INSERT INTO nutrient_types VALUES (101, 'C18: 2 conj-A, undifferentiated', 1);
INSERT INTO nutrient_types VALUES (102, 'C18: 2, trans, undifferentiated', 1);
INSERT INTO nutrient_types VALUES (103, 'C18:3,n-3', 1);
INSERT INTO nutrient_types VALUES (104, 'C18:3,n-6', 1);
INSERT INTO nutrient_types VALUES (105, 'C18:4,n-3', 1);
INSERT INTO nutrient_types VALUES (106, 'C20:2,n-6', 1);
INSERT INTO nutrient_types VALUES (107, 'C20:4,n-3', 1);
INSERT INTO nutrient_types VALUES (108, 'C20:3,n-3', 1);
INSERT INTO nutrient_types VALUES (109, 'C20:3,n-6', 1);
INSERT INTO nutrient_types VALUES (110, 'C20:4,n-6', 1);
INSERT INTO nutrient_types VALUES (111, 'C20:5,n-3', 1);
INSERT INTO nutrient_types VALUES (112, 'C22:5,n-3', 1);
INSERT INTO nutrient_types VALUES (113, 'C22:6,n-3', 1);
INSERT INTO nutrient_types VALUES (114, 'Retinol', 1);
INSERT INTO nutrient_types VALUES (115, 'Total carotene', 1);
INSERT INTO nutrient_types VALUES (116, 'Alpha-carotene', 1);
INSERT INTO nutrient_types VALUES (117, 'Beta-carotene', 1);
INSERT INTO nutrient_types VALUES (118, 'Beta-carotene equivalents', 1);
INSERT INTO nutrient_types VALUES (119, 'Beta cryptoxanthin', 1);
INSERT INTO nutrient_types VALUES (120, 'Vitamin A', 1);
INSERT INTO nutrient_types VALUES (121, 'Vitamin A Retionol equivelents', 1);
INSERT INTO nutrient_types VALUES (122, 'Vitamin D', 1);
INSERT INTO nutrient_types VALUES (123, 'Thiamin', 1);
INSERT INTO nutrient_types VALUES (124, 'Riboflavin', 1);
INSERT INTO nutrient_types VALUES (125, 'Niacin', 1);
INSERT INTO nutrient_types VALUES (126, 'Tryptophan/60', 1);
INSERT INTO nutrient_types VALUES (127, 'Tryptophan', 1);
INSERT INTO nutrient_types VALUES (128, 'Niacin equivalent', 1);
INSERT INTO nutrient_types VALUES (129, 'Vitamin C', 1);
INSERT INTO nutrient_types VALUES (130, 'Vitamin E', 1);
INSERT INTO nutrient_types VALUES (131, 'Vitamin E, alpha-tocopherol equivalents', 1);
INSERT INTO nutrient_types VALUES (132, 'Vitamin B6', 1);
INSERT INTO nutrient_types VALUES (133, 'Vitamin B12', 1);
INSERT INTO nutrient_types VALUES (134, 'Folate', 1);
INSERT INTO nutrient_types VALUES (135, 'Folate food, naturally occurring food folates', 1);
INSERT INTO nutrient_types VALUES (136, 'Pantothenic acid', 1);
INSERT INTO nutrient_types VALUES (137, 'Biotin', 1);
INSERT INTO nutrient_types VALUES (138, 'Sodium', 1);
INSERT INTO nutrient_types VALUES (139, 'Potassium', 1);
INSERT INTO nutrient_types VALUES (140, 'Calcium', 1);
INSERT INTO nutrient_types VALUES (141, 'Magnesium', 1);
INSERT INTO nutrient_types VALUES (142, 'Phosphorus', 1);
INSERT INTO nutrient_types VALUES (143, 'Iron', 1);
INSERT INTO nutrient_types VALUES (144, 'Haem iron', 1);
INSERT INTO nutrient_types VALUES (145, 'Non-haem iron', 1);
INSERT INTO nutrient_types VALUES (146, 'Copper', 1);
INSERT INTO nutrient_types VALUES (147, 'Zinc', 1);
INSERT INTO nutrient_types VALUES (148, 'Chloride', 1);
INSERT INTO nutrient_types VALUES (149, 'Iodine', 1);
INSERT INTO nutrient_types VALUES (150, 'Iodide', 1);
INSERT INTO nutrient_types VALUES (151, 'Manganese', 1);
INSERT INTO nutrient_types VALUES (152, 'Selenium', 1);
INSERT INTO nutrient_types VALUES (153, 'Linoleic acid (g)', 1);
INSERT INTO nutrient_types VALUES (154, 'NaCl (mg)', 1);
INSERT INTO nutrient_types VALUES (155, 'a-Tocopherol (mg)', 1);
INSERT INTO nutrient_types VALUES (156, 'Beta-tocopherol', 1);
INSERT INTO nutrient_types VALUES (157, 'Ash (g)', 1);
INSERT INTO nutrient_types VALUES (158, 'Caffeine', 1);
INSERT INTO nutrient_types VALUES (159, 'Carbohydrate by difference, FSANZ', 1);
INSERT INTO nutrient_types VALUES (160, 'Delta-tocopherol', 1);
INSERT INTO nutrient_types VALUES (161, 'Gamma-tocopherol', 1);
INSERT INTO nutrient_types VALUES (162, 'Dietary folate equivalents', 1);
INSERT INTO nutrient_types VALUES (163, 'Folic acid, synthetic folic acid', 1);
INSERT INTO nutrient_types VALUES (164, 'Niacin equivalents from tryptophan', 1);
INSERT INTO nutrient_types VALUES (165, 'Total carbohydrate by difference', 1);
INSERT INTO nutrient_types VALUES (166, 'Carbohydrate, available', 1);
INSERT INTO nutrient_types VALUES (167, 'Available carbohydrate by difference', 1);
INSERT INTO nutrient_types VALUES (168, 'Available carbohydrates by weight', 1);
INSERT INTO nutrient_types VALUES (169, 'Total carbohydrates by summation', 1);
INSERT INTO nutrient_types VALUES (170, 'Carbohydrate, labeling', 1);
INSERT INTO nutrient_types VALUES (171, 'Added sugar', 1);
INSERT INTO nutrient_types VALUES (172, 'Fatty acid conversion factor (FCF)', 1);
INSERT INTO nutrient_types VALUES (173, 'Dry matter', 1);
INSERT INTO nutrient_types VALUES (174, 'D3 cholecalciferol', 1);
INSERT INTO nutrient_types VALUES (175, '25-hydroxycholecalciferol', 1);
INSERT INTO nutrient_types VALUES (176, 'alpha-tocotrienol', 1);
INSERT INTO nutrient_types VALUES (177, 'Vitamin K1', 1);
INSERT INTO nutrient_types VALUES (178, 'Vitamin B1', 1);
INSERT INTO nutrient_types VALUES (179, 'HET, hydroxyethyl thiazole', 1);
INSERT INTO nutrient_types VALUES (180, 'Free folate', 1);
INSERT INTO nutrient_types VALUES (181, 'L-ascorbic acid', 1);
INSERT INTO nutrient_types VALUES (182, 'L-dehydroascorbic', 1);
INSERT INTO nutrient_types VALUES (183, 'Chromium, Cr', 1);
INSERT INTO nutrient_types VALUES (184, 'Molybdenum, Mo', 1);
INSERT INTO nutrient_types VALUES (185, 'Cobalt, Co.', 1);
INSERT INTO nutrient_types VALUES (186, 'Nickel, Ni', 1);
INSERT INTO nutrient_types VALUES (187, 'Mercury, Hg', 1);
INSERT INTO nutrient_types VALUES (188, 'Arsenic, As', 1);
INSERT INTO nutrient_types VALUES (189, 'Cadmium, Cd', 1);
INSERT INTO nutrient_types VALUES (190, 'Lead, Pb', 1);
INSERT INTO nutrient_types VALUES (191, 'Tin, Sn', 1);
INSERT INTO nutrient_types VALUES (192, 'L-lactic acid', 1);
INSERT INTO nutrient_types VALUES (193, 'D-lactic acid', 1);
INSERT INTO nutrient_types VALUES (194, 'Lactic acid, total', 1);
INSERT INTO nutrient_types VALUES (195, 'Citric acid', 1);
INSERT INTO nutrient_types VALUES (196, 'Oxalic acid', 1);
INSERT INTO nutrient_types VALUES (197, 'Malic Acid', 1);
INSERT INTO nutrient_types VALUES (198, 'Acetic Acid', 1);
INSERT INTO nutrient_types VALUES (199, 'Benzoic acid', 1);
INSERT INTO nutrient_types VALUES (200, 'Histamine', 1);
INSERT INTO nutrient_types VALUES (201, 'Tyramine', 1);
INSERT INTO nutrient_types VALUES (202, 'Phenylethylamine', 1);
INSERT INTO nutrient_types VALUES (203, 'Putrescin', 1);
INSERT INTO nutrient_types VALUES (204, 'Cadaverine', 1);
INSERT INTO nutrient_types VALUES (205, 'Spermine', 1);
INSERT INTO nutrient_types VALUES (206, 'Spermidine', 1);
INSERT INTO nutrient_types VALUES (207, 'Serotonin', 1);
INSERT INTO nutrient_types VALUES (208, 'Other polyunsaturated', 1);
INSERT INTO nutrient_types VALUES (209, 'Other fatty acids', 1);
INSERT INTO nutrient_types VALUES (210, 'Fatty acids, total', 1);
INSERT INTO nutrient_types VALUES (211, 'Isoleucine', 1);
INSERT INTO nutrient_types VALUES (212, 'Leucine', 1);
INSERT INTO nutrient_types VALUES (213, 'Lysine', 1);
INSERT INTO nutrient_types VALUES (214, 'Methionine', 1);
INSERT INTO nutrient_types VALUES (215, 'Cystine', 1);
INSERT INTO nutrient_types VALUES (216, 'Phenylalanine', 1);
INSERT INTO nutrient_types VALUES (217, 'Tyrosine', 1);
INSERT INTO nutrient_types VALUES (218, 'Threonine', 1);
INSERT INTO nutrient_types VALUES (219, 'Valin', 1);
INSERT INTO nutrient_types VALUES (220, 'Arginine', 1);
INSERT INTO nutrient_types VALUES (221, 'Histidine', 1);
INSERT INTO nutrient_types VALUES (222, 'Alanine', 1);
INSERT INTO nutrient_types VALUES (223, 'Aspartic acid', 1);
INSERT INTO nutrient_types VALUES (224, 'Glutamic acid', 1);
INSERT INTO nutrient_types VALUES (225, 'Glycine', 1);
INSERT INTO nutrient_types VALUES (226, 'Proline', 1);
INSERT INTO nutrient_types VALUES (227, 'Serin', 1);






INSERT INTO schema_version VALUES (11);






