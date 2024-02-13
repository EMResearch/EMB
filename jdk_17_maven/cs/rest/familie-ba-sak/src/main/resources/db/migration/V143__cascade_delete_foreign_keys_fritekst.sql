ALTER TABLE vedtaksbegrunnelse_fritekst
    DROP CONSTRAINT vedtaksbegrunnelse_fritekst_fk_vedtaksperiode_id_fkey,
    ADD CONSTRAINT vedtaksbegrunnelse_fritekst_fk_vedtaksperiode_id_fkey
        FOREIGN KEY (fk_vedtaksperiode_id)
            REFERENCES vedtaksperiode (id)
            ON DELETE CASCADE;