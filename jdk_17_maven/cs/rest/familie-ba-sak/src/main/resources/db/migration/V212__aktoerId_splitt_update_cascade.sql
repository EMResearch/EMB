-- personident
ALTER TABLE personident DROP CONSTRAINT fk_personident;

ALTER TABLE personident
    ADD CONSTRAINT fk_personident
        FOREIGN KEY (fk_aktoer_id) references aktoer ON UPDATE CASCADE;

-- person_resultat
ALTER TABLE person_resultat DROP CONSTRAINT fk_person_resultat;

ALTER TABLE person_resultat
    ADD CONSTRAINT fk_person_resultat
        FOREIGN KEY (fk_aktoer_id) references aktoer ON UPDATE CASCADE;

-- po_person
ALTER TABLE po_person DROP CONSTRAINT fk_po_person;

ALTER TABLE po_person
    ADD CONSTRAINT fk_po_person
        FOREIGN KEY (fk_aktoer_id) references aktoer ON UPDATE CASCADE;

-- fagsak
ALTER TABLE fagsak DROP CONSTRAINT fagsak;

ALTER TABLE fagsak
    ADD CONSTRAINT fagsak
        FOREIGN KEY (fk_aktoer_id) references aktoer ON UPDATE CASCADE;

-- andel_tilkjent_ytelse
ALTER TABLE andel_tilkjent_ytelse DROP CONSTRAINT fk_andel_tilkjent_ytelse;

ALTER TABLE andel_tilkjent_ytelse
    ADD CONSTRAINT fk_andel_tilkjent_ytelse
        FOREIGN KEY (fk_aktoer_id) references aktoer ON UPDATE CASCADE;

-- foedselshendelse_pre_lansering
ALTER TABLE foedselshendelse_pre_lansering DROP CONSTRAINT fk_foedselshendelse_pre_lansering;

ALTER TABLE foedselshendelse_pre_lansering
    ADD CONSTRAINT fk_foedselshendelse_pre_lansering
        FOREIGN KEY (fk_aktoer_id) references aktoer ON UPDATE CASCADE;

-- gr_periode_overgangsstonad
ALTER TABLE gr_periode_overgangsstonad DROP CONSTRAINT fk_gr_periode_overgangsstonad;

ALTER TABLE gr_periode_overgangsstonad
    ADD CONSTRAINT fk_gr_periode_overgangsstonad
        FOREIGN KEY (fk_aktoer_id) references aktoer ON UPDATE CASCADE;

-- aktoer_til_kompetanse
ALTER TABLE aktoer_til_kompetanse DROP CONSTRAINT aktoer_til_kompetanse_fk_aktoer_id_fkey;

ALTER TABLE aktoer_til_kompetanse
    ADD CONSTRAINT aktoer_til_kompetanse_fk_aktoer_id_fkey
        FOREIGN KEY (fk_aktoer_id) references aktoer ON UPDATE CASCADE;

-- aktoer_til_utenlandsk_periodebeloep
ALTER TABLE aktoer_til_utenlandsk_periodebeloep DROP CONSTRAINT aktoer_til_utenlandsk_periodebeloep_fk_aktoer_id_fkey;

ALTER TABLE aktoer_til_utenlandsk_periodebeloep
    ADD CONSTRAINT aktoer_til_utenlandsk_periodebeloep_fk_aktoer_id_fkey
        FOREIGN KEY (fk_aktoer_id) references aktoer ON UPDATE CASCADE;

-- aktoer_til_valutakurs
ALTER TABLE aktoer_til_valutakurs DROP CONSTRAINT aktoer_til_valutakurs_fk_aktoer_id_fkey;

ALTER TABLE aktoer_til_valutakurs
    ADD CONSTRAINT aktoer_til_valutakurs_fk_aktoer_id_fkey
        FOREIGN KEY (fk_aktoer_id) references aktoer ON UPDATE CASCADE;
