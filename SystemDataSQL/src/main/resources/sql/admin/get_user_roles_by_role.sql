SELECT r2.survey_id, r2.user_id, r2.role 
FROM user_roles r1 JOIN user_roles r2 ON  (r1.survey_id = r2.survey_id AND r1.user_id = r2.user_id)
WHERE (r1.role = {role} AND r1.survey_id = {survey_id})
ORDER BY (r2.survey_id, r2.user_id)