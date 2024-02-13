# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for etter endret utbetaling, en mor med ett barn

  Bakgrunn:
    Gitt følgende behandling
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 13.04.2020  |

  Scenario: Begrunnelse etter endret utbetaling delt bosted
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 13.04.2020 | 03.01.2021 | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 04.01.2021 | 15.01.2022 | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 16.01.2022 |            | Oppfylt  |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | Fra dato   | Til dato   | BehandlingId | Årsak       | Prosent |
      | 3456    | 01.05.2020 | 31.01.2021 | 1            | DELT_BOSTED | 0       |
      | 3456    | 01.02.2021 | 31.01.2022 | 1            | DELT_BOSTED | 100     |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId | Prosent |
      | 3456    | 01.05.2020 | 31.01.2021 | 0     | 1            | 0       |
      | 3456    | 01.02.2021 | 31.01.2022 | 1354  | 1            | 100     |
      | 3456    | 01.02.2022 | 31.03.2038 | 1354  | 1            | 100     |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Inkluderte Begrunnelser                        | Ekskluderte Begrunnelser                       | Kommentar                                          |
      | 01.05.2020 | 31.01.2021 | UTBETALING         |                                                |                                                | Ingen etter endret utbetalingsbegrunnelse skal med |
      | 01.02.2021 | 31.01.2022 | UTBETALING         |                                                | ETTER_ENDRET_UTBETALING_HAR_AVTALE_DELT_BOSTED |                                                    |
      | 01.02.2022 | 31.03.2038 | UTBETALING         | ETTER_ENDRET_UTBETALING_HAR_AVTALE_DELT_BOSTED |                                                |                                                    |
      | 01.04.2038 |            | OPPHØR             | OPPHØR_UNDER_18_ÅR                             |                                                |                                                    |

  Scenario: Begrunnelse etter endret utbetaling allerede utbetalt
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2020 |            | Oppfylt  |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | Fra dato   | Til dato   | BehandlingId | Årsak             | Prosent |
      | 3456    | 01.05.2020 | 31.01.2021 | 1            | ETTERBETALING_3ÅR | 0       |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId | Prosent |
      | 3456    | 01.05.2020 | 31.01.2021 | 0     | 1            | 0       |
      | 3456    | 01.02.2021 | 31.03.2038 | 1354  | 1            | 100     |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Inkluderte Begrunnelser               | Ekskluderte Begrunnelser |
      | 01.05.2020 | 31.01.2021 | OPPHØR             |                                       |                          |
      | 01.02.2021 | 31.03.2038 | UTBETALING         | ETTER_ENDRET_UTBETALING_ETTERBETALING |                          |
      | 01.04.2038 |            | OPPHØR             | OPPHØR_UNDER_18_ÅR                    |                          |



