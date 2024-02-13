ALTER TABLE avtale RENAME COLUMN godkjent_av_deltaker TO gammel_godkjent_av_deltaker;
ALTER TABLE avtale RENAME COLUMN godkjent_av_arbeidsgiver TO gammel_godkjent_av_arbeidsgiver;
ALTER TABLE avtale RENAME COLUMN godkjent_av_veileder TO gammel_godkjent_av_veileder;


ALTER TABLE avtale RENAME COLUMN dato_godkjent_deltaker TO godkjent_av_deltaker;
ALTER TABLE avtale RENAME COLUMN dato_godkjent_arbeidsgiver TO godkjent_av_arbeidsgiver;
ALTER TABLE avtale RENAME COLUMN dato_godkjent_veileder TO godkjent_av_veileder;

alter table avtale add godkjent_pa_vegne_av boolean default false;

create table godkjent_pa_vegne_grunn (
    avtale uuid primary key references avtale(id),
    ikke_bank_id boolean default false,
    reservert boolean default false,
    digital_kompetanse boolean default false
);