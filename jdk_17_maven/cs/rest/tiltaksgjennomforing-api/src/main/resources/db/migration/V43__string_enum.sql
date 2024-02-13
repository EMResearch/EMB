update varslbar_hendelse
set varslbar_hendelse_type = (case varslbar_hendelse_type
                                  when '0' then 'OPPRETTET'
                                  when '1' then 'GODKJENT_AV_ARBEIDSGIVER'
                                  when '2' then 'GODKJENT_AV_VEILEDER'
                                  when '3' then 'GODKJENT_AV_DELTAKER'
                                  when '4' then 'GODKJENT_PAA_VEGNE_AV'
                                  when '5' then 'GODKJENNINGER_OPPHEVET_AV_ARBEIDSGIVER'
                                  when '6' then 'GODKJENNINGER_OPPHEVET_AV_VEILEDER'
                                  when '7' then 'SMS_VARSLING_FEILET'
                                  when '8' then 'ENDRET'
                                  when '9' then 'DELT_MED_DELTAKER'
                                  when '10' then 'DELT_MED_ARBEIDSGIVER'
                                  when '11' then 'AVBRUTT'
                                  when '12' then 'LÅST_OPP'
                                  when '13' then 'GJENOPPRETTET'
                                  when '14' then 'OPPRETTET_AV_ARBEIDSGIVER'
                                  when '15' then 'NY_VEILEDER'
                                  when '16' then 'AVTALE_FORDELT'
                                  when '17' then 'TILSKUDDSPERIODE_AVSLATT'
                                  when '18' then 'TILSKUDDSPERIODE_GODKJENT'

                                  else varslbar_hendelse_type end);

update sms_varsel
set status = (case status
                                  when '0' then 'USENDT'
                                  when '1' then 'SENDT'
                                  when '2' then 'FEIL'
                                  else status end);