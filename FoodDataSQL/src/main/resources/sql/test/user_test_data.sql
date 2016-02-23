INSERT INTO locales VALUES ('test1', 'Test Locale 1', 'English (UK)', 'Test Locale 1', 'English (UK)');
INSERT INTO locales VALUES ('test2', 'Test Locale 2', 'English (UK)', 'Test Locale 2', 'English (UK)');

INSERT INTO as_served_sets VALUES ('as_served_1', 'As served 1');
INSERT INTO as_served_sets VALUES ('as_served_1_leftovers', 'As served 1 leftovers');
INSERT INTO as_served_sets VALUES ('as_served_2', 'As served 2');
INSERT INTO as_served_sets VALUES ('as_served_2_leftovers', 'As served 2 leftovers');

INSERT INTO as_served_images VALUES (1, 'as_served_1', 51.2999999999999972, 'PStApl1.jpg');
INSERT INTO as_served_images VALUES (2, 'as_served_1', 66.9599999999999937, 'PStApl2.jpg');
INSERT INTO as_served_images VALUES (3, 'as_served_1', 87.4000000000000057, 'PStApl3.jpg');
INSERT INTO as_served_images VALUES (4, 'as_served_1', 114.069999999999993, 'PStApl4.jpg');
INSERT INTO as_served_images VALUES (5, 'as_served_1', 148.889999999999986, 'PStApl5.jpg');
INSERT INTO as_served_images VALUES (6, 'as_served_1', 194.330000000000013, 'PStApl6.jpg');
INSERT INTO as_served_images VALUES (7, 'as_served_1', 253.650000000000006, 'PStApl7.jpg');
INSERT INTO as_served_images VALUES (8, 'as_served_1_leftovers', 5, 'PStApl8.jpg');
INSERT INTO as_served_images VALUES (9, 'as_served_1_leftovers', 7.37000000000000011, 'PStApl9.jpg');
INSERT INTO as_served_images VALUES (10, 'as_served_1_leftovers', 10.8599999999999994, 'PStApl10.jpg');
INSERT INTO as_served_images VALUES (11, 'as_served_1_leftovers', 16.0199999999999996, 'PStApl11.jpg');
INSERT INTO as_served_images VALUES (12, 'as_served_1_leftovers', 23.6099999999999994, 'PStApl12.jpg');
INSERT INTO as_served_images VALUES (13, 'as_served_1_leftovers', 34.7999999999999972, 'PStApl13.jpg');
INSERT INTO as_served_images VALUES (14, 'as_served_1_leftovers', 51.2999999999999972, 'PStApl14.jpg');
INSERT INTO as_served_images VALUES (15, 'as_served_2', 51.2999999999999972, 'PStApl1.jpg');
INSERT INTO as_served_images VALUES (16, 'as_served_2', 66.9599999999999937, 'PStApl2.jpg');
INSERT INTO as_served_images VALUES (17, 'as_served_2', 87.4000000000000057, 'PStApl3.jpg');
INSERT INTO as_served_images VALUES (18, 'as_served_2', 114.069999999999993, 'PStApl4.jpg');
INSERT INTO as_served_images VALUES (19, 'as_served_2', 148.889999999999986, 'PStApl5.jpg');
INSERT INTO as_served_images VALUES (20, 'as_served_2', 194.330000000000013, 'PStApl6.jpg');
INSERT INTO as_served_images VALUES (21, 'as_served_2', 253.650000000000006, 'PStApl7.jpg');
INSERT INTO as_served_images VALUES (22, 'as_served_2_leftovers', 5, 'PStApl8.jpg');
INSERT INTO as_served_images VALUES (23, 'as_served_2_leftovers', 7.37000000000000011, 'PStApl9.jpg');
INSERT INTO as_served_images VALUES (24, 'as_served_2_leftovers', 10.8599999999999994, 'PStApl10.jpg');
INSERT INTO as_served_images VALUES (25, 'as_served_2_leftovers', 16.0199999999999996, 'PStApl11.jpg');
INSERT INTO as_served_images VALUES (26, 'as_served_2_leftovers', 23.6099999999999994, 'PStApl12.jpg');
INSERT INTO as_served_images VALUES (27, 'as_served_2_leftovers', 34.7999999999999972, 'PStApl13.jpg');
INSERT INTO as_served_images VALUES (28, 'as_served_2_leftovers', 51.2999999999999972, 'PStApl14.jpg');

SELECT pg_catalog.setval('as_served_images_id_seq', 28, true);

