ALTER TABLE okonomi_xml_sendt
    DROP COLUMN IF EXISTS meldingstype;

ALTER TABLE behandlingsvedtak
    DROP COLUMN IF EXISTS ansvarlig_saksbehandler;
