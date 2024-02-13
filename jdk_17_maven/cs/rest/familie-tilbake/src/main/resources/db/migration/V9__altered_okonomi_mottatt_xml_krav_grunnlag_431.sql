ALTER TABLE okonomi_xml_mottatt
    DROP COLUMN sekvens;

ALTER TABLE okonomi_xml_mottatt
    DROP COLUMN tilkoblet;

ALTER TABLE okonomi_xml_mottatt
    ADD COLUMN kravstatuskode VARCHAR NOT NULL;

COMMENT ON COLUMN okonomi_xml_mottatt.kravstatuskode
    IS 'Angir mottatt xmls kravstatus';

ALTER TABLE okonomi_xml_mottatt
    ADD COLUMN ytelsestype VARCHAR NOT NULL;

COMMENT ON COLUMN okonomi_xml_mottatt.ytelsestype
    IS 'Angir tilhørende ytelsestype';

ALTER TABLE okonomi_xml_mottatt
    RENAME COLUMN henvisning TO referanse;

COMMENT ON COLUMN okonomi_xml_mottatt.referanse
    IS 'Peker på referanse-feltet i kravgrunnlaget, og kommer opprinnelig fra fagsystemet';

ALTER TABLE okonomi_xml_mottatt
    ALTER COLUMN referanse SET NOT NULL;

ALTER TABLE okonomi_xml_mottatt
    ADD COLUMN kontrollfelt VARCHAR;

COMMENT ON COLUMN okonomi_xml_mottatt.kontrollfelt
    IS 'Brukes ved innsending av tilbakekrevingsvedtak for å kontrollere at kravgrunnlaget ikke er blitt endret i mellomtiden';

ALTER TABLE okonomi_xml_mottatt
    ADD COLUMN ekstern_kravgrunnlag_id BIGINT;

COMMENT ON COLUMN okonomi_xml_mottatt.ekstern_kravgrunnlag_id
    IS 'Referanse til kravgrunnlag fra ostbk. Brukes ved omgjøring for å hente nytt grunnlag';

ALTER TABLE okonomi_xml_mottatt
    ADD COLUMN vedtak_id BIGINT NOT NULL;

COMMENT ON COLUMN okonomi_xml_mottatt.vedtak_id
    IS 'Identifikasjon av tilbakekrevingsvedtaket opprettet av tilbakekrevingskomponenten';

ALTER TABLE okonomi_xml_mottatt
    ALTER COLUMN ekstern_fagsak_id SET NOT NULL;

ALTER TABLE kravgrunnlag431
    ALTER COLUMN vedtak_id TYPE BIGINT USING vedtak_id::BIGINT;

ALTER TABLE kravgrunnlag431
    ALTER COLUMN omgjort_vedtak_id TYPE BIGINT USING omgjort_vedtak_id::BIGINT;

ALTER TABLE kravgrunnlag431
    ALTER COLUMN ekstern_kravgrunnlag_id TYPE BIGINT USING ekstern_kravgrunnlag_id::BIGINT;

ALTER TABLE kravgrunnlag431
    ALTER COLUMN ekstern_kravgrunnlag_id SET NOT NULL;

