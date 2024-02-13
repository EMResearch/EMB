update avtale set godkjent_for_etterregistrering = false;
alter table avtale alter column godkjent_for_etterregistrering set default false;