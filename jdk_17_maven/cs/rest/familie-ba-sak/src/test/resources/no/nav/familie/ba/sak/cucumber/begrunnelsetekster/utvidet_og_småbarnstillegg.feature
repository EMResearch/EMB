# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for utvidet barnetrygd og småbarnstillegg

  Bakgrunn:
    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId |
      | 1            | 1        |                     |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 01.09.1984  |
      | 1            | 4567    | BARN       | 02.02.2015  |

  Scenario: Skal håndtere reduksjon utvidet
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET,LOVLIG_OPPHOLD                  |                  | 01.09.1984 |            | OPPFYLT  | Nei                  |
      | 1234    | UTVIDET_BARNETRYGD                             |                  | 15.10.2022 | 15.05.2023 | OPPFYLT  | Nei                  |

      | 4567    | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 02.02.2015 |            | OPPFYLT  | Nei                  |
      | 4567    | UNDER_18_ÅR                                    |                  | 02.02.2015 | 01.02.2033 | OPPFYLT  | Nei                  |
      | 4567    | BOR_MED_SØKER                                  |                  | 15.02.2022 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 4567    | 1            | 01.03.2022 | 01.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.03.2023 | 01.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.07.2023 | 01.01.2033 | 1310  | ORDINÆR_BARNETRYGD | 100     |
      | 1234    | 1            | 01.11.2022 | 01.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     |
      | 1234    | 1            | 01.03.2023 | 01.05.2023 | 2489  | UTVIDET_BARNETRYGD | 100     |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | Fra dato | Til dato | BehandlingId | Årsak | Prosent |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser                  | Ekskluderte Begrunnelser |
      | 01.03.2022 | 31.10.2022 | UTBETALING         |           |                                          |                          |
      | 01.11.2022 | 28.02.2023 | UTBETALING         |           |                                          |                          |
      | 01.03.2023 | 31.05.2023 | UTBETALING         |           |                                          |                          |
      | 01.06.2023 | 30.06.2023 | UTBETALING         |           | REDUKSJON_SAMBOER_IKKE_LENGER_FORSVUNNET |                          |
      | 01.07.2023 | 31.01.2033 | UTBETALING         |           |                                          |                          |
      | 01.02.2033 |            | OPPHØR             |           |                                          |                          |

  Scenario: Skal gi riktige begrunnelser ved småbarnstillegg
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET,LOVLIG_OPPHOLD                  |                  | 01.09.1984 |            | OPPFYLT  | Nei                  |
      | 1234    | UTVIDET_BARNETRYGD                             |                  | 15.10.2022 | 15.05.2023 | OPPFYLT  | Nei                  |

      | 4567    | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 02.02.2015 |            | OPPFYLT  | Nei                  |
      | 4567    | UNDER_18_ÅR                                    |                  | 02.02.2015 | 01.02.2033 | OPPFYLT  | Nei                  |
      | 4567    | BOR_MED_SØKER                                  |                  | 15.02.2022 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 4567    | 1            | 01.03.2022 | 01.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.03.2023 | 01.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.07.2023 | 01.01.2033 | 1310  | ORDINÆR_BARNETRYGD | 100     |
      | 1234    | 1            | 01.12.2022 | 01.02.2023 | 2489  | SMÅBARNSTILLEGG    | 100     |
      | 1234    | 1            | 01.11.2022 | 01.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     |
      | 1234    | 1            | 01.03.2023 | 01.05.2023 | 2489  | UTVIDET_BARNETRYGD | 100     |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | Fra dato | Til dato | BehandlingId | Årsak | Prosent |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser                                 | Ekskluderte Begrunnelser  |
      | 01.03.2022 | 31.10.2022 | UTBETALING         |           |                                                         |                           |
      | 01.11.2022 | 30.11.2022 | UTBETALING         |           |                                                         | INNVILGET_SMÅBARNSTILLEGG |
      | 01.12.2022 | 28.02.2023 | UTBETALING         |           | INNVILGET_SMÅBARNSTILLEGG                               |                           |
      | 01.03.2023 | 31.05.2023 | UTBETALING         |           | REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_BARN_UNDER_TRE_ÅR | INNVILGET_SMÅBARNSTILLEGG |
      | 01.06.2023 | 30.06.2023 | UTBETALING         |           |                                                         |                           |
      | 01.07.2023 | 31.01.2033 | UTBETALING         |           |                                                         |                           |
      | 01.02.2033 |            | OPPHØR             |           |                                                         |                           |
