UPDATE behandling
SET status = 'SATT_PÅ_VENT'
WHERE id IN (SELECT fk_behandling_id from sett_paa_vent WHERE aktiv = true);