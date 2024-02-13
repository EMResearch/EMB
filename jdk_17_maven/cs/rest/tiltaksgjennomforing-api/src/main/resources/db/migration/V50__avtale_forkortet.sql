create table avtale_forkortet(
    id uuid primary key,
    avtale_id uuid references avtale(id),
    avtale_innhold_id uuid,
    tidspunkt timestamp,
    ny_slutt_dato date,
    grunn varchar,
    annet_grunn varchar
);