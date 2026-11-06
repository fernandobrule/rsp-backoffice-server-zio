
SELECT name, default_version, installed_version
FROM pg_available_extensions
WHERE name = 'postgis';

SELECT ST_Distance(
    ST_SetSRID(ST_MakePoint(-46.6333, -23.5505), 4326)::geography,
    ST_SetSRID(ST_MakePoint(-46.6433, -23.5505), 4326)::geography
);

