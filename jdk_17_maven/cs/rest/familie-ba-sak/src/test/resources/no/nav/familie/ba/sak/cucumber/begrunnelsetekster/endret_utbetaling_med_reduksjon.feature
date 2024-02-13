# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser endret utbetaling med reduksjon

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 14.10.1987  |
      | 1            | 3456    | BARN       | 08.02.2013  |
      | 1            | 5678    | BARN       | 13.01.2017  |

  Scenario: Begrunnelse endret utbetaling - endre mottaker
    Og følgende dagens dato 04.10.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                      | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET,LOVLIG_OPPHOLD               | 14.10.1987 |            | OPPFYLT  |

      | 3456    | UNDER_18_ÅR                                 | 08.02.2013 | 07.02.2031 | OPPFYLT  |
      | 3456    | GIFT_PARTNERSKAP                            | 08.02.2013 |            | OPPFYLT  |
      | 3456    | BOSATT_I_RIKET,BOR_MED_SØKER,LOVLIG_OPPHOLD | 01.02.2022 |            | OPPFYLT  |

      | 5678    | UNDER_18_ÅR                                 | 13.01.2017 | 12.01.2035 | OPPFYLT  |
      | 5678    | GIFT_PARTNERSKAP                            | 13.01.2017 |            | OPPFYLT  |
      | 5678    | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD | 01.02.2022 |            | OPPFYLT  |


    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3456    | 1            | 01.03.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3456    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3456    | 1            | 01.07.2023 | 31.01.2031 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 5678    | 1            | 01.03.2022 | 31.12.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 5678    | 1            | 01.01.2023 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5678    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 5678    | 1            | 01.07.2023 | 30.09.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 5678    | 1            | 01.10.2023 | 31.12.2034 | 0     | ORDINÆR_BARNETRYGD | 0       | 1310 |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak          | Prosent |
      | 5678    | 1            | 01.10.2023 | 31.12.2034 | ENDRE_MOTTAKER | 0       |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser                    | Ekskluderte Begrunnelser |
      | 01.03.2022 | 31.12.2022 | UTBETALING         |           |                                            |                          |
      | 01.01.2023 | 28.02.2023 | UTBETALING         |           |                                            |                          |
      | 01.03.2023 | 30.06.2023 | UTBETALING         |           |                                            |                          |
      | 01.07.2023 | 30.09.2023 | UTBETALING         |           |                                            |                          |
      | 01.10.2023 | 31.01.2031 | UTBETALING         |           | ENDRET_UTBETALING_REDUKSJON_ENDRE_MOTTAKER |                          |
      | 01.02.2031 | 31.12.2034 | OPPHØR             |           |                                            |                          |
      | 01.01.2035 |            | OPPHØR             |           |                                            |                          |