# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for manuell saksbehandling

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.02.1988  |
      | 1            | 4567    | BARN       | 17.10.2005  |
      | 1            | 5678    | BARN       | 16.06.2010  |

  Scenario: Begrunnelser med øvrig trigger automatisk skal ikke være inkludert
    Og følgende dagens dato 18.09.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | LOVLIG_OPPHOLD                                               |                  | 11.02.1988 |            | OPPFYLT  | Nei                  |
      | 1234    | BOSATT_I_RIKET                                               |                  | 15.12.2022 |            | OPPFYLT  | Nei                  |

      | 4567    | BOSATT_I_RIKET,GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOR_MED_SØKER |                  | 17.10.2005 |            | OPPFYLT  | Nei                  |
      | 4567    | UNDER_18_ÅR                                                  |                  | 17.10.2005 | 16.10.2023 | OPPFYLT  | Nei                  |

      | 5678    | BOR_MED_SØKER,BOSATT_I_RIKET,GIFT_PARTNERSKAP,LOVLIG_OPPHOLD |                  | 16.06.2010 |            | OPPFYLT  | Nei                  |
      | 5678    | UNDER_18_ÅR                                                  |                  | 16.06.2010 | 15.06.2028 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 4567    | 1            | 01.01.2023 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 4567    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 4567    | 1            | 01.07.2023 | 30.09.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

      | 5678    | 1            | 01.01.2023 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5678    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 5678    | 1            | 01.07.2023 | 31.05.2028 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser | Ekskluderte Begrunnelser         |
      | 01.01.2023 | 28.02.2023 | UTBETALING         |           |                         |                                  |
      | 01.03.2023 | 30.06.2023 | UTBETALING         |           |                         |                                  |
      | 01.07.2023 | 30.09.2023 | UTBETALING         |           |                         |                                  |
      | 01.10.2023 | 31.05.2028 | UTBETALING         |           |                         | REDUKSJON_UNDER_18_ÅR_AUTOVEDTAK |
      | 01.06.2028 |            | OPPHØR             |           |                         |                                  |
