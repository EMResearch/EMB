# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder for institusjonssaker

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype  |
      | 1        | INSTITUSJON |

    Gitt følgende vedtak
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | IKKE_VURDERT        | SØKNAD           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | BARN       | 10.01.2018  |

  Scenario: Skal kunne endre utbetalingen til 0 prosent
    Og følgende dagens dato 26.09.2023
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | UNDER_18_ÅR                                                  |                  | 10.01.2018 | 09.01.2036 | OPPFYLT  | Nei                  |
      | 1       | GIFT_PARTNERSKAP,BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER |                  | 15.08.2020 | 15.10.2020 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.09.2020 | 30.09.2020 | 0     | ORDINÆR_BARNETRYGD | 0       | 970  |
      | 1       | 1            | 01.10.2020 | 31.10.2020 | 1354  | ORDINÆR_BARNETRYGD | 100     | 1354 |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent |
      | 1       | 1            | 01.09.2018 | 30.09.2020 | ETTERBETALING_3ÅR | 0       |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar          |
      | 01.09.2020 | 30.09.2020 | OPPHØR             | Etterbetaling 3 år |
      | 01.10.2020 | 31.10.2020 | UTBETALING         |                    |
      | 01.11.2020 |            | OPPHØR             |                    |