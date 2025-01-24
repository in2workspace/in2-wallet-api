CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create User table
CREATE TABLE IF NOT EXISTS wallet.user (
    id uuid PRIMARY KEY UNIQUE DEFAULT uuid_generate_v4(),
    user_id uuid,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create Credential table
CREATE TABLE IF NOT EXISTS wallet.credential (
    id uuid PRIMARY KEY UNIQUE DEFAULT uuid_generate_v4(),
    credential_id uuid NOT NULL,
    user_id uuid NOT NULL,
    credential_type TEXT[] NOT NULL,
    credential_status INTEGER NOT NULL,
    credential_format INTEGER,
    credential_data VARCHAR(255),
    json_vc JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES wallet.user (user_id) ON DELETE CASCADE
);

-- Create DeferredCredentialMetadata table
CREATE TABLE IF NOT EXISTS wallet.deferred_credential_metadata (
    id uuid PRIMARY KEY UNIQUE DEFAULT uuid_generate_v4(),
    transaction_id uuid NOT NULL,
    credential_id uuid NOT NULL,
    access_token VARCHAR(255) NOT NULL,
    deferred_endpoint VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_credential FOREIGN KEY (credential_id) REFERENCES wallet.credential (credential_id) ON DELETE CASCADE
);