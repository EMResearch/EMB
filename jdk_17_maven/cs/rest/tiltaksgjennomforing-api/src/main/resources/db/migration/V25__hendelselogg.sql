create table hendelselogg(
    id uuid primary key,
    avtale_id uuid references avtale(id),
    tidspunkt timestamp without time zone not null,
    utført_av varchar,
    hendelse varchar
);