alter table PO_PERSON add column foedselsdato TIMESTAMP(3) default CURRENT_TIMESTAMP;

alter table BEHANDLING_VEDTAK add column status varchar(50) default 'OPPRETTET';
