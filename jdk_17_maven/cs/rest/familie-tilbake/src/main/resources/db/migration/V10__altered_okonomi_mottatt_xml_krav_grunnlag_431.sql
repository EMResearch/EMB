DROP TABLE gruppering_kravvedtaksstatus;
DROP TABLE kravvedtaksstatus437;

ALTER TABLE okonomi_xml_mottatt
    ADD COLUMN sperret BOOLEAN DEFAULT FALSE NOT NULL;

COMMENT ON COLUMN okonomi_xml_mottatt.sperret
    IS 'Angir om grunnlaget har fått sper melding fra økonomi';

ALTER TABLE kravgrunnlag431
    ADD COLUMN avsluttet BOOLEAN DEFAULT FALSE NOT NULL;

COMMENT ON COLUMN kravgrunnlag431.avsluttet
    IS 'Angir om grunnlaget har fått avsl melding fra økonomi';

ALTER TABLE okonomi_xml_mottatt_arkiv
    ADD COLUMN ekstern_fagsak_id VARCHAR NOT NULL;

COMMENT ON COLUMN okonomi_xml_mottatt_arkiv.ekstern_fagsak_id
    IS 'Saksnummer(som økonomi har sendt)';

ALTER TABLE okonomi_xml_mottatt_arkiv
    ADD COLUMN ytelsestype VARCHAR NOT NULL;

COMMENT ON COLUMN okonomi_xml_mottatt_arkiv.ytelsestype
    IS 'Angir tilhørende ytelsestype';
