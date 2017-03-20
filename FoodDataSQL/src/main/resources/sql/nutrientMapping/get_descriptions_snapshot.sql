SELECT code, foods.description, foods_local.local_description, foods.food_group_id,
  food_groups.description AS food_group_description, food_groups_local.local_description AS food_group_local_description
FROM foods
  LEFT JOIN foods_local ON foods.code=foods_local.food_code AND foods_local.locale_id={locale_id}
  JOIN food_groups ON foods.food_group_id=food_groups.id
  LEFT JOIN food_groups_local ON foods.food_group_id=food_groups_local.food_group_id AND food_groups_local.locale_id={locale_id}
WHERE foods.code IN ({food_codes})