SELECT r.survey_id as survey_id, r.user_id as user_id, name, value 
FROM user_roles r JOIN user_custom_fields f ON (r.survey_id = f.survey_id AND r.user_id = f.user_id)
WHERE (role = {role} AND r.survey_id = {survey_id})
ORDER BY (r.survey_id, r.user_id)