INSERT INTO food_groups VALUES (0, 'Unassigned');
INSERT INTO food_groups VALUES (1, 'White bread/rolls');
INSERT INTO food_groups VALUES (2, 'Brown and wholemeal bread/rolls');
INSERT INTO food_groups VALUES (3, 'Sweet breads: malt bread, currant bread');
INSERT INTO food_groups VALUES (4, 'Other breads <8g/100g fat: bagel, chapatis (made without fat), milk bread, grilled papadums, pitta, rye bread, soda, flour tortillas, crumpets');
INSERT INTO food_groups VALUES (5, 'Sugar coated breakfast cereals: sugar puffs, frosties');
INSERT INTO food_groups VALUES (6, 'High sugar breakfast cereals: sugar content above 30g total sugar per 100g');
INSERT INTO food_groups VALUES (7, 'Other breakfast cereals: muesli, bran flakes');
INSERT INTO food_groups VALUES (8, 'Breakfast alternatives: nutrigrain bar, pop tart, breakfast cereal bars');
INSERT INTO food_groups VALUES (9, 'Rice');
INSERT INTO food_groups VALUES (10, 'Pasta');

INSERT INTO foods VALUES ('F000', 'Food definition test 1', 1, '5b5d7c1a-1e69-4ffd-a781-95f352c8a4e3');
INSERT INTO foods VALUES ('F001', 'Uncategorised food', 1, '8416a806-b15f-49fe-aa8c-fbab94a4faaf');
INSERT INTO foods VALUES ('F002', 'Parent test', 1, '4b9b394e-9152-45fb-a4ea-76adabecdbd6');
INSERT INTO foods VALUES ('F003', 'Inheritance test 1', 0, 'df50b3a1-2b4f-4dda-ba1f-d786e66dc8a3');
INSERT INTO foods VALUES ('F004', 'Food definition test 2', 5, 'a993cfe8-11b5-4a6a-bead-7b2beceb4b05');
INSERT INTO foods VALUES ('F005', 'Food definition test 3', 8, '5efefa47-1b2a-4257-b849-9f1c28b894de');
INSERT INTO foods VALUES ('F006', 'Inheritance test 2', 0, 'e3d6a225-a80e-4cca-8cf0-c61ab5ce0953');
INSERT INTO foods VALUES ('F007', 'Default attributes test', 0, '43a3d285-14a8-4d8b-8577-9de605b1cc09');
INSERT INTO foods VALUES ('F008', 'PSM test 1', 0, '18710555-fa6b-43c1-8b3a-898fc4e90e99');

INSERT INTO associated_food_prompts VALUES (1, 'F000', 'en_GB', 'C000', 'Prompt 1', false, 'name1');
INSERT INTO associated_food_prompts VALUES (2, 'F000', 'en_GB', 'C001', 'Prompt 2', true, 'name2');

SELECT pg_catalog.setval('associated_food_prompts_id_seq', 2, true);

INSERT INTO brands VALUES (1, 'F000', 'en_GB', 'brand1');
INSERT INTO brands VALUES (2, 'F000', 'en_GB', 'brand2');
INSERT INTO brands VALUES (3, 'F001', 'en_GB', 'brand3');
INSERT INTO brands VALUES (4, 'F001', 'en_GB', 'brand4');

SELECT pg_catalog.setval('brands_id_seq', 4, true);

INSERT INTO categories VALUES ('C000', 'Category 1', false, 'c6ea45fe-4d6b-450a-9cd0-dda378e0d609');
INSERT INTO categories VALUES ('C001', 'Category 2', false, '5b4ae898-b914-491d-bac8-b15d08357fed');
INSERT INTO categories VALUES ('C002', 'Category 3', true, 'bd5a16db-4e92-43a7-8a21-64b7d861ef59');
INSERT INTO categories VALUES ('C003', 'Category 4', false, '4557289a-e29c-4296-a5f2-558d8f3aff18');
INSERT INTO categories VALUES ('C004', 'Nested category 1', false, 'c79ecaa7-ddd2-4a3c-9f7a-391f4706b1b1');
INSERT INTO categories VALUES ('C005', 'Nested category 2', false, '305d4594-5bf8-4dc8-80b2-40d2b5d0dea6');
INSERT INTO categories VALUES ('C006', 'Nested category 3', false, '305d4594-5bf8-4dc8-80b2-40d2b5d0dea6');

