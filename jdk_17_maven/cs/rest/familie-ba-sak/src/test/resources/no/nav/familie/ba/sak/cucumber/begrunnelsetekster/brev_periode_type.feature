# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for forskjellige brevperiodetyper

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat         | Behandlingsårsak |
      | 1            | 1        |                     | DELVIS_INNVILGET_OG_OPPHØRT | SØKNAD           |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 03.07.1967  |
      | 1            | 2       | BARN       | 29.04.2020  |
      | 1            | 3       | BARN       | 02.12.2019  |

    Og følgende dagens dato 25.09.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD                                               |                  | 03.07.1967 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                                               |                  | 25.08.2020 | 25.09.2020 | OPPFYLT  | Nei                  |

      | 2       | UNDER_18_ÅR                                                  |                  | 29.04.2020 | 28.04.2038 | OPPFYLT  | Nei                  |
      | 2       | LOVLIG_OPPHOLD,BOR_MED_SØKER,GIFT_PARTNERSKAP,BOSATT_I_RIKET |                  | 29.04.2020 |            | OPPFYLT  | Nei                  |

      | 3       | BOR_MED_SØKER,GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 02.12.2019 |            | OPPFYLT  | Nei                  |
      | 3       | UNDER_18_ÅR                                                  |                  | 02.12.2019 | 01.12.2037 | OPPFYLT  | Nei                  |

  Scenario: Skal gi begrunnelse knyttet til utbetaling dersom det fremdeles er utbetaling

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3       | 1            | 01.09.2020 | 30.09.2020 | 1354  | ORDINÆR_BARNETRYGD | 100     | 1354 |
      | 2       | 1            | 01.09.2020 | 30.09.2020 | 0     | ORDINÆR_BARNETRYGD | 0       | 1354 |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent |
      | 2       | 1            | 01.09.2020 | 01.09.2020 | ETTERBETALING_3ÅR | 0       |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser                           | Ekskluderte Begrunnelser                             |
      | 01.09.2020 | 30.09.2020 | UTBETALING         |           | ENDRET_UTBETALING_TRE_ÅR_TILBAKE_I_TID_UTBETALING | ENDRET_UTBETALING_ETTERBETALING_TRE_ÅR_TILBAKE_I_TID |
      | 01.10.2020 |            | OPPHØR             |           |                                                   |                                                      |

  Scenario: Skal gi begrunnelse knyttet til "ingen utbetaling" dersom det ikke er utbetaling

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3       | 1            | 01.09.2020 | 30.09.2020 | 0     | ORDINÆR_BARNETRYGD | 0       | 1354 |
      | 2       | 1            | 01.09.2020 | 30.09.2020 | 0     | ORDINÆR_BARNETRYGD | 0       | 1354 |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent |
      | 2       | 1            | 01.09.2020 | 01.09.2020 | ETTERBETALING_3ÅR | 0       |
      | 3       | 1            | 01.09.2020 | 01.09.2020 | ETTERBETALING_3ÅR | 0       |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser                              | Ekskluderte Begrunnelser                          |
      | 01.09.2020 | 30.09.2020 | OPPHØR             |           | ENDRET_UTBETALING_ETTERBETALING_TRE_ÅR_TILBAKE_I_TID | ENDRET_UTBETALING_TRE_ÅR_TILBAKE_I_TID_UTBETALING |
      | 01.10.2020 |            | OPPHØR             |           |                                                      |                                                   |
