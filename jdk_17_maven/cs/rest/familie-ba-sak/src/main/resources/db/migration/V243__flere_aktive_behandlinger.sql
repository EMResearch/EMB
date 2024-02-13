UPDATE behandling SET aktivert_tid = opprettet_tid;
ALTER TABLE behandling ALTER COLUMN aktivert_tid DROP DEFAULT;

-- Kun en behandling kan ha status annet enn AVSLUTTET eller SATT_PÅ_MASKINELL_VENT
CREATE UNIQUE INDEX UIDX_BEHANDLING_02 ON behandling (fk_fagsak_id) WHERE (status <> 'AVSLUTTET' AND status <> 'SATT_PÅ_MASKINELL_VENT');
-- Kun en behandling kan ha status SATT_PÅ_VENT
CREATE UNIQUE INDEX UIDX_BEHANDLING_03 ON behandling (fk_fagsak_id) WHERE status = 'SATT_PÅ_MASKINELL_VENT';
