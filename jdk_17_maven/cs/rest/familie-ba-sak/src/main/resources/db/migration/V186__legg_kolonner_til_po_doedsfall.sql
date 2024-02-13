ALTER TABLE po_doedsfall
    ADD COLUMN opprettet_av            VARCHAR      DEFAULT 'VL'                   NOT NULL,
    ADD COLUMN opprettet_tid           TIMESTAMP(3) DEFAULT localtimestamp         NOT NULL,
    ADD COLUMN endret_av               VARCHAR,
    ADD COLUMN endret_tid              TIMESTAMP(3);