INSERT INTO categories_attributes VALUES (1, 'C000', NULL, NULL, NULL);
INSERT INTO categories_attributes VALUES (2, 'C001', false, true, 3456);
INSERT INTO categories_attributes VALUES (3, 'C002', NULL, NULL, NULL);
INSERT INTO categories_attributes VALUES (4, 'C003', NULL, NULL, NULL);
INSERT INTO categories_attributes VALUES (5, 'C004', NULL, NULL, NULL);
INSERT INTO categories_attributes VALUES (6, 'C005', NULL, NULL, NULL);
INSERT INTO categories_attributes VALUES (7, 'C006', NULL, NULL, NULL);

SELECT pg_catalog.setval('categories_attributes_id_seq', 7, true);

INSERT INTO categories_categories VALUES (1, 'C005', 'C001');
INSERT INTO categories_categories VALUES (2, 'C004', 'C003');
INSERT INTO categories_categories VALUES (3, 'C005', 'C004');
INSERT INTO categories_categories VALUES (4, 'C006', 'C002');

SELECT pg_catalog.setval('categories_categories_id_seq', 4, true);

INSERT INTO categories_local VALUES ('C000', 'en_GB', 'Category 1', false, '234716b5-4ab3-4108-9ec0-e5af8e712211');
INSERT INTO categories_local VALUES ('C001', 'en_GB', 'Category 2', false, '4e9811ac-7430-4707-821b-4b2b1469f965');
INSERT INTO categories_local VALUES ('C002', 'en_GB', 'Category 3', false, 'e133a387-d4c6-4e76-aa56-fc67cddb19a8');
INSERT INTO categories_local VALUES ('C003', 'en_GB', 'Category 4', false, '7b20099d-a54d-43bb-8946-093b6fbf68ca');
INSERT INTO categories_local VALUES ('C004', 'en_GB', 'Nested category 1', false, 'e0e442f7-d19d-4c3e-a7d1-4de91e86adc3');
INSERT INTO categories_local VALUES ('C005', 'en_GB', 'Nested category 2', false, 'abfc37ce-4439-4761-84b1-a158ebb23fea');
INSERT INTO categories_local VALUES ('C006', 'en_GB', 'Nested category 3', false, 'abfc37ce-4439-4761-84b1-a158ebb23fea');

INSERT INTO categories_local VALUES ('C000', 'test1', 'Locale 1 Category 1', true, '234716b5-4ab3-4108-9ec0-e5af8e712211');
INSERT INTO categories_local VALUES ('C001', 'test1', 'Locale 1 Category 2', false, '4e9811ac-7430-4707-821b-4b2b1469f965');
INSERT INTO categories_local VALUES ('C002', 'test1', 'Locale 1 Category 3', false, 'e133a387-d4c6-4e76-aa56-fc67cddb19a8');
INSERT INTO categories_local VALUES ('C003', 'test1', 'Locale 1 Category 4', false, '7b20099d-a54d-43bb-8946-093b6fbf68ca');
INSERT INTO categories_local VALUES ('C004', 'test1', 'Locale 1 Nested category 1', false, 'e0e442f7-d19d-4c3e-a7d1-4de91e86adc3');
INSERT INTO categories_local VALUES ('C005', 'test1', 'Locale Nested category 2', false, 'abfc37ce-4439-4761-84b1-a158ebb23fea');
INSERT INTO categories_local VALUES ('C006', 'test1', 'Locale Nested category 3', false, 'abfc37ce-4439-4761-84b1-a158ebb23fea');

INSERT INTO categories_portion_size_methods VALUES (1, 'C001', 'en_GB', 'as-served', 'No description', 'portion/placeholder.jpg', false);
INSERT INTO categories_portion_size_methods VALUES (2, 'C001', 'en_GB', 'drink-scale', 'No description', 'portion/placeholder.jpg', false);

INSERT INTO categories_portion_size_method_params VALUES (1, 1, 'serving-image-set', 'as_served_2');
INSERT INTO categories_portion_size_method_params VALUES (2, 1, 'leftovers-image-set', 'as_served_2_leftovers');
INSERT INTO categories_portion_size_method_params VALUES (3, 2, 'drinkware-id', 'glasses_beer');
INSERT INTO categories_portion_size_method_params VALUES (4, 2, 'initial-fill-level', '0.9');
INSERT INTO categories_portion_size_method_params VALUES (5, 2, 'skip-fill-level', 'true');

SELECT pg_catalog.setval('categories_portion_size_method_params_id_seq', 5, true);

SELECT pg_catalog.setval('categories_portion_size_methods_id_seq', 2, true);

INSERT INTO drinkware_sets VALUES ('mugs', 'Mugs', 'Gmug');
INSERT INTO drinkware_sets VALUES ('glasses_beer', 'Glasses (beer)', 'gbeer');

