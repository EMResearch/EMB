drop table FAGSAK_PERSON;

alter table ANDEL_TILKJENT_YTELSE
    drop column PERSON_IDENT;

alter table PERSON_RESULTAT
    drop column PERSON_IDENT;

alter table GR_PERIODE_OVERGANGSSTONAD
    drop column PERSON_IDENT;

alter table FOEDSELSHENDELSE_PRE_LANSERING
    drop column PERSON_IDENT;

alter table PO_PERSON
    drop column PERSON_IDENT;