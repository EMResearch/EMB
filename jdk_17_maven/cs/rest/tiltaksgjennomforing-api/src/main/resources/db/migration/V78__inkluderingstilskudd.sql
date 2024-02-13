CREATE TABLE inkluderingstilskuddsutgift
(
    id uuid primary key,
    avtale_innhold_id uuid references avtale_innhold(id),
    beløp integer,
    type varchar,
    tidspunkt_lagt_til timestamp without time zone not null default now()
);

alter table avtale_innhold add column inkluderingstilskudd_begrunnelse varchar;