UPDATE BEHANDLING
SET behandling_opprinnelse = 'FØDSELSHENDELSE'
where behandling_opprinnelse = 'AUTOMATISK_VED_FØDSELSHENDELSE';

UPDATE BEHANDLING
SET behandling_opprinnelse = 'SØKNAD'
where behandling_opprinnelse = 'MANUELL'
   OR behandling_opprinnelse = 'AUTOMATISK_VED_JOURNALFØRING';

ALTER TABLE BEHANDLING
    RENAME COLUMN behandling_opprinnelse TO opprettet_aarsak;
ALTER TABLE BEHANDLING
    ADD COLUMN skal_behandles_automatisk BOOLEAN default false;