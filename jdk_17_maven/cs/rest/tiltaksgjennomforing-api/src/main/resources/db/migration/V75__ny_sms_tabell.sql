create table sms (
    sms_varsel_id uuid primary key,
    telefonnummer varchar,
    identifikator varchar,
    meldingstekst varchar,
    avtale_id uuid references avtale(id),
    tidspunkt timestamp without time zone,
    hendelse_type varchar,
    avsender_applikasjon varchar
);