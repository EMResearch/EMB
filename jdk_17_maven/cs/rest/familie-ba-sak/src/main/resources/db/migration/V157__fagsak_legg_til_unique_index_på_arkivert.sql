CREATE UNIQUE INDEX uidx_fagsak_person_ident_ikke_arkivert ON fagsak_person(ident)
    WHERE arkivert = false;
