WITH p AS (
    SELECT prototype_locale_id AS l FROM locales WHERE id={locale_id} UNION SELECT {locale_id} AS l
)
SELECT code, description, local_description, simple_local_description,
  CASE WHEN do_not_use IS NULL THEN false ELSE do_not_use END AS do_not_use,
  ARRAY(SELECT locale_id FROM foods_restrictions WHERE food_code = code) AS restrict
FROM foods LEFT JOIN foods_local ON foods.code = foods_local.food_code
WHERE (lower(code) LIKE {pattern} OR lower(simple_local_description) LIKE {pattern}) AND foods_local.locale_id IN (SELECT l FROM p)
ORDER BY local_description DESC
LIMIT 30