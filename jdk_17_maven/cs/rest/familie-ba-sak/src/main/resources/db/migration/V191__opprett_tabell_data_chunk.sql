create TABLE data_chunk
(
    id                      BIGINT                                      PRIMARY KEY,
    fk_batch_id             bigint       REFERENCES BATCH (id)          NOT NULL,
    transaksjons_id         UUID                                        NOT NULL,
    chunk_nr                BIGINT                                      NOT NULL,
    er_sendt                BOOLEAN                                     NOT NULL,
    versjon                 BIGINT       DEFAULT 0                      NOT NULL,
    opprettet_av            VARCHAR      DEFAULT 'VL'                   NOT NULL,
    opprettet_tid           TIMESTAMP(3) DEFAULT localtimestamp         NOT NULL,
    endret_av               VARCHAR,
    endret_tid              TIMESTAMP(3)
);

create sequence data_chunk_seq increment by 50 start with 1000000 NO CYCLE;

create INDEX data_chunk_transaksjons_id_chunk_nr_idx ON data_chunk(transaksjons_id, chunk_nr);
create INDEX data_chunk_transaksjons_id_idx ON data_chunk(transaksjons_id);