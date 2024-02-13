# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser ved ingen endring

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 06.11.1984  |
      | 1            | 2       | BARN       | 07.09.2019  |

  Scenario: Gi innvilget-begrunnelser når det ikke er endring i andelene
    Og følgende dagens dato 15.09.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2       | UNDER_18_ÅR                                 |                              | 07.09.2019 | 06.09.2037 | OPPFYLT  | Nei                  |
      | 2       | GIFT_PARTNERSKAP                            |                              | 07.09.2019 |            | OPPFYLT  | Nei                  |
      | 2       | LOVLIG_OPPHOLD                              |                              | 07.09.2019 | 14.07.2023 | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET                              | BARN_BOR_I_NORGE             | 07.09.2019 | 14.07.2023 | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER                               | BARN_BOR_I_EØS_MED_SØKER     | 07.06.2023 | 14.07.2023 | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER |                              | 15.07.2023 | 15.08.2023 | OPPFYLT  | Nei                  |

      | 1       | LOVLIG_OPPHOLD                              |                              | 06.11.1984 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                              | OMFATTET_AV_NORSK_LOVGIVNING | 11.11.2021 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.07.2023 | 31.08.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |

    Og med kompetanser for begrunnelse
      | AktørId | Fra dato   | Til dato   | Resultat            | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.07.2023 | 31.07.2023 | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | BE                             | BE                  |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser                                | Ekskluderte Begrunnelser |
      | 01.07.2023 | 31.07.2023 | UTBETALING         |           |                                                        |                          |
      | 01.08.2023 | 31.08.2023 | UTBETALING         |           | INNVILGET_OVERGANG_EØS_TIL_NASJONAL_SEPARASJONSAVTALEN |                          |
      | 01.09.2023 |            | OPPHØR             |           |                                                        |                          |

  Scenario: Skal inkludere begrunnelser med "Innvilget eller økning" resultat dersom periode resultatet er ingen endring for EØS
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | BARN       | 18.11.2015  |
      | 1            | 4567    | SØKER      | 01.11.1976  |
      | 1            | 5678    | BARN       | 09.03.2021  |

    Og følgende dagens dato 25.09.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | GIFT_PARTNERSKAP |                              | 18.11.2015 |            | OPPFYLT  | Nei                  |
      | 1234    | UNDER_18_ÅR      |                              | 18.11.2015 | 17.11.2033 | OPPFYLT  | Nei                  |
      | 1234    | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 12.01.2016 |            | OPPFYLT  | Nei                  |
      | 1234    | LOVLIG_OPPHOLD   |                              | 12.01.2016 |            | OPPFYLT  | Nei                  |
      | 1234    | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 12.01.2016 |            | OPPFYLT  | Nei                  |

      | 4567    | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 12.01.2016 |            | OPPFYLT  | Nei                  |
      | 4567    | LOVLIG_OPPHOLD   |                              | 12.01.2016 |            | OPPFYLT  | Nei                  |

      | 5678    | UNDER_18_ÅR      |                              | 09.03.2021 | 08.03.2039 | OPPFYLT  | Nei                  |
      | 5678    | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 09.03.2021 |            | OPPFYLT  | Nei                  |
      | 5678    | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 09.03.2021 |            | OPPFYLT  | Nei                  |
      | 5678    | LOVLIG_OPPHOLD   |                              | 09.03.2021 |            | OPPFYLT  | Nei                  |
      | 5678    | GIFT_PARTNERSKAP |                              | 09.03.2021 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 5678    | 1            | 01.04.2021 | 31.08.2021 | 1354  | ORDINÆR_BARNETRYGD | 100     | 1354 |
      | 5678    | 1            | 01.09.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     | 1654 |
      | 5678    | 1            | 01.01.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 5678    | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 5678    | 1            | 01.07.2023 | 28.02.2027 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 5678    | 1            | 01.03.2027 | 28.02.2039 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 1234    | 1            | 01.02.2016 | 31.07.2019 | 0     | ORDINÆR_BARNETRYGD | 0       | 970  |
      | 1234    | 1            | 01.08.2019 | 31.12.2019 | 943   | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 1234    | 1            | 01.01.2020 | 31.08.2020 | 936   | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 1234    | 1            | 01.09.2020 | 31.12.2020 | 1236  | ORDINÆR_BARNETRYGD | 100     | 1354 |
      | 1234    | 1            | 01.01.2021 | 31.03.2021 | 1241  | ORDINÆR_BARNETRYGD | 100     | 1354 |
      | 1234    | 1            | 01.04.2021 | 31.08.2021 | 1354  | ORDINÆR_BARNETRYGD | 100     | 1354 |
      | 1234    | 1            | 01.09.2021 | 31.10.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     | 1654 |
      | 1234    | 1            | 01.11.2021 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 1234    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 1234    | 1            | 01.07.2023 | 31.10.2033 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent |
      | 1234    | 1            | 01.02.2016 | 01.07.2019 | ETTERBETALING_3ÅR | 0       |

    Og med kompetanser for begrunnelse
      | AktørId    | Fra dato   | Til dato   | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 5678, 1234 | 01.04.2021 |            | NORGE_ER_PRIMÆRLAND   | 1            | ARBEIDER         | INAKTIV                   | NO                    | LV                             | LV                  |
      | 1234       | 01.08.2019 | 31.03.2021 | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | LV                             | LV                  |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Inkluderte Begrunnelser | Inkluderte Begrunnelser                                | Ekskluderte Begrunnelser |
      | 01.02.2016 | 31.07.2019 | OPPHØR             |                                   |                                                        |                          |
      | 01.08.2019 | 31.12.2019 | UTBETALING         |                                   |                                                        |                          |
      | 01.01.2020 | 31.08.2020 | UTBETALING         | EØS_FORORDNINGEN                  | INNVILGET_TILLEGGSTEKST_SATSENDRING_OG_VALUTAJUSTERING |                          |
      | 01.09.2020 | 31.12.2020 | UTBETALING         |                                   |                                                        |                          |
      | 01.01.2021 | 31.03.2021 | UTBETALING         |                                   |                                                        |                          |
      | 01.04.2021 | 31.08.2021 | UTBETALING         |                                   |                                                        |                          |
      | 01.09.2021 | 31.10.2021 | UTBETALING         |                                   |                                                        |                          |
      | 01.11.2021 | 31.12.2021 | UTBETALING         |                                   |                                                        |                          |
      | 01.01.2022 | 28.02.2023 | UTBETALING         |                                   |                                                        |                          |
      | 01.03.2023 | 30.06.2023 | UTBETALING         |                                   |                                                        |                          |
      | 01.07.2023 | 28.02.2027 | UTBETALING         |                                   |                                                        |                          |
      | 01.03.2027 | 31.10.2033 | UTBETALING         |                                   |                                                        |                          |
      | 01.11.2033 | 28.02.2039 | UTBETALING         |                                   |                                                        |                          |
      | 01.03.2039 |            | OPPHØR             |                                   |                                                        |                          |