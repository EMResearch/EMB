UPDATE BEHANDLING
SET STATUS = 'UTREDES'
WHERE STATUS = 'OPPRETTET';

UPDATE BEHANDLING
SET STATUS = 'UTREDES'
WHERE STATUS = 'UNDERKJENT_AV_BESLUTTER';

UPDATE BEHANDLING
SET STATUS = 'FATTER_VEDTAK'
WHERE STATUS = 'SENDT_TIL_BESLUTTER';

UPDATE BEHANDLING
SET STATUS = 'IVERKSETTER_VEDTAK'
WHERE STATUS = 'GODKJENT';

UPDATE BEHANDLING
SET STATUS = 'IVERKSETTER_VEDTAK'
WHERE STATUS = 'SENDT_TIL_IVERKSETTING';

UPDATE BEHANDLING
SET STATUS = 'IVERKSETTER_VEDTAK'
WHERE STATUS = 'IVERKSATT';

UPDATE BEHANDLING
SET STATUS = 'AVSLUTTET'
WHERE STATUS = 'FERDIGSTILT';

UPDATE FAGSAK
SET STATUS = 'AVSLUTTET'
WHERE STATUS = 'STANSET';