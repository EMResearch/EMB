ALTER TABLE behandling DROP COLUMN oppgave_id;
ALTER TABLE behandling ADD COLUMN behandling_opprinnelse varchar default 'MANUELL';
