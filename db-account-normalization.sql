-- Normalize existing account numbers to exactly 8 characters.
-- Rule used in app: trim, uppercase, then left-pad with 0 up to 8 chars.
-- If value is longer than 8, keep first 8 chars.

UPDATE transactions
SET account_number = CASE
    WHEN account_number IS NULL THEN NULL
    WHEN CHAR_LENGTH(TRIM(account_number)) = 0 THEN account_number
    WHEN CHAR_LENGTH(TRIM(account_number)) >= 8 THEN LEFT(UPPER(TRIM(account_number)), 8)
    ELSE LPAD(UPPER(TRIM(account_number)), 8, '0')
END;
