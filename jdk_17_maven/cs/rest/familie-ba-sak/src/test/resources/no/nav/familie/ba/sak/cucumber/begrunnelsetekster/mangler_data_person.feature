# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser når ett barn fødes etter et annet

  Bakgrunn:
    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId |
      | 1            | 1        |                     |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 27.01.1985  |
      | 1            | 4567    | BARN       | 02.02.2015  |
      | 1            | 6789    | BARN       | 22.08.2022  |

  Scenario: Skal tåle periode uten vilkår på person fra mars 2022 til august 2022 når barn 6789 ikke er født.
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                           | Utdypende vilkår         | Fra dato   | Til dato   | Resultat |
      | 1234    | LOVLIG_OPPHOLD, BOSATT_I_RIKET                   |                          | 27.01.1985 |            | OPPFYLT  |
      | 1234    | UTVIDET_BARNETRYGD                               |                          | 11.11.2022 | 15.05.2023 | OPPFYLT  |

      | 4567    | BOR_MED_SØKER                                    | VURDERING_ANNET_GRUNNLAG | 15.02.2022 |            | OPPFYLT  |
      | 4567    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD |                          | 02.02.2015 |            | OPPFYLT  |
      | 4567    | UNDER_18_ÅR                                      |                          | 02.02.2015 | 01.02.2033 | OPPFYLT  |

      | 6789    | BOSATT_I_RIKET, LOVLIG_OPPHOLD, GIFT_PARTNERSKAP |                          | 22.08.2022 |            | OPPFYLT  |
      | 6789    | BOR_MED_SØKER                                    | DELT_BOSTED              | 22.08.2022 |            | OPPFYLT  |
      | 6789    | UNDER_18_ÅR                                      |                          | 22.08.2022 | 21.08.2040 | OPPFYLT  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 6789    | 1            | 01.09.2022 | 01.02.2023 | 838   | ORDINÆR_BARNETRYGD | 50      |
      | 6789    | 1            | 01.03.2023 | 01.06.2023 | 862   | ORDINÆR_BARNETRYGD | 50      |
      | 6789    | 1            | 01.07.2023 | 01.07.2028 | 883   | ORDINÆR_BARNETRYGD | 50      |
      | 6789    | 1            | 01.08.2028 | 01.07.2040 | 655   | ORDINÆR_BARNETRYGD | 50      |
      | 4567    | 1            | 01.03.2022 | 01.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.03.2023 | 01.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.07.2023 | 01.01.2033 | 1310  | ORDINÆR_BARNETRYGD | 100     |
      | 1234    | 1            | 01.12.2022 | 01.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     |
      | 1234    | 1            | 01.03.2023 | 01.05.2023 | 2489  | UTVIDET_BARNETRYGD | 100     |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | Fra dato | Til dato | BehandlingId | Årsak | Prosent |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser | Ekskluderte Begrunnelser |
      | 01.03.2022 | 31.08.2022 | UTBETALING         |           |                         |                          |
      | 01.09.2022 | 30.11.2022 | UTBETALING         |           |                         |                          |
      | 01.12.2022 | 28.02.2023 | UTBETALING         |           |                         |                          |
      | 01.03.2023 | 31.05.2023 | UTBETALING         |           |                         |                          |
      | 01.06.2023 | 30.06.2023 | UTBETALING         |           |                         |                          |
      | 01.07.2023 | 31.07.2028 | UTBETALING         |           |                         |                          |
      | 01.08.2028 | 31.01.2033 | UTBETALING         |           |                         |                          |
      | 01.02.2033 | 31.07.2040 | UTBETALING         |           |                         |                          |
      | 01.08.2040 |            | OPPHØR             |           |                         |                          |
