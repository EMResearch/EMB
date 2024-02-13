ALTER TABLE ANDEL_TIL_ENDRET_ANDEL
    DROP CONSTRAINT andel_til_endret_andel_fk_andel_tilkjent_ytelse_id_fkey,
    ADD CONSTRAINT andel_til_endret_andel_fk_andel_tilkjent_ytelse_id_fkey
        FOREIGN KEY (FK_ANDEL_TILKJENT_YTELSE_ID)
            REFERENCES andel_tilkjent_ytelse (id)
            ON DELETE CASCADE



