UPDATE behandling_steg_tilstand
SET behandling_steg_status = 'UTFØRT'
WHERE behandling_steg = 'BEHANDLING_AVSLUTTET';