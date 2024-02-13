# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for revurdering med opphør

  Bakgrunn:
    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId |
      | 1            | 1        |                     |
      | 2            | 1        | 1                   |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 29.10.1984  |
      | 1            | 4567    | BARN       | 22.08.2022  |
      | 2            | 1234    | SØKER      | 29.10.1984  |
      | 2            | 4567    | BARN       | 22.08.2022  |

  Scenario: Skal håndtere opphør på tvers av behandlinger
    Og lag personresultater for begrunnelse for behandling 1
    Og lag personresultater for begrunnelse for behandling 2

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | LOVLIG_OPPHOLD,BOSATT_I_RIKET                  |                  | 29.10.1984 |            | OPPFYLT  | Nei                  |
      | 1234    | UTVIDET_BARNETRYGD                             |                  | 07.09.2022 |            | OPPFYLT  | Nei                  |

      | 4567    | LOVLIG_OPPHOLD,GIFT_PARTNERSKAP,BOSATT_I_RIKET |                  | 22.08.2022 |            | OPPFYLT  | Nei                  |
      | 4567    | BOR_MED_SØKER                                  |                  | 22.06.2023 |            | OPPFYLT  | Nei                  |
      | 4567    | UNDER_18_ÅR                                    |                  | 22.08.2022 | 21.08.2040 | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for begrunnelse for behandling 2
      | AktørId | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | LOVLIG_OPPHOLD,BOSATT_I_RIKET                  |                  | 29.10.1984 |            | OPPFYLT  | Nei                  |
      | 1234    | UTVIDET_BARNETRYGD                             |                  | 07.09.2022 |            | OPPFYLT  | Nei                  |

      | 4567    | GIFT_PARTNERSKAP,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 22.08.2022 |            | OPPFYLT  | Nei                  |
      | 4567    | BOR_MED_SØKER                                  |                  | 22.06.2023 | 15.08.2023 | OPPFYLT  | Nei                  |
      | 4567    | UNDER_18_ÅR                                    |                  | 22.08.2022 | 21.08.2040 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 4567    | 1            | 01.07.2023 | 01.07.2028 | 1766  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.08.2028 | 01.07.2040 | 1310  | ORDINÆR_BARNETRYGD | 100     |
      | 1234    | 1            | 01.07.2023 | 01.07.2040 | 2516  | UTVIDET_BARNETRYGD | 100     |
      | 4567    | 2            | 01.07.2023 | 01.08.2023 | 1766  | ORDINÆR_BARNETRYGD | 100     |
      | 1234    | 2            | 01.07.2023 | 01.08.2023 | 2516  | UTVIDET_BARNETRYGD | 100     |

    Når begrunnelsetekster genereres for behandling 2

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser | Ekskluderte Begrunnelser |
      | 01.09.2023 |          | OPPHØR             |           | OPPHØR_BARN_FLYTTET_FRA_SØKER |                          |
