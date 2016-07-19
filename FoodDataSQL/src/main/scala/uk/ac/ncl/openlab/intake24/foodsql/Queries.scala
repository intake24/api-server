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

package uk.ac.ncl.openlab.intake24.foodsql

object Queries {
  val foodsInsert = """INSERT INTO foods VALUES ({code}, {description}, {food_group_id}, {version}::uuid)"""
  
  val foodsUpdate = """UPDATE foods SET code = {new_code}, description={description}, food_group_id={food_group_id}, version={new_version}::uuid WHERE code={food_code} AND version={base_version}::uuid"""
  
  val foodsDelete = """DELETE FROM foods WHERE code={food_code}"""
  
  val foodsDeleteVersioned = """DELETE FROM foods WHERE code={food_code} AND version={version}::uuid"""
  
  val foodsLocalInsert = """INSERT INTO foods_local VALUES({food_code}, {locale_id}, {local_description}, {do_not_use}, {version}::uuid)"""
  
  val foodsLocalUpdate = """UPDATE foods_local SET version = {new_version}::uuid, local_description = {local_description}, do_not_use = {do_not_use} WHERE food_code = {food_code} AND locale_id = {locale_id} AND version = {base_version}::uuid"""
  
  val foodsLocalDelete = """DELETE FROM foods_local WHERE food_code={food_code} AND locale_id={locale_id}"""
  
  val foodNutrientTablesInsert = """INSERT INTO foods_nutrient_mapping VALUES ({food_code}, {locale_id}, {nutrient_table_id}, {nutrient_table_code})"""
  
  val foodNutrientTablesDelete = """DELETE FROM foods_nutrient_mapping WHERE food_code={food_code} AND locale_id={locale_id}"""
  
  val foodsAttributesInsert = """INSERT INTO foods_attributes VALUES (DEFAULT, {food_code}, {same_as_before_option}, {ready_meal_option}, {reasonable_amount})"""
  
  val foodsAttributesDelete = """DELETE FROM foods_attributes WHERE food_code={food_code}""" 
  
  val foodsPortionSizeMethodsInsert = """INSERT INTO foods_portion_size_methods VALUES(DEFAULT, {food_code}, {locale_id}, {method}, {description}, {image_url}, {use_for_recipes})"""
  
  val foodsPortionSizeMethodsDelete = """DELETE FROM foods_portion_size_methods WHERE food_code={food_code} AND locale_id={locale_id}"""
  
  val foodsPortionSizeMethodsParamsInsert = """INSERT INTO foods_portion_size_method_params VALUES(DEFAULT, {portion_size_method_id}, {name}, {value})"""
  
  
  
  val categoriesInsert = """INSERT INTO categories VALUES({code},{description},{is_hidden},{version}::uuid)"""
  
  val categoriesUpdate = """UPDATE categories SET code = {new_code}, description={description}, is_hidden={is_hidden}, version={new_version}::uuid WHERE code={category_code} AND version={base_version}::uuid"""
 
  val categoriesDelete = """DELETE FROM categories WHERE code={category_code}"""
  
  val categoriesLocalInsert = """INSERT INTO categories_local VALUES({category_code}, {locale_id}, {local_description}, {do_not_use}, {version}::uuid)"""
  
  val categoriesLocalUpdate = """UPDATE categories_local SET version = {new_version}::uuid, local_description = {local_description} WHERE category_code = {category_code} AND locale_id = {locale_id} AND version = {base_version}::uuid"""
  
  val categoriesLocalDelete = """DELETE FROM categories_local WHERE category_code={category_code} AND locale_id={locale_id}"""
  
  val foodsCategoriesInsert = """INSERT INTO foods_categories VALUES(DEFAULT, {food_code},{category_code})"""
  
  val foodsCategoriesDelete = """DELETE FROM foods_categories WHERE food_code={food_code} AND category_code={category_code}"""
  
  val categoriesCategoriesInsert = """INSERT INTO categories_categories VALUES(DEFAULT, {subcategory_code},{category_code})"""
  
  val categoriesCategoriesDelete = """DELETE FROM categories_categories WHERE subcategory_code={subcategory_code} AND category_code={category_code}"""
  
  val categoriesAttributesInsert = """INSERT INTO categories_attributes VALUES (DEFAULT, {category_code}, {same_as_before_option}, {ready_meal_option}, {reasonable_amount})"""
  
  val categoriesAttributesDelete = """DELETE FROM categories_attributes WHERE category_code={category_code}"""
  
  val categoriesPortionSizeMethodsInsert = """INSERT INTO categories_portion_size_methods VALUES(DEFAULT, {category_code}, {locale_id}, {method}, {description}, {image_url}, {use_for_recipes})"""
  
  val categoriesPortionSizeMethodsDelete = """DELETE FROM categories_portion_size_methods WHERE category_code={category_code} AND locale_id={locale_id}"""
  
  val categoriesPortionSizeMethodParamsInsert = """INSERT INTO categories_portion_size_method_params VALUES(DEFAULT, {portion_size_method_id}, {name}, {value})"""
  
}