INSERT INTO drinkware_scales VALUES (1, 'mugs', 470, 420, 52, 375, 1, 'mugs/mug_a_v2.jpg', 'mugs/mug_a_fill_v2.png');
INSERT INTO drinkware_scales VALUES (2, 'mugs', 470, 420, 39, 367, 2, 'mugs/mug_b_v2.jpg', 'mugs/mug_b_fill_v2.png');
INSERT INTO drinkware_scales VALUES (3, 'mugs', 470, 420, 52, 341, 3, 'mugs/mug_c_v2.jpg', 'mugs/mug_c_fill_v2.png');
INSERT INTO drinkware_scales VALUES (4, 'mugs', 470, 420, 112, 291, 4, 'mugs/mug_d_v2.jpg', 'mugs/mug_d_fill_v2.png');
INSERT INTO drinkware_scales VALUES (5, 'mugs', 470, 420, 53, 283, 5, 'mugs/mug_e_v2.jpg', 'mugs/mug_e_fill_v2.png');
INSERT INTO drinkware_scales VALUES (6, 'mugs', 470, 420, 76, 354, 6, 'mugs/mug_f_v2.jpg', 'mugs/mug_f_fill_v2.png');
INSERT INTO drinkware_scales VALUES (35, 'glasses_beer', 400, 600, 72, 467, 1, 'glasses/glass1.jpg', 'glasses/glass1_fill.png');
INSERT INTO drinkware_scales VALUES (36, 'glasses_beer', 400, 600, 71, 365, 2, 'glasses/glass2.jpg', 'glasses/glass2_fill.png');
INSERT INTO drinkware_scales VALUES (37, 'glasses_beer', 400, 600, 94, 395, 3, 'glasses/glass3.jpg', 'glasses/glass3_fill.png');
INSERT INTO drinkware_scales VALUES (38, 'glasses_beer', 400, 600, 155, 409, 7, 'glasses/glass7.jpg', 'glasses/glass7_fill.png');

SELECT pg_catalog.setval('drinkware_scales_id_seq', 38, true);

