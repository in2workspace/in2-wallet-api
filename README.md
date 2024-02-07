<div align="center">

<h1>Wallet Server</h1>
<span>by </span><a href="https://in2.es">in2.es</a>
<p><p>

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=in2workspace_wallet-server&metric=alert_status)](https://sonarcloud.io/dashboard?id=in2workspace_wallet-server)

[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=in2workspace_credential-issuer&metric=bugs)](https://sonarcloud.io/summary/new_code?in2workspace_credential-issuer)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=in2workspace_credential-issuer&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=in2workspace_credential-issuer)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=in2workspace_credential-issuer&metric=security_rating)](https://sonarcloud.io/dashboard?id=in2workspace_credential-issuer)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=in2workspace_credential-issuer&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=in2workspace_credential-issuer)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=in2workspace_credential-issuer&metric=ncloc)](https://sonarcloud.io/dashboard?id=in2workspace_credential-issuer)

[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=in2workspace_wallet-server&metric=coverage)](https://sonarcloud.io/summary/new_code?id=in2workspace_wallet-server)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=in2workspace_credential-issuer&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=in2workspace_credential-issuer)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=in2workspace_credential-issuer&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=in2workspace_credential-issuer)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=in2workspace_credential-issuer&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=in2workspace_credential-issuer)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=in2workspace_credential-issuer&metric=sqale_index)](https://sonarcloud.io/summary/new_code?id=in2workspace_credential-issuer)

</div>

## Introduction

Wallet Server is a service that allows to manage digital credentials. It is designed to be used in a decentralized identity ecosystem, where users can store their credentials in a secure and private way.

Wallet Server includes the requested features described in the [EUDI Wallet Arquitecture Reference](https://github.com/eu-digital-identity-wallet/eudi-doc-architecture-and-reference-framework/blob/main/docs/arf.md), and it is EBSI compliance ([EBSI test v3.4](https://hub.ebsi.eu/wallet-conformance)).

## Features
- Create did:key identifier from ES256 key algorithm.
- Create did:key:jwk_jcs-pub identifier from ES256 key algorithm.

## Supported Platforms
* Keycloak //todo version
* Scorpio //todo version
* Orion-LD //todo version
* Hashicorp Vault //todo version

## Resources
* [Google Java Style](https://github.com/checkstyle/checkstyle/blob/master/src/main/resources/google_checks.xml)
