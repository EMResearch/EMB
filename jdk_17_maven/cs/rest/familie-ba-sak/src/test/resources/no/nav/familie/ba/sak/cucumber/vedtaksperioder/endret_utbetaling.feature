# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder med endret utbetaling der endringstidspunkt påvirker periodene

  Bakgrunn:
    Gitt følgende vedtak
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 24.12.1987  |
      | 1            | 3456    | BARN       | 02.12.2016  |

  Scenario: Skal lage ikke utbetalingsperiode når andelene er endret til 0% og det ikke er delt bosted

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |                      |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 02.12.2016 |            | Oppfylt  |                      |
      | 3456    | UNDER_18_ÅR                                                     | 02.12.2016 | 01.12.2034 | Oppfylt  |                      |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId | Prosent |
      | 3456    | 01.01.2017 | 30.11.2034 | 1234  | 1            | 0       |

    Og med endrede utbetalinger
      | AktørId | Fra dato   | Til dato   | BehandlingId | Årsak             | Prosent |
      | 3456    | 01.01.2017 | 30.11.2034 | 1            | ETTERBETALING_3ÅR | 0       |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar            |
      | 01.01.2017 | 30.11.2034 | Opphør             | Endret utbetaling 0% |
      | 01.12.2034 |            | Opphør             | Opphør 18 år         |

  Scenario:  Skal lage utbetalingsperiode når andelene er endret til 0% og det er delt bosted

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |                      |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 02.12.2016 |            | Oppfylt  |                      |
      | 3456    | UNDER_18_ÅR                                                     | 02.12.2016 | 01.12.2034 | Oppfylt  |                      |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId | Prosent |
      | 3456    | 01.01.2017 | 30.11.2034 | 1234  | 1            | 0       |

    Og med endrede utbetalinger
      | AktørId | Fra dato   | Til dato   | BehandlingId | Årsak       | prosent |
      | 3456    | 01.01.2017 | 30.11.2034 | 1            | DELT_BOSTED | 0       |


    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar       |
      | 01.01.2017 | 30.11.2034 | Utbetaling         | Delt bosted     |
      | 01.12.2034 |            | Opphør             | Barn er over 18 |

  Scenario: Skal ikke slå sammen vedtaksperiodene som ikke er innvilget dersom det er på grunn av endret utbetaling
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende vedtak
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | AVSLÅTT             | SØKNAD           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | BARN       | 02.02.2015  |
      | 1            | 2       | SØKER      | 17.04.1985  |

    Og følgende dagens dato 27.09.2023
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD                  |                  | 17.04.1985 |            | OPPFYLT  | Nei                  |

      | 1       | BOSATT_I_RIKET,GIFT_PARTNERSKAP,LOVLIG_OPPHOLD |                  | 02.02.2015 |            | OPPFYLT  | Nei                  |
      | 1       | UNDER_18_ÅR                                    |                  | 02.02.2015 | 01.02.2033 | OPPFYLT  | Nei                  |
      | 1       | BOR_MED_SØKER                                  |                  | 02.02.2015 | 15.12.2018 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.03.2015 | 31.12.2018 | 0     | ORDINÆR_BARNETRYGD | 0       | 970  |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent |
      | 1       | 1            | 01.03.2015 | 31.12.2018 | ETTERBETALING_3ÅR | 0       |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype |
      | 01.03.2015 | 31.12.2018 | OPPHØR             |
      | 01.01.2019 |            | OPPHØR             |
