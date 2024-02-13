update avtale_innhold set arena_migrering_deltaker = false where godkjent_pa_vegne_av = true;
update avtale_innhold set arena_migrering_arbeidsgiver = false where godkjent_pa_vegne_av_arbeidsgiver = true;
