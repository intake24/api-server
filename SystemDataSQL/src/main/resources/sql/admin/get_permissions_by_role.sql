SELECT r.survey_id as survey_id, r.user_id as user_id, permission 
FROM user_roles r JOIN user_permissions p ON (r.survey_id = p.survey_id AND r.user_id = p.user_id)
WHERE (role = {role} AND r.survey_id = {survey_id})
ORDER BY (r.survey_id, r.user_id)