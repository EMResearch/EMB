ALTER TABLE totrinnsresultatsgrunnlag
    RENAME COLUMN gruppering_fakta_feilutbetaling_id TO fakta_feilutbetaling_id;

ALTER TABLE totrinnsresultatsgrunnlag
    DROP CONSTRAINT totrinnsresultatsgrunnlag_gruppering_fakta_feilutbetaling__fkey;

ALTER TABLE totrinnsresultatsgrunnlag
    ADD CONSTRAINT totrinnsresultatsgrunnlag_fakta_feilutbetaling_fkey FOREIGN KEY (fakta_feilutbetaling_id) REFERENCES fakta_feilutbetaling;

ALTER TABLE fakta_feilutbetaling
    ADD COLUMN behandling_id UUID REFERENCES behandling;

COMMENT ON COLUMN fakta_feilutbetaling.behandling_id
    IS 'Referanse til behandling';

ALTER TABLE fakta_feilutbetaling
    ADD COLUMN aktiv BOOLEAN;

COMMENT ON COLUMN fakta_feilutbetaling.aktiv
    IS 'Angir status av fakta om feilutbetaling';

ALTER TABLE fakta_feilutbetaling
    ALTER COLUMN behandling_id SET NOT NULL;

ALTER TABLE fakta_feilutbetaling
    ALTER COLUMN aktiv SET NOT NULL;

ALTER TABLE fakta_feilutbetalingsperiode
    ALTER COLUMN hendelsestype DROP NOT NULL;

ALTER TABLE fakta_feilutbetalingsperiode
    ALTER COLUMN hendelsesundertype DROP NOT NULL;

DROP TABLE gruppering_fakta_feilutbetaling;

ALTER TABLE fagsystemsbehandling
    ADD COLUMN revurderingsvedtaksdato DATE;

COMMENT ON COLUMN fagsystemsbehandling.revurderingsvedtaksdato
    IS 'vedtaksdato av fagsystemsrevurdering';

ALTER TABLE fagsystemsbehandling
    ALTER COLUMN revurderingsvedtaksdato SET NOT NULL;

ALTER TABLE varsel
    DROP COLUMN revurderingsvedtaksdato;