INSERT INTO drinkware_volume_samples VALUES (1, 1, 0.100000000000000006, 9);
INSERT INTO drinkware_volume_samples VALUES (2, 1, 0.200000000000000011, 42);
INSERT INTO drinkware_volume_samples VALUES (3, 1, 0.299999999999999989, 78.2000000000000028);
INSERT INTO drinkware_volume_samples VALUES (4, 1, 0.400000000000000022, 115.799999999999997);
INSERT INTO drinkware_volume_samples VALUES (5, 1, 0.5, 158);
INSERT INTO drinkware_volume_samples VALUES (6, 1, 0.599999999999999978, 196.400000000000006);
INSERT INTO drinkware_volume_samples VALUES (7, 1, 0.699999999999999956, 242.400000000000006);
INSERT INTO drinkware_volume_samples VALUES (8, 1, 0.800000000000000044, 289.800000000000011);
INSERT INTO drinkware_volume_samples VALUES (9, 1, 0.900000000000000022, 346);
INSERT INTO drinkware_volume_samples VALUES (10, 1, 1, 403.399999999999977);
INSERT INTO drinkware_volume_samples VALUES (11, 2, 0.100000000000000006, 33.2000000000000028);
INSERT INTO drinkware_volume_samples VALUES (12, 2, 0.200000000000000011, 69.4000000000000057);
INSERT INTO drinkware_volume_samples VALUES (13, 2, 0.299999999999999989, 100.400000000000006);
INSERT INTO drinkware_volume_samples VALUES (14, 2, 0.400000000000000022, 135.199999999999989);
INSERT INTO drinkware_volume_samples VALUES (15, 2, 0.5, 165.199999999999989);
INSERT INTO drinkware_volume_samples VALUES (16, 2, 0.599999999999999978, 195.800000000000011);
INSERT INTO drinkware_volume_samples VALUES (17, 2, 0.699999999999999956, 224.800000000000011);
INSERT INTO drinkware_volume_samples VALUES (18, 2, 0.800000000000000044, 255.199999999999989);
INSERT INTO drinkware_volume_samples VALUES (19, 2, 0.900000000000000022, 285.600000000000023);
INSERT INTO drinkware_volume_samples VALUES (20, 2, 1, 314.600000000000023);
INSERT INTO drinkware_volume_samples VALUES (21, 3, 0.100000000000000006, 13.4000000000000004);
INSERT INTO drinkware_volume_samples VALUES (22, 3, 0.200000000000000011, 45);
INSERT INTO drinkware_volume_samples VALUES (23, 3, 0.299999999999999989, 81.5999999999999943);
INSERT INTO drinkware_volume_samples VALUES (24, 3, 0.400000000000000022, 115);
INSERT INTO drinkware_volume_samples VALUES (25, 3, 0.5, 149.800000000000011);
INSERT INTO drinkware_volume_samples VALUES (26, 3, 0.599999999999999978, 182);
INSERT INTO drinkware_volume_samples VALUES (27, 3, 0.699999999999999956, 217.199999999999989);
INSERT INTO drinkware_volume_samples VALUES (28, 3, 0.800000000000000044, 248.599999999999994);
INSERT INTO drinkware_volume_samples VALUES (29, 3, 0.900000000000000022, 284.399999999999977);
INSERT INTO drinkware_volume_samples VALUES (30, 3, 1, 314.600000000000023);
INSERT INTO drinkware_volume_samples VALUES (31, 4, 0.100000000000000006, 0);
INSERT INTO drinkware_volume_samples VALUES (32, 4, 0.200000000000000011, 8.59999999999999964);
INSERT INTO drinkware_volume_samples VALUES (33, 4, 0.299999999999999989, 31.1999999999999993);
INSERT INTO drinkware_volume_samples VALUES (34, 4, 0.400000000000000022, 54.2000000000000028);
INSERT INTO drinkware_volume_samples VALUES (35, 4, 0.5, 81.5999999999999943);
INSERT INTO drinkware_volume_samples VALUES (36, 4, 0.599999999999999978, 107);
INSERT INTO drinkware_volume_samples VALUES (37, 4, 0.699999999999999956, 130.400000000000006);
INSERT INTO drinkware_volume_samples VALUES (38, 4, 0.800000000000000044, 152.400000000000006);
INSERT INTO drinkware_volume_samples VALUES (39, 4, 0.900000000000000022, 180.199999999999989);
INSERT INTO drinkware_volume_samples VALUES (40, 4, 1, 215.400000000000006);
INSERT INTO drinkware_volume_samples VALUES (41, 5, 0.100000000000000006, 0);
INSERT INTO drinkware_volume_samples VALUES (42, 5, 0.200000000000000011, 31.6000000000000014);
INSERT INTO drinkware_volume_samples VALUES (43, 5, 0.299999999999999989, 52.2000000000000028);
INSERT INTO drinkware_volume_samples VALUES (44, 5, 0.400000000000000022, 78.5999999999999943);
INSERT INTO drinkware_volume_samples VALUES (45, 5, 0.5, 112.599999999999994);
INSERT INTO drinkware_volume_samples VALUES (46, 5, 0.599999999999999978, 137.599999999999994);
INSERT INTO drinkware_volume_samples VALUES (47, 5, 0.699999999999999956, 168);
INSERT INTO drinkware_volume_samples VALUES (48, 5, 0.800000000000000044, 196.800000000000011);
INSERT INTO drinkware_volume_samples VALUES (49, 5, 0.900000000000000022, 225.800000000000011);
INSERT INTO drinkware_volume_samples VALUES (50, 5, 1, 253);
INSERT INTO drinkware_volume_samples VALUES (51, 6, 0.100000000000000006, 1);
INSERT INTO drinkware_volume_samples VALUES (52, 6, 0.200000000000000011, 20.6000000000000014);
INSERT INTO drinkware_volume_samples VALUES (53, 6, 0.299999999999999989, 58.6000000000000014);
INSERT INTO drinkware_volume_samples VALUES (54, 6, 0.400000000000000022, 103.400000000000006);
INSERT INTO drinkware_volume_samples VALUES (55, 6, 0.5, 148.199999999999989);
INSERT INTO drinkware_volume_samples VALUES (56, 6, 0.599999999999999978, 202);
INSERT INTO drinkware_volume_samples VALUES (57, 6, 0.699999999999999956, 249.199999999999989);
INSERT INTO drinkware_volume_samples VALUES (58, 6, 0.800000000000000044, 301);
INSERT INTO drinkware_volume_samples VALUES (59, 6, 0.900000000000000022, 348.800000000000011);
INSERT INTO drinkware_volume_samples VALUES (60, 6, 1, 399.800000000000011);
INSERT INTO drinkware_volume_samples VALUES (384, 36, 0.0819672131147541061, 16);
INSERT INTO drinkware_volume_samples VALUES (385, 36, 0.163934426229508212, 35.2999999999999972);
INSERT INTO drinkware_volume_samples VALUES (386, 36, 0.245901639344262318, 52.7000000000000028);
INSERT INTO drinkware_volume_samples VALUES (387, 36, 0.327868852459016424, 73);
INSERT INTO drinkware_volume_samples VALUES (388, 36, 0.409836065573770503, 92.2999999999999972);
INSERT INTO drinkware_volume_samples VALUES (389, 36, 0.491803278688524637, 117.700000000000003);
INSERT INTO drinkware_volume_samples VALUES (390, 36, 0.57377049180327877, 146.699999999999989);
INSERT INTO drinkware_volume_samples VALUES (391, 36, 0.655737704918032849, 177.699999999999989);
INSERT INTO drinkware_volume_samples VALUES (392, 36, 0.737704918032786927, 215.300000000000011);
INSERT INTO drinkware_volume_samples VALUES (393, 36, 0.819672131147541005, 254);
INSERT INTO drinkware_volume_samples VALUES (394, 36, 0.901639344262295084, 293.699999999999989);
INSERT INTO drinkware_volume_samples VALUES (395, 36, 0.983606557377049273, 332.699999999999989);
INSERT INTO drinkware_volume_samples VALUES (396, 36, 1, 341.300000000000011);
INSERT INTO drinkware_volume_samples VALUES (397, 37, 0.0833333333333333287, 21.3000000000000007);
INSERT INTO drinkware_volume_samples VALUES (398, 37, 0.166666666666666657, 39.7000000000000028);
INSERT INTO drinkware_volume_samples VALUES (399, 37, 0.25, 63.2999999999999972);
INSERT INTO drinkware_volume_samples VALUES (400, 37, 0.333333333333333315, 87);
INSERT INTO drinkware_volume_samples VALUES (401, 37, 0.416666666666666685, 111.700000000000003);
INSERT INTO drinkware_volume_samples VALUES (402, 37, 0.5, 139.300000000000011);
INSERT INTO drinkware_volume_samples VALUES (403, 37, 0.58333333333333337, 162);
INSERT INTO drinkware_volume_samples VALUES (404, 37, 0.66666666666666663, 191.699999999999989);
INSERT INTO drinkware_volume_samples VALUES (405, 37, 0.75, 220.300000000000011);
INSERT INTO drinkware_volume_samples VALUES (406, 37, 0.83333333333333337, 252);
INSERT INTO drinkware_volume_samples VALUES (407, 37, 0.91666666666666663, 283);
INSERT INTO drinkware_volume_samples VALUES (408, 37, 1, 317.300000000000011);
INSERT INTO drinkware_volume_samples VALUES (409, 37, 1, 317.300000000000011);
INSERT INTO drinkware_volume_samples VALUES (410, 38, 0.0952380952380952328, 5);
INSERT INTO drinkware_volume_samples VALUES (411, 38, 0.190476190476190466, 17);
INSERT INTO drinkware_volume_samples VALUES (412, 38, 0.285714285714285698, 45.2999999999999972);
INSERT INTO drinkware_volume_samples VALUES (413, 38, 0.380952380952380931, 81.7000000000000028);
INSERT INTO drinkware_volume_samples VALUES (414, 38, 0.476190476190476164, 129.699999999999989);
INSERT INTO drinkware_volume_samples VALUES (415, 38, 0.571428571428571397, 181.699999999999989);
INSERT INTO drinkware_volume_samples VALUES (416, 38, 0.66666666666666663, 235.300000000000011);
INSERT INTO drinkware_volume_samples VALUES (417, 38, 0.761904761904761862, 291);
INSERT INTO drinkware_volume_samples VALUES (418, 38, 0.857142857142857095, 344.699999999999989);
INSERT INTO drinkware_volume_samples VALUES (419, 38, 0.952380952380952328, 392.300000000000011);
INSERT INTO drinkware_volume_samples VALUES (420, 38, 1, 418);

