INSERT INTO vedtaksperiode (id, fk_vedtak_id, fom, tom, type)
    (SELECT NEXTVAL('vedtaksperiode_seq'), fk_vedtak_id, fom, tom,
        CASE split_part(begrunnelse,'_', 1)
        WHEN 'INNVILGET' THEN 'UTBETALING'
        WHEN 'REDUKSJON' THEN 'UTBETALING'
        WHEN 'AVSLAG' THEN 'AVSLAG'
        WHEN 'OPPHØR' THEN 'OPPHØR'
        WHEN 'FORTSATT' THEN 'FORTSATT_INNVILGET'
        END AS type
    FROM vedtak_begrunnelse vb
    INNER JOIN vedtak v
    ON vb.fk_vedtak_id = v.id
    INNER JOIN behandling b
    ON v.fk_behandling_id = b.id
    WHERE NOT EXISTS(
            SELECT id
            FROM vedtaksperiode v
            WHERE v.fk_vedtak_id = vb.fk_vedtak_id AND
                ((v.fom IS NULL AND vb.fom IS NULL) OR v.fom = vb.fom) AND
                ((v.tom IS NULL AND vb.tom IS NULL) OR v.tom = vb.tom) AND
                v.type = v.type)
        AND b.status != 'FATTER_VEDTAK'
        AND b.resultat != 'FORTSATT_INNVILGET'
    GROUP BY fk_vedtak_id, fom, tom, type);

INSERT INTO vedtaksbegrunnelse(id, fk_vedtaksperiode_id, vedtak_begrunnelse_spesifikasjon)
    SELECT nextval('vedtaksbegrunnelse_seq'), vp.id, begrunnelse
    FROM vedtak_begrunnelse vb
    INNER JOIN vedtaksperiode vp
        ON vp.fk_vedtak_id = vb.fk_vedtak_id AND
           ((vp.fom IS NULL AND vb.fom IS NULL) OR vp.fom = vb.fom) AND
           ((vp.tom IS NULL AND vb.tom IS NULL) OR vp.tom = vb.tom) AND
           vp.type = CASE split_part(vb.begrunnelse,'_', 1)
                        WHEN 'INNVILGET' THEN 'UTBETALING'
                        WHEN 'REDUKSJON' THEN 'UTBETALING'
                        WHEN 'AVSLAG' THEN 'AVSLAG'
                        WHEN 'OPPHØR' THEN 'OPPHØR'
                        WHEN 'FORTSATT' THEN 'FORTSATT_INNVILGET'
                     END
    WHERE NOT EXISTS(
            SELECT id
            FROM vedtaksbegrunnelse vbs
            WHERE vbs.fk_vedtaksperiode_id = vp.id
                AND vbs.vedtak_begrunnelse_spesifikasjon = vb.begrunnelse)
        AND vb.begrunnelse NOT LIKE '%FRITEKST%'
    GROUP BY vp.id, begrunnelse;

INSERT INTO vedtaksbegrunnelse_fritekst(id, fk_vedtaksperiode_id, fritekst)
    SELECT nextval('vedtaksbegrunnelse_fritekst_seq'), vp.id, brev_begrunnelse
    FROM vedtak_begrunnelse vb
    INNER JOIN vedtaksperiode vp
        ON vp.fk_vedtak_id = vb.fk_vedtak_id AND
           ((vp.fom IS NULL AND vb.fom IS NULL) OR vp.fom = vb.fom) AND
           ((vp.tom IS NULL AND vb.tom IS NULL) OR vp.tom = vb.tom) AND
           vp.type = CASE split_part(vb.begrunnelse,'_', 1)
                        WHEN 'INNVILGET' THEN 'UTBETALING'
                        WHEN 'REDUKSJON' THEN 'UTBETALING'
                        WHEN 'AVSLAG' THEN 'AVSLAG'
                        WHEN 'OPPHØR' THEN 'OPPHØR'
                        WHEN 'FORTSATT' THEN 'FORTSATT_INNVILGET'
                     END
    WHERE NOT EXISTS(
            SELECT id
            FROM vedtaksbegrunnelse_fritekst vbfs
            WHERE vbfs.fk_vedtaksperiode_id = vp.id
                AND vbfs.fritekst = vb.brev_begrunnelse)
        AND vb.brev_begrunnelse IS NOT NULL
        AND vb.begrunnelse LIKE '%FRITEKST%';