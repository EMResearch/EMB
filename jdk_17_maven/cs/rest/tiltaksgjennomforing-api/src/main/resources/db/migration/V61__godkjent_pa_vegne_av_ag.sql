alter table avtale_innhold add column godkjent_pa_vegne_av_arbeidsgiver boolean;

alter table avtale_innhold add column klarer_ikke_gi_fa_tilgang boolean;
alter table avtale_innhold add column vet_ikke_hvem_som_kan_gi_tilgang boolean;
alter table avtale_innhold add column far_ikke_tilgang_personvern boolean;