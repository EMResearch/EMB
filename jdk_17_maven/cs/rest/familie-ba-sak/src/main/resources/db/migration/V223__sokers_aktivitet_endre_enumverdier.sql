UPDATE kompetanse
SET sokers_aktivitetsland ='NO'
WHERE soekers_aktivitet = 'ARBEIDER_I_NORGE'
  and sokers_aktivitetsland is Null;

UPDATE kompetanse
SET sokers_aktivitetsland ='NO'
WHERE soekers_aktivitet = 'SELVSTENDIG_NÆRINGSDRIVENDE'
  and sokers_aktivitetsland is Null;

UPDATE kompetanse
SET sokers_aktivitetsland ='NO'
WHERE soekers_aktivitet = 'MOTTAR_UTBETALING_FRA_NAV_SOM_ERSTATTER_LØNN'
  and sokers_aktivitetsland is Null;

UPDATE kompetanse
SET sokers_aktivitetsland ='NO'
WHERE soekers_aktivitet = 'MOTTAR_UFØRETRYGD_FRA_NORGE'
  and sokers_aktivitetsland is Null;

UPDATE kompetanse
SET sokers_aktivitetsland ='NO'
WHERE soekers_aktivitet = 'MOTTAR_PENSJON_FRA_NORGE'
  and sokers_aktivitetsland is Null;

UPDATE kompetanse
SET soekers_aktivitet='ARBEIDER'
WHERE soekers_aktivitet = 'ARBEIDER_I_NORGE';

UPDATE kompetanse
SET soekers_aktivitet='MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN'
WHERE soekers_aktivitet = 'MOTTAR_UTBETALING_FRA_NAV_SOM_ERSTATTER_LØNN';

UPDATE kompetanse
SET soekers_aktivitet='MOTTAR_UFØRETRYGD'
WHERE soekers_aktivitet = 'MOTTAR_UFØRETRYGD_FRA_NORGE';

UPDATE kompetanse
SET soekers_aktivitet='MOTTAR_PENSJON'
WHERE soekers_aktivitet = 'MOTTAR_PENSJON_FRA_NORGE';