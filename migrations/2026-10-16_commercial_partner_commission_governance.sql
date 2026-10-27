ALTER TABLE commercial_partners
    ADD COLUMN commission_configured BOOLEAN NOT NULL DEFAULT TRUE;

-- Existing percentages remain configured. A secondary partner's level is chosen
-- for each operation, never stored on the partner catalog record.
