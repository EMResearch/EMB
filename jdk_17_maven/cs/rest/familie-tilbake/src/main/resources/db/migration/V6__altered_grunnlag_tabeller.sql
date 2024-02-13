ALTER TABLE kravgrunnlag431
    ADD COLUMN behandling_id UUID REFERENCES behandling;

COMMENT ON COLUMN kravgrunnlag431.behandling_id
    IS 'Fk: behandling fremmednøkkel for tilknyttet behandling';

ALTER TABLE kravgrunnlag431
    ADD COLUMN aktiv BOOLEAN;

COMMENT ON COLUMN kravgrunnlag431.aktiv
    IS 'Angir status av grunnlag';

ALTER TABLE kravgrunnlag431
    ADD COLUMN sperret BOOLEAN;

COMMENT ON COLUMN kravgrunnlag431.sperret
    IS 'Angir om grunnlaget har fått sper melding fra økonomi';

ALTER TABLE kravgrunnlag431
    ADD COLUMN utbet_id_type VARCHAR;

COMMENT ON COLUMN kravgrunnlag431.utbet_id_type IS 'Angir om Utbetales-til-id er fnr, orgnr, TSS-nr etc';

ALTER TABLE kravgrunnlag431
    ALTER COLUMN behandling_id SET NOT NULL;

ALTER TABLE kravgrunnlag431
    ALTER COLUMN aktiv SET NOT NULL;

ALTER TABLE kravgrunnlag431
    ALTER COLUMN sperret SET NOT NULL;

ALTER TABLE kravgrunnlag431
    ALTER COLUMN utbet_id_type SET NOT NULL;

ALTER TABLE kravgrunnlag431
    RENAME COLUMN fagsystem TO fagsystem_id;

DROP TABLE gruppering_krav_grunnlag;
