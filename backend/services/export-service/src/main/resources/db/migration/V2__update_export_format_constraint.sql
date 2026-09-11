ALTER TABLE export_data.export_pipeline
DROP
CONSTRAINT IF EXISTS export_pipeline_format_check;

-- 2. Expand the column length to allow longer custom format identifiers
ALTER TABLE export_data.export_pipeline
ALTER
COLUMN format TYPE VARCHAR(50);