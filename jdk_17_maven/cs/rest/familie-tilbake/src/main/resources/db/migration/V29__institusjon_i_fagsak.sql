
ALTER TABLE fagsak
    ADD COLUMN institusjon_organisasjonsnummer VARCHAR;

COMMENT ON COLUMN fagsak.institusjon_organisasjonsnummer
    IS 'Organisasjonsnummer for institusjon';

ALTER TABLE fagsak
    ADD COLUMN institusjon_navn VARCHAR;

COMMENT ON COLUMN fagsak.institusjon_navn
    IS 'Navn på intitusjon';
