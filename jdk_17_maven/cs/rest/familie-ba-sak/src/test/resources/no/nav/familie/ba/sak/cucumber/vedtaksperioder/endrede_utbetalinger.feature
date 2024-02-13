# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder med endrede utbetalinger

  Bakgrunn:
    Gitt følgende vedtak
      | BehandlingId | ForrigeBehandlingId |
      | 1            |                     |
      | 2            | 1                   |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 13.04.2020  |

  Scenario: Skal lage vedtaksperioder for mor med ett barn med endrede utbetalinger
    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | BOR_MED_SØKER, GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |

    Og med endrede utbetalinger
      | AktørId | Fra dato   | Til dato   | BehandlingId |
      | 3456    | 01.05.2021 | 31.03.2038 | 1            |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 30.04.2021 | 1054  | 1            |
      | 3456    | 01.05.2021 | 31.03.2038 | 1354  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar     |
      | 01.05.2020 | 30.04.2021 | Utbetaling         | Barn og søker |
      | 01.05.2021 | 31.03.2038 | Utbetaling         | Barn og søker |
      | 01.04.2038 |            | Opphør             | Kun søker     |


  Scenario: Skal lage opphørsperiode når bor_med_søker-vilkåret ikke er oppfylt ved revurdering

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 15.12.1976  |
      | 1            | 3456    | BARN       | 16.06.2016  |
      | 1            | 5678    | BARN       | 11.09.2013  |
      | 2            | 1234    | SØKER      | 15.12.1976  |
      | 2            | 3456    | BARN       | 16.06.2016  |
      | 2            | 5678    | BARN       | 11.09.2013  |

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 15.12.1976 |            | Oppfylt  |
      | 1234    | UTVIDET_BARNETRYGD                               | 31.05.2022 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                      | 16.06.2016 | 15.06.2034 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 16.06.2016 |            | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 08.12.2021 |            | Oppfylt  |
      | 5678    | BOSATT_I_RIKET, LOVLIG_OPPHOLD, GIFT_PARTNERSKAP | 11.09.2013 |            | Oppfylt  |
      | 5678    | BOR_MED_SØKER                                    | 08.12.2021 |            | Oppfylt  |
      | 5678    | UNDER_18_ÅR                                      | 11.09.2013 | 10.09.2031 | Oppfylt  |

    Og lag personresultater for behandling 2
    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat     |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 15.12.1976 |            | Oppfylt      |
      | 1234    | UTVIDET_BARNETRYGD                               | 31.05.2022 |            | Oppfylt      |
      | 3456    | UNDER_18_ÅR                                      | 16.06.2016 | 15.06.2034 | Oppfylt      |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 16.06.2016 |            | Oppfylt      |
      | 3456    | BOR_MED_SØKER                                    | 08.12.2021 |            | IKKE_OPPFYLT |
      | 5678    | BOSATT_I_RIKET, LOVLIG_OPPHOLD, GIFT_PARTNERSKAP | 11.09.2013 |            | Oppfylt      |
      | 5678    | BOR_MED_SØKER                                    | 08.12.2021 |            | IKKE_OPPFYLT |
      | 5678    | UNDER_18_ÅR                                      | 11.09.2013 | 10.09.2031 | Oppfylt      |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 1234    | 31.05.2022 | 31.06.2034 | 1354  | 1            |
      | 3456    | 08.12.2021 | 31.06.2034 | 1354  | 1            |
      | 3456    | 08.12.2021 | 31.06.2034 | 1354  | 2            |
      | 5678    | 08.12.2021 | 31.10.2031 | 1354  | 1            |
      | 5678    | 08.12.2021 | 31.10.2031 | 1354  | 2            |

    Når vedtaksperioder med begrunnelser genereres for behandling 2

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar |
      | 01.01.2022 |          | Opphør             | Kun søker |


  Scenario: Skal ta med etterfølgende perioder når den første endres

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 09.08.1991  |
      | 1            | 3456    | BARN       | 31.10.2015  |
      | 2            | 1234    | SØKER      | 09.08.1991  |
      | 2            | 3456    | BARN       | 31.10.2015  |

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 09.08.1991 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                      | 31.10.2015 | 30.10.2033 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 31.10.2015 |            | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 04.05.2021 | 02.03.2023 | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 03.03.2023 |            | Oppfylt  |


    Og lag personresultater for behandling 2
    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat     |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 09.08.1991 |            | Oppfylt      |
      | 3456    | UNDER_18_ÅR                                      | 31.10.2015 | 30.10.2033 | Oppfylt      |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 31.10.2015 |            | Oppfylt      |
      | 3456    | BOR_MED_SØKER                                    | 04.05.2021 | 02.03.2023 | IKKE_OPPFYLT |
      | 3456    | BOR_MED_SØKER                                    | 03.03.2023 |            | Oppfylt      |


    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 1234    | 31.05.2021 | 30.10.2033 | 1354  | 1            |
      | 3456    | 31.05.2021 | 30.10.2033 | 1354  | 1            |
      | 3456    | 31.04.2023 | 30.10.2033 | 1354  | 2            |

    Når vedtaksperioder med begrunnelser genereres for behandling 2

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar                                              |
      | 01.06.2021 | 31.03.2023 | Opphør             | Barnet bodde ikke hos søker i denne perioden allikevel |
      | 01.04.2023 | 30.09.2033 | Utbetaling         |                                                        |
      | 01.10.2033 |            | Opphør             | Over 18                                                |