SELECT pg_catalog.setval('drinkware_volume_samples_id_seq', 420, true);

SELECT pg_catalog.setval('food_groups_id_seq', 1, false);

INSERT INTO food_groups_local VALUES (0, 'en_GB', 'Unassigned');
INSERT INTO food_groups_local VALUES (1, 'en_GB', 'White bread/rolls');
INSERT INTO food_groups_local VALUES (2, 'en_GB', 'Brown and wholemeal bread/rolls');
INSERT INTO food_groups_local VALUES (3, 'en_GB', 'Sweet breads: malt bread, currant bread');
INSERT INTO food_groups_local VALUES (4, 'en_GB', 'Other breads <8g/100g fat: bagel, chapatis (made without fat), milk bread, grilled papadums, pitta, rye bread, soda, flour tortillas, crumpets');
INSERT INTO food_groups_local VALUES (5, 'en_GB', 'Sugar coated breakfast cereals: sugar puffs, frosties');
INSERT INTO food_groups_local VALUES (6, 'en_GB', 'High sugar breakfast cereals: sugar content above 30g total sugar per 100g');
INSERT INTO food_groups_local VALUES (7, 'en_GB', 'Other breakfast cereals: muesli, bran flakes');
INSERT INTO food_groups_local VALUES (8, 'en_GB', 'Breakfast alternatives: nutrigrain bar, pop tart, breakfast cereal bars');
INSERT INTO food_groups_local VALUES (9, 'en_GB', 'Rice');
INSERT INTO food_groups_local VALUES (10, 'en_GB', 'Pasta');

