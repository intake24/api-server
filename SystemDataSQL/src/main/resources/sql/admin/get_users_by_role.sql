SELECT users.survey_id AS survey_id, users.id AS user_id, password_hash, password_salt, password_hasher
FROM users JOIN user_roles ON (users.survey_id = user_roles.survey_id AND users.id = user_roles.user_id) 
WHERE (role = {role} AND users.survey_id = {survey_id})
ORDER BY (users.survey_id, users.id)