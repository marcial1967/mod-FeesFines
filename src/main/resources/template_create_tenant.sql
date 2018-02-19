CREATE ROLE myuniversity_mymodule PASSWORD 'myuniversity' NOSUPERUSER NOCREATEDB INHERIT LOGIN;

GRANT myuniversity_mymodule TO CURRENT_USER;

CREATE SCHEMA myuniversity_mymodule AUTHORIZATION myuniversity_mymodule;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS myuniversity_mymodule.feefines (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), jsonb JSONB NOT NULL);

CREATE TABLE IF NOT EXISTS myuniversity_mymodule.owners (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), jsonb JSONB NOT NULL);

CREATE TABLE IF NOT EXISTS myuniversity_mymodule.accounts (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), jsonb JSONB NOT NULL);

CREATE TABLE IF NOT EXISTS myuniversity_mymodule.feefineactions (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), jsonb 
JSONB NOT NULL);

-- View Fees Fines History
CREATE VIEW myuniversity_mymodule.feefine_history_view AS  SELECT id AS id,row_to_json(t) AS jsonb
FROM (
SELECT a.jsonb->>'id' AS id,
  SUBSTRING(a.jsonb->>'dateCreated' FROM 1 FOR 10) AS "dateCreated",
  SUBSTRING(a.jsonb->>'dateUpdated' FROM 1 FOR 10) AS "dateUpdated",
  f.jsonb->'feeFineType' AS "feeFineType",
  a.jsonb->'amount' AS "charged",
  a.jsonb->'remaining' AS "remaining",
  a.jsonb->'status'->>'name' AS "status",
  a.jsonb->'paymentStatus'->>'name' AS "paymentStatus",
  o.jsonb->'desc' AS "feeFineOwner",
  ins.jsonb->'title' AS "item",
  i.jsonb->'barcode' AS "barcode",
  m.jsonb->'name' AS "itemType",
  h.jsonb->'callNumber' AS "callNumber",
  a.jsonb->'accountTransaction' AS "loanTransaction",
  a.jsonb->'comments' AS "comments",
  a.jsonb->'userId' AS "userId"
  FROM myuniversity_mymodule.accounts a
  INNER JOIN diku_mod_users.users u ON u.jsonb->>'id' = a.jsonb->>'userId'
  INNER JOIN myuniversity_mymodule.feefines f ON f.jsonb->>'id' = a.jsonb->>'feeFineId'
  INNER JOIN myuniversity_mymodule.owners o ON o.jsonb->>'id' = a.jsonb->>'ownerId'
  LEFT JOIN diku_mod_circulation_storage.loan l ON a.jsonb->>'loanId' = l.jsonb->>'id'
  LEFT JOIN diku_mod_inventory_storage.item i ON i.jsonb->>'id' = a.jsonb->>'itemId'
  LEFT JOIN diku_mod_inventory_storage.material_type m ON i.jsonb->>'materialTypeId' = m.jsonb->>'id'
  LEFT JOIN diku_mod_inventory_storage.holdings_record h ON i.jsonb->>'holdingsRecordId' = h.jsonb->>'id'
  LEFT JOIN diku_mod_inventory_storage.instance ins ON ins.jsonb->>'id' = h.jsonb->>'instanceId'
) t;

--View item information
CREATE VIEW myuniversity_mymodule.item_information_view AS  SELECT id AS id, to_jsonb(t) AS jsonb
FROM (
  SELECT i.jsonb->>'id' AS "id",
  ins.jsonb->'title' AS "instance",
  h.jsonb->'callNumber' AS "callNumber",
  i.jsonb->'barcode' AS "barcode",
  i.jsonb -> 'materialTypeId' AS "materialTypeId",
  i.jsonb->'status'->>'name' AS "itemStatus",
  s.jsonb->'name' AS "location"
  FROM diku_mod_inventory_storage.instance ins
  INNER JOIN diku_mod_inventory_storage.holdings_record h ON ins.jsonb->'id' = h.jsonb->'instanceId'
  INNER JOIN diku_mod_inventory_storage.item i ON h.jsonb->'id' = i.jsonb->'holdingsRecordId'
  LEFT JOIN diku_mod_inventory_storage.shelflocation s ON h.jsonb->'permanentLocationId' = s.jsonb->'id'
) t;

GRANT ALL ON myuniversity_mymodule.item_information_view TO myuniversity_mymodule;

GRANT ALL ON myuniversity_mymodule.feefine_history_view TO myuniversity_mymodule;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA myuniversity_mymodule TO myuniversity_mymodule;
