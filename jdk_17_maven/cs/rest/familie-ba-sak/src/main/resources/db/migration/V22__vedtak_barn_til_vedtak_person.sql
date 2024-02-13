alter table VEDTAK_BARN rename to VEDTAK_PERSON;

alter table VEDTAK_PERSON add column type varchar(50) default 'ORDINÆR_BARNETRYGD';

alter sequence VEDTAK_BARN_SEQ RENAME TO VEDTAK_PERSON_SEQ;

