ALTER TABLE andel_tilkjent_ytelse
    ADD COLUMN prosent NUMERIC;
-- Setter
UPDATE andel_tilkjent_ytelse SET prosent = 50 where belop in (677,527,485,827);
UPDATE andel_tilkjent_ytelse SET prosent = 100 where prosent is null;
ALTER TABLE andel_tilkjent_ytelse
    ALTER COLUMN prosent SET NOT NULL;

ALTER TABLE andel_tilkjent_ytelse
    ADD COLUMN sats BIGINT;
UPDATE andel_tilkjent_ytelse SET sats = belop * 2 where belop in (677,527,485,827);
UPDATE andel_tilkjent_ytelse SET sats = belop where sats is null;
ALTER TABLE andel_tilkjent_ytelse
    ALTER COLUMN sats SET NOT NULL;

ALTER TABLE andel_tilkjent_ytelse
    RENAME COLUMN belop TO kalkulert_utbetalingsbelop;

CREATE TABLE ANDEL_TIL_ENDRET_ANDEL
(
    FK_ANDEL_TILKJENT_YTELSE_ID     BIGINT REFERENCES ANDEL_TILKJENT_YTELSE (ID)    NOT NULL,
    FK_ENDRET_UTBETALING_ANDEL_ID   BIGINT REFERENCES ENDRET_UTBETALING_ANDEL (ID)  NOT NULL,
    PRIMARY KEY (FK_ANDEL_TILKJENT_YTELSE_ID, FK_ENDRET_UTBETALING_ANDEL_ID)
);


