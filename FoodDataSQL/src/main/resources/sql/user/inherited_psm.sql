WITH RECURSIVE t(code, level) AS (
  (SELECT category_code as code, 0 as level FROM foods_categories WHERE food_code = {food_code} ORDER BY code)
    UNION
  (SELECT category_code as code, level + 1 as level FROM t JOIN categories_categories ON subcategory_code = code ORDER BY code)
)
SELECT categories_portion_size_methods.id, category_code, method, description, image_url, use_for_recipes,
       categories_portion_size_method_params.id as param_id, name as param_name, value as param_value
  FROM
    categories_portion_size_methods JOIN t ON code = category_code
    LEFT JOIN categories_portion_size_method_params ON categories_portion_size_methods.id = categories_portion_size_method_params.portion_size_method_id
WHERE categories_portion_size_methods.locale_id = {locale_id}              
ORDER BY level, id, param_id
