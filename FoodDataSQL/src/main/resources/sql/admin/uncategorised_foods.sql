WITH v AS(
  SELECT (SELECT id FROM locales WHERE id={locale_id}) AS locale_id
), t(code, description) AS(
  SELECT code, description FROM foods WHERE NOT EXISTS(SELECT 1 FROM foods_categories WHERE food_code=foods.code)
)
SELECT v.locale_id, code, description, local_description, COALESCE(do_not_use, false) as do_not_use, ARRAY(SELECT locale_id FROM foods_restrictions WHERE food_code = code) AS restrict
  FROM v CROSS JOIN t
         LEFT JOIN foods_local ON foods_local.food_code=t.code AND foods_local.locale_id=v.locale_id
UNION ALL
SELECT v.locale_id, NULL, NULL, NULL, NULL, NULL FROM v WHERE NOT EXISTS(SELECT 1 FROM t)
ORDER BY local_description
