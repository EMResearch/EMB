# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for kompetanser

  Bakgrunn:
    Gitt følgende behandling
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 13.04.2020  |

  Scenario: Skal gi innvilget primærland begrunnelse basert på kompetanse
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | BOR_MED_SØKER, GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 30.04.2021 | 1054  | 1            |
      | 3456    | 01.05.2021 | 31.03.2038 | 1354  | 1            |

    Og med kompetanser for begrunnelse
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Annen forelders aktivitet | Barnets bostedsland |
      | 3456    | 01.05.2020 | 30.04.2021 | NORGE_ER_PRIMÆRLAND   | 1            | IKKE_AKTUELT              | NO                  |
      | 3456    | 01.05.2021 | 31.03.2038 | NORGE_ER_SEKUNDÆRLAND | 1            | I_ARBEID                  | PL                  |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Inkluderte Begrunnelser | Inkluderte Begrunnelser                            | Ekskluderte Begrunnelser                              |
      | 01.05.2020 | 30.04.2021 | Utbetaling         | EØS_FORORDNINGEN                  | INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE | INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_JOBBER_I_NORGE    |
      | 01.05.2021 | 31.03.2038 | Utbetaling         | EØS_FORORDNINGEN                  | INNVILGET_SEKUNDÆRLAND_STANDARD                    | INNVILGET_SEKUNDÆRLAND_TO_ARBEIDSLAND_NORGE_UTBETALER |
      | 01.04.2038 |            | Opphør             |                                   |                                                    |                                                       |

  Scenario: Ikke vis kompetansebegrunnelser dersom kompetansen ikke endrer seg
    Gitt følgende behandling
      | BehandlingId | FagsakId  | ForrigeBehandlingId |
      | 100173206    | 200055603 |                     |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId       | Persontype | Fødselsdato |
      | 100173206    | 2013549321777 | BARN       | 02.02.2015  |
      | 100173206    | 1448019142841 | SØKER      | 30.09.1984  |

    Og lag personresultater for begrunnelse for behandling 100173206

    Og legg til nye vilkårresultater for begrunnelse for behandling 100173206
      | AktørId       | Vilkår                          | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1448019142841 | LOVLIG_OPPHOLD                  |                              | 30.09.1984 |            | OPPFYLT  | Nei                  |
      | 1448019142841 | BOSATT_I_RIKET                  | OMFATTET_AV_NORSK_LOVGIVNING | 15.03.2023 |            | OPPFYLT  | Nei                  |

      | 2013549321777 | BOR_MED_SØKER                   | BARN_BOR_I_EØS_MED_SØKER     | 02.02.2015 |            | OPPFYLT  | Nei                  |
      | 2013549321777 | LOVLIG_OPPHOLD,GIFT_PARTNERSKAP |                              | 02.02.2015 |            | OPPFYLT  | Nei                  |
      | 2013549321777 | UNDER_18_ÅR                     |                              | 02.02.2015 | 01.02.2033 | OPPFYLT  | Nei                  |
      | 2013549321777 | BOSATT_I_RIKET                  | BARN_BOR_I_NORGE             | 02.02.2015 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId       | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 2013549321777 | 100173206    | 01.04.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     |
      | 2013549321777 | 100173206    | 01.07.2023 | 31.07.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     |
      | 2013549321777 | 100173206    | 01.08.2023 | 31.01.2033 | 167   | ORDINÆR_BARNETRYGD | 100     |

    Og med kompetanser for begrunnelse
      | AktørId       | Fra dato   | Til dato   | Resultat              | BehandlingId | Annen forelders aktivitet | Barnets bostedsland |
      | 2013549321777 | 01.04.2023 | 01.07.2023 | NORGE_ER_PRIMÆRLAND   | 100173206    | INAKTIV                   | NO                  |
      | 2013549321777 | 01.08.2023 |            | NORGE_ER_SEKUNDÆRLAND | 100173206    | I_ARBEID                  | GB                  |

    Når begrunnelsetekster genereres for behandling 100173206

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Inkluderte Begrunnelser | Inkluderte Begrunnelser | Ekskluderte Begrunnelser      |
      | 01.04.2023 | 30.06.2023 | UTBETALING         |                                   |                         |                               |
      | 01.07.2023 | 31.07.2023 | UTBETALING         | EØS_FORORDNINGEN                  |                         | INNVILGET_PRIMÆRLAND_STANDARD |
      | 01.08.2023 | 31.01.2033 | UTBETALING         |                                   |                         |                               |
      | 01.02.2033 |            | OPPHØR             |                                   |                         |                               |

  Scenario: Skal gi riktig begrunnelse ved opphør av EØS-sak
    Gitt følgende behandling
      | BehandlingId | FagsakId  | ForrigeBehandlingId |
      | 100173207    | 200055651 |                     |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId       | Persontype | Fødselsdato |
      | 100173207    | 2005858678161 | BARN       | 02.02.2015  |
      | 100173207    | 2305793738737 | SØKER      | 12.11.1984  |

    Og lag personresultater for begrunnelse for behandling 100173207

    Og legg til nye vilkårresultater for begrunnelse for behandling 100173207
      | AktørId       | Vilkår                          | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2005858678161 | LOVLIG_OPPHOLD,GIFT_PARTNERSKAP |                              | 02.02.2015 |            | OPPFYLT  | Nei                  |
      | 2005858678161 | BOSATT_I_RIKET                  | BARN_BOR_I_NORGE             | 02.02.2015 |            | OPPFYLT  | Nei                  |
      | 2005858678161 | BOR_MED_SØKER                   | BARN_BOR_I_EØS_MED_SØKER     | 02.02.2015 |            | OPPFYLT  | Nei                  |
      | 2005858678161 | UNDER_18_ÅR                     |                              | 02.02.2015 | 01.02.2033 | OPPFYLT  | Nei                  |

      | 2305793738737 | LOVLIG_OPPHOLD                  |                              | 12.11.1984 |            | OPPFYLT  | Nei                  |
      | 2305793738737 | BOSATT_I_RIKET                  | OMFATTET_AV_NORSK_LOVGIVNING | 15.03.2023 | 15.08.2023 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId       | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 2005858678161 | 100173207    | 01.04.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     |
      | 2005858678161 | 100173207    | 01.07.2023 | 31.08.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     |

    Og med kompetanser for begrunnelse
      | AktørId       | Fra dato   | Til dato   | Resultat            | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2005858678161 | 01.04.2023 | 31.08.2023 | NORGE_ER_PRIMÆRLAND | 100173207    | ARBEIDER         | MOTTAR_PENSJON            | NO                    | BE                             | NO                  |

    Når begrunnelsetekster genereres for behandling 100173207

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Inkluderte Begrunnelser | Inkluderte Begrunnelser            | Ekskluderte Begrunnelser |
      | 01.04.2023 | 30.06.2023 | UTBETALING         |                                   |                                    |                          |
      | 01.07.2023 | 31.08.2023 | UTBETALING         |                                   |                                    |                          |
      | 01.09.2023 |            | OPPHØR             | EØS_FORORDNINGEN                  | OPPHØR_IKKE_STATSBORGER_I_EØS_LAND |                          |

  Scenario: Skal begrunne endring i kompetanse når det ikke er noen endringer i resten av behandlingen
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId |
      | 1            | 1        |                     |
      | 2            | 1        | 1                   |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | BARN       | 02.02.2015  |
      | 1            | 5678    | SØKER      | 10.05.1985  |
      | 2            | 1234    | BARN       | 02.02.2015  |
      | 2            | 5678    | SØKER      | 10.05.1985  |

    Og følgende dagens dato 2023-09-12
    Og lag personresultater for begrunnelse for behandling 1
    Og lag personresultater for begrunnelse for behandling 2

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                          | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 5678    | LOVLIG_OPPHOLD                  |                              | 10.05.1985 |            | OPPFYLT  | Nei                  |
      | 5678    | BOSATT_I_RIKET                  | OMFATTET_AV_NORSK_LOVGIVNING | 15.02.2021 |            | OPPFYLT  | Nei                  |

      | 1234    | BOR_MED_SØKER                   | BARN_BOR_I_EØS_MED_SØKER     | 02.02.2015 |            | OPPFYLT  | Nei                  |
      | 1234    | BOSATT_I_RIKET                  | BARN_BOR_I_NORGE             | 02.02.2015 |            | OPPFYLT  | Nei                  |
      | 1234    | UNDER_18_ÅR                     |                              | 02.02.2015 | 01.02.2033 | OPPFYLT  | Nei                  |
      | 1234    | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD |                              | 02.02.2015 |            | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for begrunnelse for behandling 2
      | AktørId | Vilkår                          | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD |                              | 02.02.2015 |            | OPPFYLT  | Nei                  |
      | 1234    | BOSATT_I_RIKET                  | BARN_BOR_I_NORGE             | 02.02.2015 |            | OPPFYLT  | Nei                  |
      | 1234    | UNDER_18_ÅR                     |                              | 02.02.2015 | 01.02.2033 | OPPFYLT  | Nei                  |
      | 1234    | BOR_MED_SØKER                   | BARN_BOR_I_EØS_MED_SØKER     | 02.02.2015 |            | OPPFYLT  | Nei                  |

      | 5678    | LOVLIG_OPPHOLD                  |                              | 10.05.1985 |            | OPPFYLT  | Nei                  |
      | 5678    | BOSATT_I_RIKET                  | OMFATTET_AV_NORSK_LOVGIVNING | 15.02.2021 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 1234    | 1            | 01.03.2021 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     |
      | 1234    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     |
      | 1234    | 1            | 01.07.2023 | 31.01.2033 | 1310  | ORDINÆR_BARNETRYGD | 100     |
      | 1234    | 2            | 01.03.2021 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     |
      | 1234    | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     |
      | 1234    | 2            | 01.07.2023 | 31.01.2033 | 1310  | ORDINÆR_BARNETRYGD | 100     |

    Og med kompetanser for begrunnelse
      | AktørId | Fra dato   | Til dato   | Resultat            | BehandlingId | Søkers aktivitet  | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 1234    | 01.03.2021 |            | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER          | INAKTIV                   | NO                    | BE                             | NO                  |
      | 1234    | 01.03.2021 | 30.04.2023 | NORGE_ER_PRIMÆRLAND | 2            | ARBEIDER          | INAKTIV                   | NO                    | BE                             | NO                  |
      | 1234    | 01.05.2023 |            | NORGE_ER_PRIMÆRLAND | 2            | MOTTAR_UFØRETRYGD | INAKTIV                   | NO                    | EE                             | NO                  |

    Når begrunnelsetekster genereres for behandling 2

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Inkluderte Begrunnelser | Inkluderte Begrunnelser                 | Ekskluderte Begrunnelser                                                                       |
      | 01.05.2023 | 30.06.2023 | UTBETALING         | EØS_FORORDNINGEN                  | INNVILGET_PRIMÆRLAND_BARNET_BOR_I_NORGE | REDUKSJON_BARN_DØD_EØS, REDUKSJON_IKKE_ANSVAR_FOR_BARN, FORTSATT_INNVILGET_PRIMÆRLAND_STANDARD |
      | 01.07.2023 | 31.01.2033 | UTBETALING         |                                   |                                         |                                                                                                |
      | 01.02.2033 |            | OPPHØR             |                                   |                                         |                                                                                                |

