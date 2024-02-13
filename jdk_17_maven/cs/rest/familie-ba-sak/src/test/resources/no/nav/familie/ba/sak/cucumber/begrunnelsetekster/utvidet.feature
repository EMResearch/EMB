# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for utvidet barnetrygd

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 26.04.1985  |
      | 1            | 2       | BARN       | 12.01.2022  |

  Scenario: Skal gi innvilgelsesbegrunnelse INNVILGET_BOR_ALENE_MED_BARN for utvidet i første utbetalingsperiode etter at utvidet er oppfylt
    Og følgende dagens dato 28.09.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 26.04.1985 |            | OPPFYLT  | Nei                  |
      | 1       | UTVIDET_BARNETRYGD            |                  | 13.02.2023 |            | OPPFYLT  | Nei                  |

      | 2       | GIFT_PARTNERSKAP              |                  | 12.01.2022 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                   |                  | 12.01.2022 | 11.01.2040 | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER  |                  | 13.02.2023 |            | OPPFYLT  | Nei                  |
      | 2       | LOVLIG_OPPHOLD                |                  | 23.04.2023 | 30.06.2023 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.05.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 2       | 1            | 01.05.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser      | Ekskluderte Begrunnelser |
      | 01.05.2023 | 30.06.2023 | UTBETALING         |           | INNVILGET_BOR_ALENE_MED_BARN | INNVILGET_SKILT          |
      | 01.07.2023 |            | OPPHØR             |           |                              |                          |
