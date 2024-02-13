ALTER TABLE sett_paa_vent
    ADD COLUMN tid_tatt_av_vent  TIMESTAMP(3),
    ADD COLUMN tid_satt_paa_vent TIMESTAMP(3) NOT NULL DEFAULT now();




