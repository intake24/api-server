SELECT id AS user_id, name, email, phone,
       ARRAY(SELECT permission FROM user_permissions WHERE user_permissions.user_id = users.id AND user_permissions.survey_id={survey_id}) AS permissions,
       ARRAY(SELECT role FROM user_roles WHERE user_roles.user_id = users.id AND user_roles.survey_id={survey_id}) AS roles,
       ARRAY(SELECT ARRAY[name,value] FROM user_custom_fields WHERE user_custom_fields.user_id = users.id AND user_custom_fields.survey_id={survey_id}) AS custom_fields
FROM users WHERE survey_id={survey_id} ORDER BY id OFFSET {offset} LIMIT {limit}