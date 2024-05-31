---
layout: page
title: Wallet Server Technical Documentation
date: 2024-04-24
version: 1.2.0
---

<h1>Technical Documentation</h1>

<h2>Table of Contents</h2>
<!-- TOC -->
* [Introduction](#introduction)
* [Architecture](#architecture)
* [Data Model](#data-model)
  * [Relationship Diagram](#relationship-diagram)
  * [Entities](#entities)
    * [User Entity](#user-entity)
    * [Credential Entity](#credential-entity)
    * [Transaction Entity](#transaction-entity)
<!-- TOC -->

# Introduction
Wallet Server is a Java/Spring application that provides a REST API for managing a wallet.

# Architecture

# Data Model

The Wallet solution is based on the following entities:

## Relationship Diagram

![Data Model Relationship Diagram](./diagrams/data-model-diagram.png)

## Entities

### User Entity

> NOTE: This is a non-normative example of a User entity.

```json
{
  "id": "urn:WalletUser:f517af8f-954e-47c2-a778-dd312154dc2d",
  "type": "WalletUser"
}
```

### Credential Entity

> NOTE: This is a non-normative example of a Credential entity.

```json
{
  "id": "urn:credential:80e19af1-c487-4598-a2b3-c0d8b917481f",
  "type": "Credential",
  "status": {
    "type": "Property",
    "value": "Issued"
  },
  "credentialType": {
    "type": "Property",
    "value": ["LEARCredential", "VerifiableCredential"]
  },
  "jwt_vc": {
    "type": "Property",
    "value": "eyJhbGciOiJFUzI1NksifQ.eyJzdWIiOiJkaWQ6a2V5OjEyMzQ1Njc4IiwiaXNzIjoiZGlkOmtleToxMjM0NTY3OCIsImlhdCI6MTUxNjIzOTAyMn0.3J3"
  },
  "cwt_vc": {
    "type": "Property",
    "value": "eyJhbGciOiJFUzI1NksifQ.eyJzdWIiOiJkaWQ6a2V5OjEyMzQ1Njc4IiwiaXNzIjoiZGlkOmtleToxMjM0NTY3OCIsImlhdCI6MTUxNjIzOTAyMn0.3J3"
  },
  "json_vc": {
    "type": "Property",
    "value": {
      "id": "urn:Credential:123",
      "subject": "did:key:45684"
    }
  },
  "belongsTo": {
    "type": "Relationship",
    "object": "urn:WalletUser:f517af8f-954e-47c2-a778-dd312154dc2d"
  }
}
```

### Transaction Entity

> NOTE: This is a non-normative example of a Transaction entity.

```json
{
  "id": "urn:Transaction:958e84cf-888b-488a-bf30-7f3b14f70699",
  "type": "Transaction",
  "linkedTo": {
    "type": "Relationship",
    "object": "urn:Credential:80e19af1-c487-4598-a2b3-c0d8b917481f"
  },
  "transactionData": {
    "type" : "Property",
    "value" : {
      "transaction_id" : "1234",
      "access_token" : "ey123",
      "deferred_endpoint" : "https://isser.com/deferred"
    }
  }
}
```
