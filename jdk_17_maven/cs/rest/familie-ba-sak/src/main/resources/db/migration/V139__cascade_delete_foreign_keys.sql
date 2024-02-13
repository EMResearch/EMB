ALTER TABLE vedtaksbegrunnelse
    DROP CONSTRAINT vedtaksbegrunnelse_fk_vedtaksperiode_id_fkey,
    ADD CONSTRAINT vedtaksbegrunnelse_fk_vedtaksperiode_id_fkey
        FOREIGN KEY (fk_vedtaksperiode_id)
            REFERENCES vedtaksperiode (id)
            ON DELETE CASCADE;

ALTER TABLE andel_tilkjent_ytelse
    DROP CONSTRAINT andel_tilkjent_ytelse_tilkjent_ytelse_id_fkey,
    ADD CONSTRAINT andel_tilkjent_ytelse_tilkjent_ytelse_id_fkey
        FOREIGN KEY (tilkjent_ytelse_id)
            REFERENCES tilkjent_ytelse (id)
            ON DELETE CASCADE;

ALTER TABLE okonomi_simulering_postering
    DROP CONSTRAINT vedtak_simulering_postering_fk_vedtak_simulering_mottaker__fkey, -- Er to understreker i fk-navn hentet fra database
    ADD CONSTRAINT vedtak_simulering_postering_fk_vedtak_simulering_mottaker__fkey
        FOREIGN KEY (fk_okonomi_simulering_mottaker_id)
            REFERENCES okonomi_simulering_mottaker (id)
            ON DELETE CASCADE;