SELECT id AS user_id, u.survey_id, name, email, phone,
       ARRAY(SELECT ARRAY[name,value] FROM user_custom_fields WHERE user_custom_fields.user_id = u.id AND user_custom_fields.survey_id=u.survey_id) AS custom_fields
FROM survey_support_staff AS s JOIN users AS u ON u.survey_id=s.user_survey_id AND u.id=s.user_id
WHERE s.survey_id={survey_id}