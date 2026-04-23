-- Runs on first Postgres startup (mounted at /docker-entrypoint-initdb.d).
-- Creates one database and role per microservice.

CREATE ROLE template_service      WITH LOGIN PASSWORD 'template_service';
CREATE ROLE clause_service        WITH LOGIN PASSWORD 'clause_service';
CREATE ROLE questionnaire_service WITH LOGIN PASSWORD 'questionnaire_service';
CREATE ROLE assembly_service      WITH LOGIN PASSWORD 'assembly_service';
CREATE ROLE document_service      WITH LOGIN PASSWORD 'document_service';
CREATE ROLE admin_service         WITH LOGIN PASSWORD 'admin_service';
CREATE ROLE audit_service         WITH LOGIN PASSWORD 'audit_service';
CREATE ROLE keycloak              WITH LOGIN PASSWORD 'keycloak';

CREATE DATABASE template_service      OWNER template_service;
CREATE DATABASE clause_service        OWNER clause_service;
CREATE DATABASE questionnaire_service OWNER questionnaire_service;
CREATE DATABASE assembly_service      OWNER assembly_service;
CREATE DATABASE document_service      OWNER document_service;
CREATE DATABASE admin_service         OWNER admin_service;
CREATE DATABASE audit_service         OWNER audit_service;
CREATE DATABASE keycloak              OWNER keycloak;