INSERT INTO foods_attributes VALUES (1, 'F000', NULL, true, NULL);
INSERT INTO foods_attributes VALUES (2, 'F001', NULL, NULL, NULL);
INSERT INTO foods_attributes VALUES (3, 'F002', NULL, NULL, NULL);
INSERT INTO foods_attributes VALUES (4, 'F003', NULL, NULL, NULL);
INSERT INTO foods_attributes VALUES (5, 'F004', true, NULL, NULL);
INSERT INTO foods_attributes VALUES (6, 'F005', NULL, NULL, 1234);
INSERT INTO foods_attributes VALUES (7, 'F006', true, NULL, NULL);
INSERT INTO foods_attributes VALUES (8, 'F007', NULL, NULL, NULL);
INSERT INTO foods_attributes VALUES (9, 'F008', NULL, NULL, NULL);

SELECT pg_catalog.setval('foods_attributes_id_seq', 9, true);


INSERT INTO foods_categories VALUES (1, 'F004', 'C000');
INSERT INTO foods_categories VALUES (2, 'F000', 'C000');
INSERT INTO foods_categories VALUES (3, 'F005', 'C000');
INSERT INTO foods_categories VALUES (4, 'F003', 'C001');
INSERT INTO foods_categories VALUES (5, 'F002', 'C002');
INSERT INTO foods_categories VALUES (6, 'F007', 'C002');
INSERT INTO foods_categories VALUES (7, 'F008', 'C002');
INSERT INTO foods_categories VALUES (8, 'F002', 'C005');
INSERT INTO foods_categories VALUES (9, 'F006', 'C005');

SELECT pg_catalog.setval('foods_categories_id_seq', 9, true);

INSERT INTO foods_local VALUES ('F000', 'en_GB', 'Food definition test 1', false, 'cb792b08-738f-46fb-9bbd-f37e96a79a23');
INSERT INTO foods_local VALUES ('F001', 'en_GB', 'Uncategorised food', false, 'c7472142-3364-465c-bace-3276a9d4af51');
INSERT INTO foods_local VALUES ('F002', 'en_GB', 'Parent test', false, 'e375313a-c705-40f0-b069-cbb019dbd015');
INSERT INTO foods_local VALUES ('F003', 'en_GB', 'Inheritance test 1', false, '89dac8df-1d39-4625-ba8a-cd7e1f7c8e5f');
INSERT INTO foods_local VALUES ('F004', 'en_GB', 'Food definition test 2', false, 'dead24f8-f29c-40c9-8041-1584e8e1e3b0');
INSERT INTO foods_local VALUES ('F005', 'en_GB', 'Food definition test 3', false, 'c6c2767b-499a-4eb5-a66f-06aa02ba2972');
INSERT INTO foods_local VALUES ('F006', 'en_GB', 'Inheritance test 2', false, 'abdffc81-d3f5-48f8-8a1d-db45a5c828f1');
INSERT INTO foods_local VALUES ('F007', 'en_GB', 'Default attributes test', false, 'ed0a169e-474c-4630-aa90-6dce02942613');
INSERT INTO foods_local VALUES ('F008', 'en_GB', 'PSM test 1', false, '60cb0687-a0e9-475d-ad6d-c912ffb4ae2f');

INSERT INTO foods_nutrient_tables VALUES ('F000', 'en_GB', 'NDNS', '100');
INSERT INTO foods_nutrient_tables VALUES ('F001', 'en_GB', 'NDNS', '1000');
INSERT INTO foods_nutrient_tables VALUES ('F002', 'en_GB', 'NDNS', '1000');
INSERT INTO foods_nutrient_tables VALUES ('F004', 'en_GB', 'NDNS', '200');
INSERT INTO foods_nutrient_tables VALUES ('F005', 'en_GB', 'NDNS', '300');

