-- Assouplissement de la contrainte : les super-likes gratuits (bonus) ont un montant à 0
ALTER TABLE transactions
  DROP CONSTRAINT chk_transaction_amount_positive;

ALTER TABLE transactions
  ADD CONSTRAINT chk_transaction_amount_non_negative CHECK (amount >= 0);