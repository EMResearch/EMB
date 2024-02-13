UPDATE vilkar_resultat
SET vedtak_begrunnelse_spesifikasjoner = concat('Standardbegrunnelse$', vedtak_begrunnelse_spesifikasjoner)
WHERE vedtak_begrunnelse_spesifikasjoner <> '';

UPDATE vilkar_resultat
SET vedtak_begrunnelse_spesifikasjoner = replace(vedtak_begrunnelse_spesifikasjoner, ';', ';Standardbegrunnelse$')
WHERE vedtak_begrunnelse_spesifikasjoner like '%;%';

UPDATE endret_utbetaling_andel
SET vedtak_begrunnelse_spesifikasjoner = concat('Standardbegrunnelse$', vedtak_begrunnelse_spesifikasjoner)
WHERE vedtak_begrunnelse_spesifikasjoner <> '';

UPDATE endret_utbetaling_andel
SET vedtak_begrunnelse_spesifikasjoner = replace(vedtak_begrunnelse_spesifikasjoner, ';', ';Standardbegrunnelse$')
WHERE vedtak_begrunnelse_spesifikasjoner like '%;%';
