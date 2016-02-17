/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

-- Locales

CREATE TABLE locales
(
  id character varying(16) NOT NULL,
  english_name character varying(64) NOT NULL,
  english_language_name character varying(64) NOT NULL,
  local_name character varying(64) NOT NULL,
  local_language_name character varying(64) NOT NULL,

  CONSTRAINT locales_pk PRIMARY KEY(id)
);

CREATE TABLE locale_prototypes
(
  locale_id character varying(16) NOT NULL,
  prototype_locale_id character varying(16) NOT NULL,

  CONSTRAINT locale_prototypes_pk PRIMARY KEY (locale_id, prototype_locale_id),
  CONSTRAINT locale_prototypes_only_one_prototype UNIQUE(locale_id),
  CONSTRAINT locale_prototypes_locale_id_fk FOREIGN KEY (locale_id)
    REFERENCES locales(id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT locale_prototypes_prototype_locale_id_fk FOREIGN KEY (prototype_locale_id)
    REFERENCES locales(id) ON UPDATE CASCADE ON DELETE CASCADE
);

-- Food groups

CREATE TABLE food_groups
(
  id serial NOT NULL,
  description character varying(256) NOT NULL,
  CONSTRAINT food_groups_id_pk PRIMARY KEY (id)
);

CREATE TABLE food_groups_local
(
  food_group_id integer NOT NULL,
  locale_id character varying(16) NOT NULL,
  local_description character varying(256) NOT NULL,

  CONSTRAINT food_groups_local_pk PRIMARY KEY(food_group_id, locale_id),
  CONSTRAINT food_groups_local_food_group_id_fk FOREIGN KEY (food_group_id)
    REFERENCES food_groups(id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT food_groups_local_locale_id_fk FOREIGN KEY (locale_id)
    REFERENCES locales(id) ON UPDATE CASCADE ON DELETE CASCADE
);

-- Food nutrient tables
CREATE TABLE nutrient_tables
(
  id character varying(32) NOT NULL,
  description character varying(512) NOT NULL,

  CONSTRAINT nutrient_tables_pk PRIMARY KEY (id)
);

-- Foods

CREATE TABLE foods
(
  code character(4) NOT NULL,
  description character varying(128) NOT NULL,
  food_group_id integer NOT NULL,
  version uuid NOT NULL,

  CONSTRAINT foods_code_pk PRIMARY KEY (code),
  CONSTRAINT food_group_id_fk FOREIGN KEY (food_group_id)
    REFERENCES food_groups (id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX foods_food_group_index ON foods (food_group_id);

CREATE TABLE foods_local
(
  food_code character(4) NOT NULL,
  locale_id character varying(16) NOT NULL,
  local_description character varying(128) NOT NULL,
  do_not_use boolean NOT NULL DEFAULT false,
  version uuid NOT NULL,

  CONSTRAINT foods_local_pk PRIMARY KEY(food_code, locale_id),
  CONSTRAINT foods_local_food_code_fk FOREIGN KEY(food_code)
    REFERENCES foods(code) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT food_local_locale_id_fk FOREIGN KEY (locale_id)
    REFERENCES locales(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE foods_restrictions
(
  food_code character(4) NOT NULL,
  locale_id character varying(16) NOT NULL,

  CONSTRAINT foods_restrictions_pk PRIMARY KEY (food_code, locale_id),
  CONSTRAINT foods_restrictions_food_code_fk FOREIGN KEY (food_code)
    REFERENCES foods(code) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT foods_restrictions_locale_id_fk FOREIGN KEY (locale_id)
    REFERENCES locales(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE foods_nutrient_tables
(
  food_code character(4) NOT NULL,
  locale_id character varying(16) NOT NULL,
  nutrient_table_id character varying(64) NOT NULL,
  nutrient_table_code character varying(64) NOT NULL,

  CONSTRAINT foods_nutrient_tables_pk PRIMARY KEY (food_code, locale_id,nutrient_table_id),
  CONSTRAINT foods_nutrient_tables_food_code_fk FOREIGN KEY (food_code)
    REFERENCES foods(code) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT foods_nutrient_tables_locale_id_fk FOREIGN KEY (locale_id)
    REFERENCES locales(id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT foods_nutrient_tables_nutrient_table_id_fk FOREIGN KEY (nutrient_table_id)
    REFERENCES nutrient_tables(id)
);

CREATE TABLE foods_portion_size_methods
(
  id serial NOT NULL,
  food_code character(4) NOT NULL,
  locale_id character varying(16) NOT NULL,
  method character varying(32) NOT NULL,
  description character varying(128) NOT NULL,
  image_url character varying(512),
  use_for_recipes boolean NOT NULL,
  CONSTRAINT foods_portion_size_methods_pk PRIMARY KEY (id),
  CONSTRAINT foods_portion_size_methods_food_id_fk FOREIGN KEY (food_code)
   REFERENCES foods (code) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT foods_portion_size_methods_locale_id_fk FOREIGN KEY (locale_id)
    REFERENCES locales(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX foods_portion_size_methods_food_code_index ON foods_portion_size_methods(food_code);

CREATE INDEX foods_portion_size_methods_locale_id_index ON foods_portion_size_methods(locale_id);

CREATE TABLE foods_portion_size_method_params
(
  id serial NOT NULL,
  portion_size_method_id integer NOT NULL,
  name character varying(32) NOT NULL,
  value character varying(128) NOT NULL,
  CONSTRAINT foods_portion_size_method_params_pk PRIMARY KEY (id),
  CONSTRAINT foods_portion_size_method_params_portion_size_method_id_fk FOREIGN KEY (portion_size_method_id)
      REFERENCES foods_portion_size_methods (id)
      ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX foods_portion_size_method_params_psm_id_index ON foods_portion_size_method_params(portion_size_method_id);

CREATE TABLE foods_attributes
(
  id serial NOT NULL,
  food_code character(4) NOT NULL,
    same_as_before_option boolean,
  ready_meal_option boolean,
  reasonable_amount integer,
  CONSTRAINT foods_attributes_pk PRIMARY KEY(id),
  CONSTRAINT foods_attributes_food_code_fk FOREIGN KEY(food_code)
    REFERENCES foods (code)
    ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX foods_attributes_food_code_index ON foods_attributes (food_code);

-- Categories

CREATE TABLE categories
(
  code character(4) NOT NULL,
  description character varying(128) NOT NULL,
  is_hidden boolean NOT NULL,
  version uuid NOT NULL,

  CONSTRAINT categories_pk PRIMARY KEY (code)
);

CREATE TABLE categories_local
(
  category_code character(4) NOT NULL,
  locale_id character varying(16) NOT NULL,
  local_description character varying(128) NOT NULL,
	do_not_use boolean NOT NULL DEFAULT false,
  version uuid NOT NULL,

  CONSTRAINT categories_local_pk PRIMARY KEY(category_code, locale_id),
  CONSTRAINT categories_local_category_code_fk FOREIGN KEY(category_code)
    REFERENCES categories(code) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT categories_local_locale_id_fk FOREIGN KEY (locale_id)
    REFERENCES locales(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE categories_restrictions
(
  category_code character(4) NOT NULL,
  locale_id character varying(16) NOT NULL,

  CONSTRAINT categories_restrictions_pk PRIMARY KEY (category_code, locale_id),
  CONSTRAINT categories_restrictions_food_code_fk FOREIGN KEY (category_code)
    REFERENCES categories(code) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT categories_restrictions_locale_id_fk FOREIGN KEY (locale_id)
    REFERENCES locales(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE foods_categories
(
  id serial NOT NULL,
  food_code character (4) NOT NULL,
  category_code character (4) NOT NULL,
  CONSTRAINT foods_categories_pk PRIMARY KEY (id),
  CONSTRAINT foods_categories_food_code_fk FOREIGN KEY(food_code)
    REFERENCES foods (code) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT foods_categories_category_code_fk FOREIGN KEY(category_code)
    REFERENCES categories (code) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT foods_categories_unique UNIQUE(food_code, category_code)
);

CREATE INDEX foods_categories_food_code_index ON foods_categories (food_code);

CREATE INDEX foods_categories_category_code_index ON foods_categories (category_code);

CREATE TABLE categories_categories
(
  id serial NOT NULL,
  subcategory_code character(4) NOT NULL,
  category_code character(4) NOT NULL,
  CONSTRAINT categories_categories_pk PRIMARY KEY (id),
  CONSTRAINT categories_categories_subcategory_code_fk FOREIGN KEY(subcategory_code)
    REFERENCES categories(code) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT categories_categories_category_code_fk FOREIGN KEY(category_code)
    REFERENCES categories(code) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT categories_categories_unique UNIQUE (subcategory_code, category_code)
);

CREATE INDEX categories_categories_subcategory_code_index ON categories_categories (subcategory_code);

CREATE INDEX categories_categories_category_code_index ON categories_categories (category_code);

CREATE TABLE categories_portion_size_methods
(
  id serial NOT NULL,
  category_code character(4) NOT NULL,
  locale_id character varying(16) NOT NULL,
  method character varying(32) NOT NULL,
  description character varying(128) NOT NULL,
  image_url character varying(512),
  use_for_recipes boolean NOT NULL,
  CONSTRAINT categories_portion_size_methods_pk PRIMARY KEY (id),
  CONSTRAINT categories_portion_size_methods_categories_code_fk FOREIGN KEY (category_code)
    REFERENCES categories (code) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT categories_portion_size_methods_locale_id_fk FOREIGN KEY (locale_id)
    REFERENCES locales(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX categories_portion_size_methods_categories_code_index ON categories_portion_size_methods (category_code);

CREATE INDEX categories_portion_size_methods_locale_id_index ON categories_portion_size_methods (locale_id);


CREATE TABLE categories_portion_size_method_params
(
  id serial NOT NULL,
  portion_size_method_id integer NOT NULL,
  name character varying(32) NOT NULL,
  value character varying(128) NOT NULL,
  CONSTRAINT categories_portion_size_method_params_pk PRIMARY KEY (id),
  CONSTRAINT categories_portion_size_method_params_portion_size_method_id_fk FOREIGN KEY (portion_size_method_id)
      REFERENCES categories_portion_size_methods (id)
      ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX categories_portion_size_method_params_psm_id_index ON categories_portion_size_method_params (portion_size_method_id);

CREATE TABLE categories_attributes
(
  id serial NOT NULL,
  category_code character(4) NOT NULL,
  same_as_before_option boolean,
  ready_meal_option boolean,
  reasonable_amount integer,
  CONSTRAINT categories_attributes_pk PRIMARY KEY(id),
  CONSTRAINT categories_attributes_category_code_fk FOREIGN KEY(category_code)
    REFERENCES categories (code)
    ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX categories_attributes_category_code_index ON categories_attributes (category_code);

-- As served images

CREATE TABLE as_served_sets
(
  id character varying(32) NOT NULL,
  description character varying(128) NOT NULL,
  CONSTRAINT as_served_sets_pk PRIMARY KEY (id)
);

CREATE TABLE as_served_images
(
  id serial NOT NULL,
  as_served_set_id character varying(32) NOT NULL,
  weight float NOT NULL,
  url character varying(512) NOT NULL,
  CONSTRAINT as_served_images_pk PRIMARY KEY (id),
  CONSTRAINT as_served_images_as_served_set_id_fk FOREIGN KEY (as_served_set_id)
      REFERENCES as_served_sets (id)
      ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX as_served_images_as_served_set_id_index ON as_served_images (as_served_set_id);

-- Guide images

CREATE TABLE guide_images
(
  id character varying(32) NOT NULL,
  description character varying(128) NOT NULL,
  base_image_url character varying(512) NOT NULL,
  CONSTRAINT guide_images_pk PRIMARY KEY (id)
);

CREATE TABLE guide_image_weights
(
  id serial NOT NULL,
  guide_image_id character varying(32) NOT NULL,
  object_id integer NOT NULL,
  description character varying(128) NOT NULL,
  weight float NOT NULL,

  CONSTRAINT guide_image_weights_pk PRIMARY KEY (id),
  CONSTRAINT guide_image_weights_guide_image_id_fk FOREIGN KEY (guide_image_id)
    REFERENCES guide_images(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX guide_image_weights_guide_image_id_index ON guide_image_weights (guide_image_id);

-- Brands

CREATE TABLE brands
(
  id serial NOT NULL,
  food_code character(4) NOT NULL,
  locale_id character varying(16) NOT NULL,
  name character varying(128) NOT NULL,
  CONSTRAINT brands_pk PRIMARY KEY (id),
  CONSTRAINT brands_food_code_fk FOREIGN KEY (food_code)
    REFERENCES foods (code) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT brands_food_locale_fk FOREIGN KEY (locale_id)
    REFERENCES locales(id) ON UPDATE CASCADE ON DELETE CASCADE

);

CREATE INDEX brands_food_code_index ON brands (food_code);

-- Drinkware

CREATE TABLE drinkware_sets
(
  id character varying (32) NOT NULL,
  description character varying (128) NOT NULL,
  guide_image_id character varying (32) NOT NULL,
  CONSTRAINT drinkware_sets_pk PRIMARY KEY (id)
  -- CONSTRAINT drinkware_sets_guide_image_id_fk
);

CREATE INDEX drinkware_sets_guide_image_id_index ON drinkware_sets (guide_image_id);

CREATE TABLE drinkware_scales
(
  id serial NOT NULL,
  drinkware_set_id character varying (32),
  width integer NOT NULL,
  height integer NOT NULL,
  empty_level integer NOT NULL,
  full_level integer NOT NULL,
  choice_id integer NOT NULL,
  base_image_url character varying (512) NOT NULL,
  overlay_image_url character varying (512) NOT NULL,

  CONSTRAINT drinkware_scales_pk PRIMARY KEY (id),
  CONSTRAINT drinkware_scales_set_id_fk FOREIGN KEY (drinkware_set_id)
    REFERENCES drinkware_sets(id)
    ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX drinkware_scales_drinkware_set_id_index ON drinkware_scales (drinkware_set_id);

CREATE TABLE drinkware_volume_samples
(
  id serial NOT NULL,
  drinkware_scale_id integer NOT NULL,
  fill float NOT NULL,
  volume float NOT NULL,

  CONSTRAINT drinkware_volume_samples_pk PRIMARY KEY (id),
  CONSTRAINT drinkware_volume_samples_drinkware_scale_id_fk FOREIGN KEY (drinkware_scale_id)
    REFERENCES drinkware_scales(id)
    ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX drinkware_volume_samples_drinkware_scale_id_index ON drinkware_volume_samples (drinkware_scale_id);

-- Associated food prompts

CREATE TABLE associated_food_prompts
(
  id serial NOT NULL,
  food_code character (4) NOT NULL,
  locale_id character varying(16) NOT NULL,
  category_code character (4) NOT NULL,
  text text NOT NULL,
  link_as_main boolean NOT NULL,
  generic_name character varying (128) NOT NULL,
  CONSTRAINT associated_food_prompts_pk PRIMARY KEY (id),
  CONSTRAINT associated_food_prompts_food_code_fk FOREIGN KEY (food_code)
    REFERENCES foods (code) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT associated_food_prompts_locale_id_fk FOREIGN KEY (locale_id)
    REFERENCES locales (id) ON UPDATE CASCADE ON DELETE CASCADE


  -- category code is not an FK (although it should be)
  -- because it can be either a food or a category
);

CREATE INDEX associated_food_prompts_food_code_index ON associated_food_prompts (food_code);

CREATE INDEX associated_food_prompts_food_locale_index ON associated_food_prompts (locale_id);

CREATE TABLE split_words
(
  id serial NOT NULL,
  locale_id character varying(16) NOT NULL,
  words text NOT NULL,

  CONSTRAINT split_words_pk PRIMARY KEY (id),
  CONSTRAINT split_words_locale_fk FOREIGN KEY(locale_id)
    REFERENCES locales(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX split_words_locale_index ON split_words(locale_id);

CREATE TABLE split_list
(
  id serial NOT NULL,
  locale_id character varying(16) NOT NULL,
  first_word character varying(64) NOT NULL,
  words text NOT NULL,

  CONSTRAINT split_list_pk PRIMARY KEY(id),
  CONSTRAINT split_list_locale_fk FOREIGN KEY(locale_id)
    REFERENCES locales(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX split_list_locale_index ON split_list(locale_id);

CREATE TABLE synonym_sets
(
  id serial NOT NULL,
  locale_id character varying(16) NOT NULL,
  synonyms text NOT NULL,

  CONSTRAINT synonym_sets_pk PRIMARY KEY(id),
  CONSTRAINT synonym_sets_locale_fk FOREIGN KEY(locale_id)
    REFERENCES locales(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX synonym_sets_locale_index ON synonym_sets(locale_id);

-- Default inheritable attributes

CREATE TABLE attribute_defaults
(
  id serial NOT NULL,
  same_as_before_option boolean,
  ready_meal_option boolean,
  reasonable_amount integer,

  CONSTRAINT attribute_defaults_pk PRIMARY KEY(id)
);

INSERT INTO attribute_defaults VALUES (DEFAULT, false, false, 1000);

INSERT INTO locales VALUES('en_GB', 'United Kingdom', 'English (UK)', 'United Kingdom', 'English (UK)');
INSERT INTO locales VALUES('ar_AE', 'United Arab Emirates', 'Arabic (UAE)', 'الإمارات العربية المتحدة' , 'العربية');
INSERT INTO nutrient_tables VALUES('NDNS', 'UK National Diet and Nutrition Survey');
