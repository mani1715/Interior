-- H2 In-Memory Database Compatibility for PostgreSQL 18 functions & types
CREATE ALIAS IF NOT EXISTS uuidv7 FOR "java.util.UUID.randomUUID";
CREATE DOMAIN IF NOT EXISTS timestamptz AS TIMESTAMP WITH TIME ZONE;
