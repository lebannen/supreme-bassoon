-- Increase dialect column length from 50 to 200 characters
ALTER TABLE pronunciations ALTER COLUMN dialect TYPE VARCHAR(200);
