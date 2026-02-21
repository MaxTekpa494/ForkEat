-- Contrainte UNIQUE sur stripe_transaction_id (idempotence au niveau BDD)
ALTER TABLE transactions
  ADD CONSTRAINT uq_stripe_transaction_id UNIQUE (stripe_transaction_id);

-- Index pour accelerer les requetes d'historique par wallet
CREATE INDEX idx_transaction_dest_date
  ON transactions (destination_wallet_id, created_at DESC);

-- Contrainte CHECK : montant toujours positif
ALTER TABLE transactions
  ADD CONSTRAINT chk_transaction_amount_positive CHECK (amount > 0);

-- Contrainte CHECK : balance jamais negative
ALTER TABLE wallets
  ADD CONSTRAINT chk_wallet_balance_non_negative CHECK (balance >= 0);
