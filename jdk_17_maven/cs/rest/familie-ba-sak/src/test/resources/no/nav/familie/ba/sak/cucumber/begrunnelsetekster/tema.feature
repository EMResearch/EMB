# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for behandlingstema

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId |
      | 1            | 1        |                     |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 02.01.1985  |
      | 1            | 4567    | BARN       | 07.09.2019  |

  Scenario: Man skal ikke få nasjonale begrunnelser dersom vedtaksperiode overlapper med eøs perioder
    Og følgende dagens dato 2023-09-13
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                          | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | LOVLIG_OPPHOLD                  |                              | 02.01.1985 |            | OPPFYLT  | Nei                  |
      | 1234    | BOSATT_I_RIKET                  | OMFATTET_AV_NORSK_LOVGIVNING | 02.01.1985 |            | OPPFYLT  | Nei                  |

      | 4567    | UNDER_18_ÅR                     |                              | 07.09.2019 | 06.09.2037 | OPPFYLT  | Nei                  |
      | 4567    | LOVLIG_OPPHOLD,GIFT_PARTNERSKAP |                              | 07.09.2019 |            | OPPFYLT  | Nei                  |
      | 4567    | BOSATT_I_RIKET                  | BARN_BOR_I_NORGE             | 07.09.2019 |            | OPPFYLT  | Nei                  |
      | 4567    | BOR_MED_SØKER                   | BARN_BOR_I_EØS_MED_SØKER     | 07.09.2019 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 4567    | 1            | 01.10.2019 | 31.08.2020 | 1054  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.09.2020 | 31.08.2021 | 1354  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.09.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.01.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.07.2023 | 31.08.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.09.2025 | 31.08.2037 | 1310  | ORDINÆR_BARNETRYGD | 100     |

    Og med kompetanser for begrunnelse
      | AktørId | Fra dato   | Til dato | Resultat            | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 4567    | 01.10.2019 |          | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | SE                             | SE                  |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Inkluderte Begrunnelser | Inkluderte Begrunnelser                           | Regelverk Ekskluderte Begrunnelser | Ekskluderte Begrunnelser     |
      | 01.10.2019 | 31.08.2020 | UTBETALING         | EØS_FORORDNINGEN                  | INNVILGET_PRIMÆRLAND_BARNETRYGD_ALLEREDE_UTBETALT | NASJONALE_REGLER                   | INNVILGET_NYFØDT_BARN_FØRSTE |
      | 01.09.2020 | 31.08.2021 | UTBETALING         |                                   |                                                   |                                    |                              |
      | 01.09.2021 | 31.12.2021 | UTBETALING         |                                   |                                                   |                                    |                              |
      | 01.01.2022 | 28.02.2023 | UTBETALING         |                                   |                                                   |                                    |                              |
      | 01.03.2023 | 30.06.2023 | UTBETALING         |                                   |                                                   |                                    |                              |
      | 01.07.2023 | 31.08.2025 | UTBETALING         |                                   |                                                   |                                    |                              |
      | 01.09.2025 | 31.08.2037 | UTBETALING         |                                   |                                                   |                                    |                              |
      | 01.09.2037 |            | OPPHØR             |                                   |                                                   |                                    |                              |

  Scenario: Man skal ikke få eøs begrunnelser dersom vedtaksperiode ikke overlapper med nasjonale perioder
    Og følgende dagens dato 2023-09-13
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                          | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | LOVLIG_OPPHOLD                  |                              | 02.01.1985 |            | OPPFYLT  | Nei                  |
      | 1234    | BOSATT_I_RIKET                  | OMFATTET_AV_NORSK_LOVGIVNING | 02.01.1985 |            | OPPFYLT  | Nei                  |

      | 4567    | UNDER_18_ÅR                     |                              | 07.09.2019 | 06.09.2037 | OPPFYLT  | Nei                  |
      | 4567    | LOVLIG_OPPHOLD,GIFT_PARTNERSKAP |                              | 07.09.2019 |            | OPPFYLT  | Nei                  |
      | 4567    | BOSATT_I_RIKET                  | BARN_BOR_I_NORGE             | 07.09.2019 |            | OPPFYLT  | Nei                  |
      | 4567    | BOR_MED_SØKER                   | BARN_BOR_I_EØS_MED_SØKER     | 07.09.2019 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 4567    | 1            | 01.10.2019 | 31.08.2020 | 1054  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.09.2020 | 31.08.2021 | 1354  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.09.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.01.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.07.2023 | 31.08.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.09.2025 | 31.08.2037 | 1310  | ORDINÆR_BARNETRYGD | 100     |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Inkluderte Begrunnelser | Inkluderte Begrunnelser                  | Regelverk Ekskluderte Begrunnelser | Ekskluderte Begrunnelser                          |
      | 01.10.2019 | 31.08.2020 | UTBETALING         | NASJONALE_REGLER                  | INNVILGET_BOSATT_I_RIKTET_LOVLIG_OPPHOLD | EØS_FORORDNINGEN                   | INNVILGET_PRIMÆRLAND_BARNETRYGD_ALLEREDE_UTBETALT |
      | 01.09.2020 | 31.08.2021 | UTBETALING         |                                   |                                          |                                    |                                                   |
      | 01.09.2021 | 31.12.2021 | UTBETALING         |                                   |                                          |                                    |                                                   |
      | 01.01.2022 | 28.02.2023 | UTBETALING         |                                   |                                          |                                    |                                                   |
      | 01.03.2023 | 30.06.2023 | UTBETALING         |                                   |                                          |                                    |                                                   |
      | 01.07.2023 | 31.08.2025 | UTBETALING         |                                   |                                          |                                    |                                                   |
      | 01.09.2025 | 31.08.2037 | UTBETALING         |                                   |                                          |                                    |                                                   |
      | 01.09.2037 |            | OPPHØR             |                                   |                                          |                                    |                                                   |

  Scenario: Søker skal ikke ha noe nasjonal begrunnelser etter vilkår dersom vilkårene er vurdert etter eøs forordningen
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 16.07.1985  |
      | 1            | 4567    | BARN       | 18.06.2019  |
      | 1            | 5678    | BARN       | 20.12.2014  |

    Og følgende dagens dato 28.09.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Vurderes etter   |
      | 4567    | UNDER_18_ÅR      |                              | 18.06.2019 | 17.06.2037 | OPPFYLT  | Nei                  |                  |
      | 4567    | GIFT_PARTNERSKAP |                              | 18.06.2019 |            | OPPFYLT  | Nei                  |                  |
      | 4567    | LOVLIG_OPPHOLD   |                              | 18.05.2022 |            | OPPFYLT  | Nei                  |                  |
      | 4567    | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 18.05.2022 |            | OPPFYLT  | Nei                  |                  |
      | 4567    | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 18.05.2022 |            | OPPFYLT  | Nei                  |                  |

      | 1234    | LOVLIG_OPPHOLD   |                              | 18.05.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 1234    | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 18.05.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |

      | 5678    | GIFT_PARTNERSKAP |                              | 20.12.2014 |            | OPPFYLT  | Nei                  |                  |
      | 5678    | UNDER_18_ÅR      |                              | 20.12.2014 | 19.12.2032 | OPPFYLT  | Nei                  |                  |
      | 5678    | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 18.05.2022 |            | OPPFYLT  | Nei                  |                  |
      | 5678    | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 18.05.2022 |            | OPPFYLT  | Nei                  |                  |
      | 5678    | LOVLIG_OPPHOLD   |                              | 18.05.2022 |            | OPPFYLT  | Nei                  |                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 4567    | 1            | 01.06.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 4567    | 1            | 01.03.2023 | 31.08.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 4567    | 1            | 01.09.2023 | 31.05.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4567    | 1            | 01.06.2025 | 31.05.2037 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 5678    | 1            | 01.06.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5678    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 5678    | 1            | 01.07.2023 | 30.11.2032 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent |
      | 4567    | 1            | 01.03.2023 | 31.08.2023 | ALLEREDE_UTBETALT | 0       |

    Og med kompetanser for begrunnelse
      | AktørId    | Fra dato   | Til dato   | Resultat            | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 5678, 4567 | 01.09.2023 |            | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | BE                             | BE                  |
      | 5678       | 01.03.2023 | 31.08.2023 | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | BE                             | BE                  |
      | 5678, 4567 | 01.06.2022 | 28.02.2023 | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | BE                             | BE                  |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Inkluderte Begrunnelser | Inkluderte Begrunnelser                    | Regelverk Ekskluderte Begrunnelser | Ekskluderte Begrunnelser              |
      | 01.06.2022 | 28.02.2023 | UTBETALING         | EØS_FORORDNINGEN                  | INNVILGET_PRIMÆRLAND_UK_OG_UTLAND_STANDARD | NASJONALE_REGLER                   | INNVILGET_EØS_BORGER_EKTEFELLE_JOBBER |
      | 01.03.2023 | 30.06.2023 | UTBETALING         |                                   |                                            |                                    |                                       |
      | 01.07.2023 | 31.08.2023 | UTBETALING         |                                   |                                            |                                    |                                       |
      | 01.09.2023 | 31.05.2025 | UTBETALING         |                                   |                                            |                                    |                                       |
      | 01.06.2025 | 30.11.2032 | UTBETALING         |                                   |                                            |                                    |                                       |
      | 01.12.2032 | 31.05.2037 | UTBETALING         |                                   |                                            |                                    |                                       |
      | 01.06.2037 |            | OPPHØR             |                                   |                                            |                                    |                                       |


  Scenario: Man skal kunne få begrunnelsetekster med tema Felles uavhengig om det er EØS eller Nasjonal
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 30.04.1994  |
      | 1            | 4567    | BARN       | 29.04.2015  |


    Og følgende dagens dato 03.10.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår             | Utdypende vilkår             | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 4567    | UNDER_18_ÅR        |                              | 29.04.2015 | 28.04.2033 | OPPFYLT      | Nei                  |
      | 4567    | GIFT_PARTNERSKAP   |                              | 29.04.2015 |            | OPPFYLT      | Nei                  |
      | 4567    | BOR_MED_SØKER      | BARN_BOR_I_NORGE_MED_SØKER   | 25.07.2022 |            | OPPFYLT      | Nei                  |
      | 4567    | LOVLIG_OPPHOLD     |                              | 25.07.2022 |            | OPPFYLT      | Nei                  |
      | 4567    | BOSATT_I_RIKET     | BARN_BOR_I_NORGE             | 25.07.2022 |            | OPPFYLT      | Nei                  |

      | 1234    | LOVLIG_OPPHOLD     |                              | 25.07.2022 |            | OPPFYLT      | Nei                  |
      | 1234    | UTVIDET_BARNETRYGD |                              | 25.07.2022 | 30.05.2023 | OPPFYLT      | Nei                  |
      | 1234    | BOSATT_I_RIKET     | OMFATTET_AV_NORSK_LOVGIVNING | 25.07.2022 |            | OPPFYLT      | Nei                  |
      | 1234    | UTVIDET_BARNETRYGD |                              | 31.05.2023 |            | IKKE_OPPFYLT | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 4567    | 1            | 01.08.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 4567    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 4567    | 1            | 01.07.2023 | 31.03.2033 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 1234    | 1            | 01.08.2022 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1234    | 1            | 01.03.2023 | 31.05.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |

    Og med kompetanser for begrunnelse
      | AktørId | Fra dato   | Til dato | Resultat            | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 4567    | 01.08.2022 |          | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | IE                             | NO                  |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser            | Ekskluderte Begrunnelser |
      | 01.08.2022 | 28.02.2023 | UTBETALING         |           | INNVILGET_FLYTTET_ETTER_SEPARASJON |                          |
      | 01.03.2023 | 31.05.2023 | UTBETALING         |           |                                    |                          |
      | 01.06.2023 | 30.06.2023 | UTBETALING         |           |                                    |                          |
      | 01.07.2023 | 31.03.2033 | UTBETALING         |           |                                    |                          |
      | 01.04.2033 |            | OPPHØR             |           |                                    |                          |