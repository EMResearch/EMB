# language: no
# encoding: UTF-8


Egenskap: Vedtaksperioder - Endringstidspunkt

  Bakgrunn:
    Gitt følgende vedtak
      | BehandlingId |
      | 1            |
      | 2            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 24.12.1987  |
      | 1            | 3456    | BARN       | 02.12.2016  |
      | 2            | 1234    | SØKER      | 24.12.1987  |
      | 2            | 3456    | BARN       | 02.12.2016  |


  Scenario: Skal kun ta med vedtaksperioder som kommer etter

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET                                   | 24.12.1987 |            | Oppfylt      |                      |
      | 1234    | LOVLIG_OPPHOLD                                   | 24.12.1987 | 01.12.2020 | Oppfylt      |                      |
      | 1234    | LOVLIG_OPPHOLD                                   | 02.12.2020 | 30.09.2021 | ikke_oppfylt | Ja                   |
      | 1234    | LOVLIG_OPPHOLD                                   | 01.10.2021 |            | Oppfylt      |                      |

      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 02.12.2016 |            | Oppfylt      |                      |
      | 3456    | BOR_MED_SØKER                                    | 02.12.2016 | 01.12.2020 | Oppfylt      |                      |
      | 3456    | UNDER_18_ÅR                                      | 02.12.2016 | 01.12.2034 | Oppfylt      |                      |
      | 3456    | BOR_MED_SØKER                                    | 02.12.2020 | 30.09.2021 | ikke_oppfylt | Ja                   |
      | 3456    | BOR_MED_SØKER                                    | 01.10.2021 |            | Oppfylt      |                      |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.12.2016 | 31.12.2020 | 1234  | 1            |
      | 3456    | 01.10.2021 | 30.11.2034 | 1234  | 1            |

    Og med overstyrt endringstidspunkt
      | Endringstidspunkt | BehandlingId |
      | 01.11.2021        | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar                                                     |
      | 01.01.2021 | 31.10.2021 | Avslag             | Avslag skal alltid med, selv om de er før endringstidspunktet |
      | 01.11.2021 | 30.11.2034 | Utbetaling         | Etter endringstidspunktet                                     |
      | 01.12.2034 |            | Opphør             | Barn er over 18                                               |

  Scenario: Skal ta med eøs-perioder som kommer før første periode
    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 15.07.2021 |            | Oppfylt  |                      |

      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 15.07.2021 |            | Oppfylt  |                      |
      | 3456    | UNDER_18_ÅR                                                     | 02.12.2016 | 01.12.2034 | Oppfylt  |                      |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.08.2021 | 30.11.2034 | 1234  | 1            |

    Og med overstyrt endringstidspunkt
      | Endringstidspunkt | BehandlingId |
      | 01.11.2021        | 1            |

    Og lag personresultater for behandling 2
    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 15.07.2021 |            | Oppfylt  |

      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 15.07.2021 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 02.12.2016 | 01.12.2034 | Oppfylt  |

      | 1234    | BOSATT_I_RIKET                                                  | 15.06.2021 | 14.07.2021 | Oppfylt  |
      | 1234    | LOVLIG_OPPHOLD                                                  | 15.06.2021 | 14.07.2021 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP                                                | 15.06.2021 | 14.07.2021 | Oppfylt  |
      | 3456    | BOSATT_I_RIKET                                                  | 15.06.2021 | 14.07.2021 | Oppfylt  |
      | 3456    | LOVLIG_OPPHOLD                                                  | 15.06.2021 | 14.07.2021 | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                                   | 15.06.2021 | 14.07.2021 | Oppfylt  |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.07.2021 | 31.07.2021 | 0     | 2            |
      | 3456    | 01.08.2021 | 30.11.2034 | 1234  | 2            |

    Når vedtaksperioder med begrunnelser genereres for behandling 2

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar        |
      | 01.07.2021 | 31.07.2021 | Utbetaling         | Sekundærland EØS |
      | 01.08.2021 | 30.11.2034 | Utbetaling         |                  |
      | 01.12.2034 |            | Opphør             | Barn er over 18  |

  Scenario: Skal ikke se på endring i avslåtte vilkår fra forrige behandling når vi beregner endringstidspunktet
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende vedtak
      | BehandlingId | FagsakId | ForrigeBehandlingId |
      | 1            | 1        |                     |
      | 2            | 1        | 1                   |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 16.02.1985  |
      | 1            | 2       | BARN       | 23.04.2017  |
      | 1            | 3       | BARN       | 22.03.2015  |
      | 2            | 1       | SØKER      | 16.02.1985  |
      | 2            | 2       | BARN       | 23.04.2017  |
      | 2            | 3       | BARN       | 22.03.2015  |

    Og følgende dagens dato 19.09.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1       | UTVIDET_BARNETRYGD                                           |                  |            |            | IKKE_OPPFYLT | Ja                   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD                                |                  | 16.02.1985 | 15.02.2023 | OPPFYLT      | Nei                  |

      | 3       | LOVLIG_OPPHOLD,BOR_MED_SØKER,BOSATT_I_RIKET,GIFT_PARTNERSKAP |                  | 22.03.2015 |            | OPPFYLT      | Nei                  |
      | 3       | UNDER_18_ÅR                                                  |                  | 22.03.2015 | 21.03.2033 | OPPFYLT      | Nei                  |

      | 2       | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 23.04.2017 |            | OPPFYLT      | Nei                  |
      | 2       | UNDER_18_ÅR                                                  |                  | 23.04.2017 | 22.04.2035 | OPPFYLT      | Nei                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                         | Utdypende vilkår            | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD                  |                             | 16.02.1985 | 15.02.2023 | OPPFYLT  | Nei                  |

      | 2       | BOSATT_I_RIKET,GIFT_PARTNERSKAP,LOVLIG_OPPHOLD |                             | 23.04.2017 |            | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER                                  |                             | 23.04.2017 | 08.01.2023 | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                    |                             | 23.04.2017 | 22.04.2035 | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER                                  | DELT_BOSTED_SKAL_IKKE_DELES | 09.01.2023 |            | OPPFYLT  | Nei                  |

      | 3       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,GIFT_PARTNERSKAP |                             | 22.03.2015 |            | OPPFYLT  | Nei                  |
      | 3       | BOR_MED_SØKER                                  |                             | 22.03.2015 | 08.01.2023 | OPPFYLT  | Nei                  |
      | 3       | UNDER_18_ÅR                                    |                             | 22.03.2015 | 21.03.2033 | OPPFYLT  | Nei                  |
      | 3       | BOR_MED_SØKER                                  | DELT_BOSTED                 | 09.01.2023 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.05.2017 | 28.02.2019 | 970   | ORDINÆR_BARNETRYGD | 100     | 970  |
      | 2       | 1            | 01.03.2019 | 31.08.2020 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.09.2020 | 31.08.2021 | 1354  | ORDINÆR_BARNETRYGD | 100     | 1354 |
      | 2       | 1            | 01.09.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     | 1654 |
      | 2       | 1            | 01.01.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |

      | 3       | 1            | 01.04.2015 | 28.02.2019 | 970   | ORDINÆR_BARNETRYGD | 100     | 970  |
      | 3       | 1            | 01.03.2019 | 31.08.2020 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.09.2020 | 28.02.2021 | 1354  | ORDINÆR_BARNETRYGD | 100     | 1354 |
      | 3       | 1            | 01.03.2021 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |

      | 3       | 2            | 01.04.2015 | 28.02.2019 | 970   | ORDINÆR_BARNETRYGD | 100     | 970  |
      | 3       | 2            | 01.03.2019 | 31.08.2020 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 2            | 01.09.2020 | 28.02.2021 | 1354  | ORDINÆR_BARNETRYGD | 100     | 1354 |
      | 3       | 2            | 01.03.2021 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |

      | 2       | 2            | 01.05.2017 | 28.02.2019 | 970   | ORDINÆR_BARNETRYGD | 100     | 970  |
      | 2       | 2            | 01.03.2019 | 31.08.2020 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.09.2020 | 31.08.2021 | 1354  | ORDINÆR_BARNETRYGD | 100     | 1354 |
      | 2       | 2            | 01.09.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     | 1654 |
      | 2       | 2            | 01.01.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak       | Prosent |
      | 3       | 2            | 01.02.2023 | 28.02.2023 | DELT_BOSTED | 100     |

    Når vedtaksperioder med begrunnelser genereres for behandling 2

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype |
      | 2023-02-01 | 2023-02-28 | UTBETALING         |
      | 2023-03-01 |            | OPPHØR             |

  Scenario: Avslag i behandlingen skal ikke påvirke endringstidspunktet
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende vedtak
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat          | Behandlingsårsak |
      | 1            | 1        |                     | ENDRET_OG_FORTSATT_INNVILGET | SØKNAD           |
      | 2            | 1        | 1                   | AVSLÅTT                      | SØKNAD           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 25.11.1987  |
      | 1            | 2       | BARN       | 19.06.2017  |
      | 2            | 1       | SØKER      | 25.11.1987  |
      | 2            | 2       | BARN       | 19.06.2017  |

    Og følgende dagens dato 20.09.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET                  |                  | 01.09.2020 | 15.05.2035 | OPPFYLT  | Nei                  |

      | 2       | UNDER_18_ÅR                                    |                  | 19.06.2017 | 18.06.2035 | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,GIFT_PARTNERSKAP |                  | 19.06.2017 |            | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER                                  |                  | 19.06.2017 | 31.08.2020 | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER                                  | DELT_BOSTED      | 01.09.2020 |            | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1       | UTVIDET_BARNETRYGD                             |                  |            |            | IKKE_OPPFYLT | Ja                   |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET                  |                  | 01.09.2020 | 15.05.2035 | OPPFYLT      | Nei                  |

      | 2       | BOR_MED_SØKER                                  |                  | 19.06.2017 | 31.08.2020 | OPPFYLT      | Nei                  |
      | 2       | LOVLIG_OPPHOLD,GIFT_PARTNERSKAP,BOSATT_I_RIKET |                  | 19.06.2017 |            | OPPFYLT      | Nei                  |
      | 2       | UNDER_18_ÅR                                    |                  | 19.06.2017 | 18.06.2035 | OPPFYLT      | Nei                  |
      | 2       | BOR_MED_SØKER                                  | DELT_BOSTED      | 01.09.2020 |            | OPPFYLT      | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.09.2020 | 30.09.2020 | 1354  | ORDINÆR_BARNETRYGD | 100     | 1354 |
      | 2       | 1            | 01.10.2020 | 31.05.2035 | 0     | ORDINÆR_BARNETRYGD | 0       | 1354 |

      | 2       | 2            | 01.09.2020 | 30.09.2020 | 1354  | ORDINÆR_BARNETRYGD | 100     | 1354 |
      | 2       | 2            | 01.10.2020 | 31.05.2035 | 0     | ORDINÆR_BARNETRYGD | 0       | 1354 |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak       | Prosent |
      | 2       | 1            | 01.10.2020 | 01.05.2035 | DELT_BOSTED | 0       |
      | 2       | 2            | 01.10.2020 | 01.05.2035 | DELT_BOSTED | 0       |

    Når vedtaksperioder med begrunnelser genereres for behandling 2

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato | Vedtaksperiodetype |
      | 01.06.2035 |          | OPPHØR             |
      |            |          | AVSLAG             |