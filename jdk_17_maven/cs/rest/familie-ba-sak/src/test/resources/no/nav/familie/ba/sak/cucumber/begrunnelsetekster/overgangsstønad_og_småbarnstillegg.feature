# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for overgangsstønad og småbarnstillegg

  Bakgrunn:
    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId |
      | 1            | 1        |                     |
      | 2            | 2        | 1                   |

    Og følgende dagens dato 05.09.2023

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 05.04.1985  |
      | 1            | 4567    | BARN       | 22.08.2022  |
      | 2            | 1234    | SØKER      | 05.04.1985  |
      | 2            | 4567    | BARN       | 22.08.2022  |

  Scenario: Skal slå sammen tidligere overgangsstønad dersom periodene er sammenhengende
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | LOVLIG_OPPHOLD,UTVIDET_BARNETRYGD,BOSATT_I_RIKET             |                  | 05.04.1985 |            | OPPFYLT  | Nei                  |

      | 4567    | LOVLIG_OPPHOLD,BOR_MED_SØKER,BOSATT_I_RIKET,GIFT_PARTNERSKAP |                  | 22.08.2022 |            | OPPFYLT  | Nei                  |
      | 4567    | UNDER_18_ÅR                                                  |                  | 22.08.2022 | 21.08.2040 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 4567    | 1            | 01.09.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.07.2023 | 31.07.2028 | 1766  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.08.2028 | 31.07.2040 | 1310  | ORDINÆR_BARNETRYGD | 100     |
      | 1234    | 1            | 01.09.2022 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     |
      | 1234    | 1            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     |
      | 1234    | 1            | 01.07.2023 | 31.07.2040 | 2516  | UTVIDET_BARNETRYGD | 100     |
      | 1234    | 1            | 01.02.2023 | 28.02.2023 | 660   | SMÅBARNSTILLEGG    | 100     |
      | 1234    | 1            | 01.03.2023 | 30.06.2023 | 678   | SMÅBARNSTILLEGG    | 100     |
      | 1234    | 1            | 01.07.2023 | 31.08.2024 | 696   | SMÅBARNSTILLEGG    | 100     |

    Og med overgangsstønad for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   |
      | 4567    | 1            | 01.02.2023 | 30.04.2023 |
      | 4567    | 1            | 01.05.2023 | 31.08.2024 |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser   | Ekskluderte Begrunnelser  |
      | 01.09.2022 | 31.01.2023 | UTBETALING         |           |                           | INNVILGET_SMÅBARNSTILLEGG |
      | 01.02.2023 | 28.02.2023 | UTBETALING         |           | INNVILGET_SMÅBARNSTILLEGG |                           |
      | 01.03.2023 | 30.06.2023 | UTBETALING         |           | INNVILGET_SMÅBARNSTILLEGG |                           |
      | 01.07.2023 | 31.08.2024 | UTBETALING         |           | INNVILGET_SMÅBARNSTILLEGG |                           |
      | 01.09.2024 | 31.07.2028 | UTBETALING         |           |                           | INNVILGET_SMÅBARNSTILLEGG |
      | 01.08.2028 | 31.07.2040 | UTBETALING         |           |                           |                           |
      | 01.08.2040 |            | OPPHØR             |           |                           |                           |

  Scenario: Skal splitte på riktige tidspunkter dersom overgangsstønaden har blitt forlenget siden siste behandling
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | LOVLIG_OPPHOLD,UTVIDET_BARNETRYGD,BOSATT_I_RIKET             |                  | 05.04.1985 |            | OPPFYLT  | Nei                  |

      | 4567    | LOVLIG_OPPHOLD,BOR_MED_SØKER,BOSATT_I_RIKET,GIFT_PARTNERSKAP |                  | 22.08.2022 |            | OPPFYLT  | Nei                  |
      | 4567    | UNDER_18_ÅR                                                  |                  | 22.08.2022 | 21.08.2040 | OPPFYLT  | Nei                  |

    Og lag personresultater for begrunnelse for behandling 2

    Og legg til nye vilkårresultater for begrunnelse for behandling 2
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | LOVLIG_OPPHOLD,UTVIDET_BARNETRYGD,BOSATT_I_RIKET             |                  | 05.04.1985 |            | OPPFYLT  | Nei                  |

      | 4567    | LOVLIG_OPPHOLD,BOR_MED_SØKER,BOSATT_I_RIKET,GIFT_PARTNERSKAP |                  | 22.08.2022 |            | OPPFYLT  | Nei                  |
      | 4567    | UNDER_18_ÅR                                                  |                  | 22.08.2022 | 21.08.2040 | OPPFYLT  | Nei                  |


    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 4567    | 1            | 01.09.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.07.2023 | 31.07.2028 | 1766  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.08.2028 | 31.07.2040 | 1310  | ORDINÆR_BARNETRYGD | 100     |
      | 1234    | 1            | 01.09.2022 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     |
      | 1234    | 1            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     |
      | 1234    | 1            | 01.07.2023 | 31.07.2040 | 2516  | UTVIDET_BARNETRYGD | 100     |
      | 1234    | 1            | 01.02.2023 | 28.02.2023 | 660   | SMÅBARNSTILLEGG    | 100     |
      | 1234    | 1            | 01.03.2023 | 30.06.2023 | 678   | SMÅBARNSTILLEGG    | 100     |
      | 1234    | 1            | 01.07.2023 | 31.10.2023 | 696   | SMÅBARNSTILLEGG    | 100     |

      | 4567    | 2            | 01.09.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 2            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 2            | 01.07.2023 | 31.07.2028 | 1766  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 2            | 01.08.2028 | 31.07.2040 | 1310  | ORDINÆR_BARNETRYGD | 100     |
      | 1234    | 2            | 01.09.2022 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     |
      | 1234    | 2            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     |
      | 1234    | 2            | 01.07.2023 | 31.07.2040 | 2516  | UTVIDET_BARNETRYGD | 100     |
      | 1234    | 2            | 01.02.2023 | 28.02.2023 | 660   | SMÅBARNSTILLEGG    | 100     |
      | 1234    | 2            | 01.03.2023 | 30.06.2023 | 678   | SMÅBARNSTILLEGG    | 100     |
      | 1234    | 2            | 01.07.2023 | 31.08.2024 | 696   | SMÅBARNSTILLEGG    | 100     |

    Og med overgangsstønad for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   |
      | 4567    | 1            | 01.02.2023 | 30.04.2023 |
      | 4567    | 1            | 01.05.2023 | 31.10.2023 |
      | 4567    | 2            | 01.02.2023 | 30.04.2023 |
      | 4567    | 2            | 01.05.2023 | 31.08.2024 |

    Når begrunnelsetekster genereres for behandling 2

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser   | Ekskluderte Begrunnelser  |
      | 01.11.2023 | 31.08.2024 | UTBETALING         |           | INNVILGET_SMÅBARNSTILLEGG |                           |
      | 01.09.2024 | 31.07.2028 | UTBETALING         |           |                           | INNVILGET_SMÅBARNSTILLEGG |
      | 01.08.2028 | 31.07.2040 | UTBETALING         |           |                           |                           |
      | 01.08.2040 |            | OPPHØR             |           |                           |                           |
