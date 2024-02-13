alter table avtale_innhold add column otp_sats decimal;

update avtale_innhold set otp_sats = 0.02
where avtale
in (select id from avtale a where a.tiltakstype = 'MIDLERTIDIG_LONNSTILSKUDD' OR a.tiltakstype = 'VARIG_LONNSTILSKUDD' )
 and otp_sats is null;