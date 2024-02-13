# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder med reduksjon fra forrige periode eller behandling

  Bakgrunn:
    Gitt følgende vedtak
      | BehandlingId | ForrigeBehandlingId |
      | 1            |                     |
      | 2            | 1                   |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 22.02.1988  |
      | 1            | 3456    | BARN       | 27.05.2005  |
      | 1            | 5678    | BARN       | 06.10.2007  |
      | 2            | 1234    | SØKER      | 22.02.1988  |
      | 2            | 3456    | BARN       | 27.05.2005  |
      | 2            | 5678    | BARN       | 06.10.2007  |

  Scenario: Skal ikke splitte når det er reduksjon fra forrige periode selv om det er reduksjon fra forrige behandling
    Og følgende dagens dato 18.09.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                         | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET,LOVLIG_OPPHOLD                  | 22.02.1988 |            | OPPFYLT  |

      | 3456    | BOSATT_I_RIKET,LOVLIG_OPPHOLD,GIFT_PARTNERSKAP | 27.05.2005 |            | OPPFYLT  |
      | 3456    | UNDER_18_ÅR                                    | 27.05.2005 | 26.05.2023 | OPPFYLT  |
      | 3456    | BOR_MED_SØKER                                  | 15.03.2022 |            | OPPFYLT  |

      | 5678    | GIFT_PARTNERSKAP,BOSATT_I_RIKET,LOVLIG_OPPHOLD | 06.10.2007 |            | OPPFYLT  |
      | 5678    | UNDER_18_ÅR                                    | 06.10.2007 | 05.10.2025 | OPPFYLT  |
      | 5678    | BOR_MED_SØKER                                  | 15.03.2022 |            | OPPFYLT  |


    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                         | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET,LOVLIG_OPPHOLD                  | 22.02.1988 |            | OPPFYLT  |

      | 3456    | GIFT_PARTNERSKAP,BOSATT_I_RIKET,LOVLIG_OPPHOLD | 27.05.2005 |            | OPPFYLT  |
      | 3456    | UNDER_18_ÅR                                    | 27.05.2005 | 26.05.2023 | OPPFYLT  |
      | 3456    | BOR_MED_SØKER                                  | 15.03.2022 | 15.03.2023 | OPPFYLT  |

      | 5678    | LOVLIG_OPPHOLD,BOSATT_I_RIKET,GIFT_PARTNERSKAP | 06.10.2007 |            | OPPFYLT  |
      | 5678    | UNDER_18_ÅR                                    | 06.10.2007 | 05.10.2025 | OPPFYLT  |
      | 5678    | BOR_MED_SØKER                                  | 15.03.2022 |            | OPPFYLT  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Sats |
      | 3456    | 1            | 01.04.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 1054 |
      | 3456    | 1            | 01.03.2023 | 30.04.2023 | 1083  | ORDINÆR_BARNETRYGD | 1083 |

      | 5678    | 1            | 01.04.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 1054 |
      | 5678    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 1083 |
      | 5678    | 1            | 01.07.2023 | 30.09.2025 | 1310  | ORDINÆR_BARNETRYGD | 1310 |

      | 3456    | 2            | 01.04.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 1054 |
      | 3456    | 2            | 01.03.2023 | 31.03.2023 | 1083  | ORDINÆR_BARNETRYGD | 1083 |

      | 5678    | 2            | 01.04.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 1054 |
      | 5678    | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 1083 |
      | 5678    | 2            | 01.07.2023 | 30.09.2025 | 1310  | ORDINÆR_BARNETRYGD | 1310 |

    Når vedtaksperioder med begrunnelser genereres for behandling 2

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.04.2023 | 30.06.2023 | UTBETALING         |           |
      | 01.07.2023 | 30.09.2025 | UTBETALING         |           |
      | 01.10.2025 |            | OPPHØR             |           |
