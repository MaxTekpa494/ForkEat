ALTER TABLE bank_infos
    DROP COLUMN iban,
    DROP COLUMN bic;

ALTER TABLE bank_infos
    ADD COLUMN external_account_id VARCHAR(255) NOT NULL;