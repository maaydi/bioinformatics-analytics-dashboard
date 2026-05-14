-- Remove old incompatible version if it exists
DROP FUNCTION IF EXISTS fts_match(tsvector, text);

-- Create PostgreSQL full-text-search helper
CREATE FUNCTION fts_match(
    document tsvector,
    query_text text
)
    RETURNS boolean
    LANGUAGE sql
    IMMUTABLE
AS
$$
SELECT document @@ websearch_to_tsquery('english', query_text);
$$;