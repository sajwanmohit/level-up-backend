ALTER TABLE app_user ADD COLUMN password_hash VARCHAR(100);

UPDATE app_user SET email = lower(btrim(email));

CREATE UNIQUE INDEX uk_app_user_email_lower ON app_user (lower(email));
