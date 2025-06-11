ALTER TABLE wallet.credential
ALTER COLUMN credential_id TYPE TEXT;

ALTER TABLE wallet.deferred_credential_metadata
ALTER COLUMN credential_id TYPE TEXT;