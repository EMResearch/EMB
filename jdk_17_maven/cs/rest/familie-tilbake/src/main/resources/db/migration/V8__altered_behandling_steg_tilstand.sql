ALTER TABLE behandlingsstegstilstand
    ADD COLUMN ventearsak VARCHAR;

COMMENT ON COLUMN behandlingsstegstilstand.ventearsak
    IS 'Årsak for at behandling er satt på vent';

ALTER TABLE behandlingsstegstilstand
    ADD COLUMN tidsfrist DATE;

COMMENT ON COLUMN behandlingsstegstilstand.tidsfrist
    IS 'Behandling blir automatisk gjenopptatt etter dette tidspunktet';