INSERT INTO foods_portion_size_methods VALUES (1, 'F000', 'en_GB', 'as-served', 'Test', 'portion/placeholder.jpg', false);
INSERT INTO foods_portion_size_methods VALUES (2, 'F008', 'en_GB', 'as-served', 'Blah', 'portion/placeholder.jpg', false);
INSERT INTO foods_portion_size_methods VALUES (3, 'F008', 'en_GB', 'guide-image', 'Blah Blah', 'test.jpg', true);

INSERT INTO foods_portion_size_method_params VALUES (1, 1, 'serving-image-set', 'as_served_1');
INSERT INTO foods_portion_size_method_params VALUES (2, 1, 'leftovers-image-set', 'as_served_1_leftovers');
INSERT INTO foods_portion_size_method_params VALUES (3, 2, 'serving-image-set', 'as_served_1');
INSERT INTO foods_portion_size_method_params VALUES (4, 2, 'leftovers-image-set', 'as_served_1_leftovers');
INSERT INTO foods_portion_size_method_params VALUES (5, 3, 'guide-image-id', 'guide1');

SELECT pg_catalog.setval('foods_portion_size_method_params_id_seq', 5, true);

SELECT pg_catalog.setval('foods_portion_size_methods_id_seq', 3, true);

INSERT INTO guide_images VALUES ('guide_1', 'Guide 1', 'guide_1.jpg');
INSERT INTO guide_images VALUES ('guide_2', 'Guide 2', 'guide_2.jpg');

INSERT INTO guide_image_weights VALUES (1, 'guide_1', 1, 'Aero - Bubbles bag', 135);
INSERT INTO guide_image_weights VALUES (2, 'guide_1', 2, 'Aero - standard Bubbles bag', 39);
INSERT INTO guide_image_weights VALUES (3, 'guide_1', 3, 'Aero - standard bar', 42.5);
INSERT INTO guide_image_weights VALUES (4, 'guide_1', 4, 'Aero - single', 27);
INSERT INTO guide_image_weights VALUES (5, 'guide_1', 5, 'Aero - medium', 19);
INSERT INTO guide_image_weights VALUES (6, 'guide_1', 6, 'Aero - treatsize', 9);
INSERT INTO guide_image_weights VALUES (7, 'guide_2', 1, '1', 1);
INSERT INTO guide_image_weights VALUES (8, 'guide_2', 2, '2', 2);
INSERT INTO guide_image_weights VALUES (9, 'guide_2', 3, '3', 3);
INSERT INTO guide_image_weights VALUES (10, 'guide_2', 4, '4', 4);

SELECT pg_catalog.setval('guide_image_weights_id_seq', 10, true);

INSERT INTO split_list VALUES (1, 'en_GB', 'biscuit', 'wafer cream marshmallow cream');
INSERT INTO split_list VALUES (2, 'en_GB', 'biscuits', 'wafer cream marshmallow cream');
INSERT INTO split_list VALUES (3, 'en_GB', 'branflakes', 'sultanas');
INSERT INTO split_list VALUES (4, 'en_GB', 'bread', 'butter');
INSERT INTO split_list VALUES (5, 'en_GB', 'bun', 'lettuce sesame');
INSERT INTO split_list VALUES (6, 'en_GB', 'cake', 'buttercream jam icing');
INSERT INTO split_list VALUES (7, 'en_GB', 'cakes', 'icing');
INSERT INTO split_list VALUES (8, 'en_GB', 'carrot', 'orange');
INSERT INTO split_list VALUES (9, 'en_GB', 'chilli', 'jalapeno');
INSERT INTO split_list VALUES (10, 'en_GB', 'cheese', 'tomato');

SELECT pg_catalog.setval('split_list_id_seq', 10, true);

INSERT INTO split_words VALUES (1, 'en_GB', 'with and ,');

SELECT pg_catalog.setval('split_words_id_seq', 1, true);

INSERT INTO synonym_sets VALUES (1, 'en_GB', 'cola pepsi coke');
INSERT INTO synonym_sets VALUES (2, 'en_GB', 'sandwich bap butty panini piece roll sarnie softie sub toastie wrap');

SELECT pg_catalog.setval('synonym_sets_id_seq', 2, true);

INSERT INTO categories_restrictions VALUES ('C005', 'test1');
INSERT INTO categories_restrictions VALUES ('C005', 'en_GB');
