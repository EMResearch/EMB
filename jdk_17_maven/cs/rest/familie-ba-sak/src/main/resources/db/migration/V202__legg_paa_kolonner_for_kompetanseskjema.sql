ALTER TABLE kompetanse
    ADD COLUMN soekers_aktivitet VARCHAR DEFAULT null,
    ADD COLUMN annen_forelderes_aktivitet       VARCHAR DEFAULT null,
    ADD COLUMN annen_forelderes_aktivitetsland  VARCHAR DEFAULT null,
    ADD COLUMN barnets_bostedsland              VARCHAR DEFAULT null,
    ADD COLUMN resultat                         VARCHAR DEFAULT null;



