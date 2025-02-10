CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS wallet;

-- Create User table
CREATE TABLE IF NOT EXISTS wallet.user (
    id uuid PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

-- Create Credential table
CREATE TABLE IF NOT EXISTS wallet.credential (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    credential_type TEXT[] NOT NULL,
    credential_status VARCHAR(50) NOT NULL,
    credential_format VARCHAR(50),
    credential_data TEXT,
    json_vc TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES wallet.user (id) ON DELETE CASCADE
);

-- Create DeferredCredentialMetadata table
CREATE TABLE IF NOT EXISTS wallet.deferred_credential_metadata (
    id uuid PRIMARY KEY UNIQUE DEFAULT uuid_generate_v4(),
    transaction_id uuid NOT NULL,
    credential_id uuid NOT NULL,
    access_token VARCHAR(255) NOT NULL,
    deferred_endpoint VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_credential FOREIGN KEY (credential_id) REFERENCES wallet.credential (id) ON DELETE CASCADE
);