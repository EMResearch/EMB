UPDATE behandling_steg_tilstand
SET behandling_steg = 'VURDER_TILBAKEKREVING'
WHERE behandling_steg = 'SIMULERING'