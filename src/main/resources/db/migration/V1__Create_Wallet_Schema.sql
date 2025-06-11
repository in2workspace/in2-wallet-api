CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS wallet;

-- Create User table
CREATE TABLE IF NOT EXISTS wallet.user (
    id uuid PRIMARY KEY UNIQUE DEFAULT uuid_generate_v4(),
    user_id uuid,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

-- Add unique constraint to user_id
ALTER TABLE wallet.user
    ADD CONSTRAINT user_user_id_uk UNIQUE (user_id);

-- Create Credential table
CREATE TABLE IF NOT EXISTS wallet.credential (
    id uuid PRIMARY KEY UNIQUE DEFAULT uuid_generate_v4(),
    credential_id VARCHAR(255) NOT NULL,
    user_id uuid NOT NULL,
    credential_type TEXT[] NOT NULL,
    credential_status VARCHAR(50) NOT NULL,
    credential_format VARCHAR(50),
    credential_data TEXT,
    json_vc TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES wallet.user (user_id) ON DELETE CASCADE
);

-- Add unique constraint to credential_id
ALTER TABLE wallet.credential
    ADD CONSTRAINT credential_credential_id_uk UNIQUE (credential_id);

-- Create DeferredCredentialMetadata table
CREATE TABLE IF NOT EXISTS wallet.deferred_credential_metadata (
    id uuid PRIMARY KEY UNIQUE DEFAULT uuid_generate_v4(),
    transaction_id uuid NOT NULL,
    credential_id VARCHAR(255) NOT NULL,
    access_token VARCHAR(255) NOT NULL,
    deferred_endpoint VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_credential FOREIGN KEY (credential_id) REFERENCES wallet.credential (credential_id) ON DELETE CASCADE
);