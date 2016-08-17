WITH v AS(
  SELECT (SELECT id FROM locales WHERE id={locale_id}) as locale_id
),  t AS(
  SELECT v.locale_id, id, description, local_description 
    FROM v CROSS JOIN food_groups 
           LEFT JOIN food_groups_local ON food_groups_local.food_group_id = food_groups.id AND food_groups_local.locale_id=v.locale_id
)
SELECT locale_id, id, description, local_description FROM t
UNION ALL
SELECT v.locale_id, NULL, NULL, NULL FROM v WHERE NOT EXISTS (SELECT 1 FROM t LIMIT 1)