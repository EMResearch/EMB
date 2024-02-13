alter table avtale_innhold drop column arena_migrering_deltaker;
alter table avtale_innhold drop column arena_migrering_arbeidsgiver;
alter table avtale_innhold add column arena_migrering_deltaker boolean;
alter table avtale_innhold add column arena_migrering_arbeidsgiver boolean;