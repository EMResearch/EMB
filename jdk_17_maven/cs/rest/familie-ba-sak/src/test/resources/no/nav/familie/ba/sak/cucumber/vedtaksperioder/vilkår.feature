# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder ved endring av vilkår for mor og et barn

  Bakgrunn:
    Gitt følgende vedtak
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 13.04.2020  |

  Scenario: Skal lage vedtaksperioder for mor med ett barn med vilkår
    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234    | LOVLIG_OPPHOLD                                   | 11.01.1970 |            | Oppfylt  |
      | 1234    | BOSATT_I_RIKET                                   | 11.01.1970 | 01.01.2021 | Oppfylt  |
      | 1234    | BOSATT_I_RIKET                                   | 02.01.2021 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 13.04.2020 | 01.03.2021 | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 02.03.2021 |            | Oppfylt  |


    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2038 | 1354  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar     |
      | 01.05.2020 | 31.01.2021 | Utbetaling         | Barn og søker |
      | 01.02.2021 | 31.03.2021 | Utbetaling         | Barn og søker |
      | 01.04.2021 | 31.03.2038 | Utbetaling         | Barn og søker |
      | 01.04.2038 |            | Opphør             | Kun søker     |


  Scenario: Skal lage vedtaksperioder når det er generelt avslag som overlapper med oppfylt periode
    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET                                                  | 11.01.1970 |            | Oppfylt      |                      |
      | 1234    | LOVLIG_OPPHOLD                                                  | 11.01.2020 | 05.05.2022 | Oppfylt      |                      |
      | 1234    | LOVLIG_OPPHOLD                                                  |            |            | ikke_oppfylt | Ja                   |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt      |                      |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2020 |            | Oppfylt      |                      |


    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 1234    | 01.05.2020 | 31.03.2038 | 1354  | 1            |
      | 3456    | 01.05.2020 | 31.03.2038 | 1354  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar                         |
      | 01.05.2020 | 31.05.2022 | Utbetaling         | Barn og søker                     |
      | 01.06.2022 |            | Opphør             | Lovlig opphold opphører for søker |
      |            |            | Avslag             | Generelt avslag lovlig opphold    |


  Scenario: Skal lage vedtaksperioder når det er åpent avslag på bor med søker samtidig som oppfylt
    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt      |                      |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt      |                      |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.01.2020 |            | Oppfylt      |                      |
      | 3456    | BOR_MED_SØKER                                                   |            |            | ikke_oppfylt | Ja                   |


    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 1234    | 01.01.2020 | 31.03.2038 | 1354  | 1            |
      | 3456    | 01.01.2020 | 31.03.2038 | 1354  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar     |
      | 01.05.2020 | 31.03.2038 | Utbetaling         | Barn og søker |
      | 01.04.2038 |            | Opphør             | Barn og søker |
      |            |            | Avslag             | Barn og søker |

  Scenario: Skal lage vedtaksperioder for mor med ett barn med vilkår - barn flytter til søker etter 1 år

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 20.08.2021 |            | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2038 | 1354  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar     |
      | 01.09.2021 | 31.03.2038 | Utbetaling         | Barn og søker |
      | 01.04.2038 |            | Opphør             | Kun søker     |


  Scenario: Skal lage vedtaksperioder for mor med ett barn med vilkår - barn har vilkår fra tidenes morgen

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat     |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 11.01.1970 |            | Oppfylt      |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt      |
      | 3456    | BOR_MED_SØKER                                    |            |            | Ikke_oppfylt |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt      |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2038 | 1354  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato | Til dato | Vedtaksperiodetype | Kommentar |

  Scenario: Skal lage vedtaksperioder med begrunnelser for mor med vilkår når barnet flytter ut

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 13.04.2020 | 21.07.2029 | Oppfylt  |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2038 | 1354  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar                                                       |
      | 01.05.2020 | 31.07.2029 | Utbetaling         | Barn og søker                                                   |
      | 01.08.2029 |            | Opphør             | Barn har oppfylte vilkår, men ett som ikke oppfylles i perioden |


  Scenario: Skal lage vedtaksperioder med begrunnelser for mor med vilkår når barnet flytter ut og inn igjen

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat     |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 11.01.1970 |            | Oppfylt      |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt      |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt      |
      | 3456    | BOR_MED_SØKER                                    | 13.04.2020 | 21.07.2029 | Oppfylt      |
      | 3456    | BOR_MED_SØKER                                    | 22.07.2029 | 16.05.2030 | Ikke_oppfylt |
      | 3456    | BOR_MED_SØKER                                    | 17.05.2030 |            | Oppfylt      |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2038 | 1354  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar     |
      | 01.05.2020 | 31.07.2029 | Utbetaling         | Barn og søker |
      | 01.08.2029 | 31.05.2030 | Opphør             | Kun søker     |
      | 01.06.2030 | 31.03.2038 | Utbetaling         | Barn og søker |
      | 01.04.2038 |            | Opphør             | Kun søker     |


  Scenario: Skal ikke lage opphør på mor når det kun er opphør på barn

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                           | Fra dato   | Til dato   | Resultat     |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD   | 11.01.1970 |            | Oppfylt      |
      | 3456    | UNDER_18_ÅR                      | 13.04.2020 | 12.04.2038 | Oppfylt      |
      | 3456    | GIFT_PARTNERSKAP, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt      |
      | 3456    | BOR_MED_SØKER                    | 13.04.2020 | 21.07.2021 | Oppfylt      |
      | 3456    | BOSATT_I_RIKET                   | 13.04.2020 | 21.07.2022 | Oppfylt      |
      | 3456    | BOR_MED_SØKER                    | 22.07.2021 | 16.05.2030 | Ikke_oppfylt |
      | 3456    | BOSATT_I_RIKET                   | 22.07.2022 | 16.05.2030 | Ikke_oppfylt |
      | 3456    | BOR_MED_SØKER                    | 17.05.2030 |            | Oppfylt      |
      | 3456    | BOSATT_I_RIKET                   | 17.05.2030 |            | Oppfylt      |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2038 | 1354  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar     |
      | 01.05.2020 | 31.07.2021 | Utbetaling         | Barn og søker |
      | 01.08.2021 | 31.05.2030 | Opphør             | Kun søker     |
      | 01.06.2030 | 31.03.2038 | Utbetaling         | Barn og søker |
      | 01.04.2038 |            | Opphør             | Kun søker     |


  Scenario: Skal lage opphør på mor når det kun er opphør i utvidet

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                           | Fra dato   | Til dato   | Resultat     |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD   | 11.01.1970 |            | Oppfylt      |
      | 3456    | UNDER_18_ÅR                      | 13.04.2020 | 12.04.2038 | Oppfylt      |
      | 3456    | GIFT_PARTNERSKAP, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt      |
      | 3456    | BOR_MED_SØKER                    | 13.04.2020 | 21.07.2021 | Oppfylt      |
      | 3456    | BOSATT_I_RIKET                   | 13.04.2020 | 21.07.2022 | Oppfylt      |
      | 3456    | BOR_MED_SØKER                    | 22.07.2021 | 16.05.2030 | Ikke_oppfylt |
      | 3456    | BOSATT_I_RIKET                   | 22.07.2022 | 16.05.2030 | Ikke_oppfylt |
      | 3456    | BOR_MED_SØKER                    | 17.05.2030 |            | Oppfylt      |
      | 3456    | BOSATT_I_RIKET                   | 17.05.2030 |            | Oppfylt      |
      | 1234    | UTVIDET_BARNETRYGD               | 13.04.2020 | 16.02.2021 | Oppfylt      |
      | 1234    | UTVIDET_BARNETRYGD               | 17.02.2021 | 16.05.2030 | Ikke_oppfylt |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 1234    | 01.05.2020 | 01.03.2021 | 678   | 1            |
      | 3456    | 01.05.2020 | 31.07.2021 | 1245  | 1            |
      | 3456    | 01.06.2030 | 31.03.2038 | 1245  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar                             |
      | 01.05.2020 | 28.02.2021 | Utbetaling         | Barn og søker. Søker har utvidet      |
      | 01.03.2021 | 31.07.2021 | Utbetaling         | Barn og søker. Søker har ikke utvidet |
      | 01.08.2021 | 31.05.2030 | Opphør             | Opphør barn. Bor ikke med søker       |
      | 01.06.2030 | 31.03.2038 | Utbetaling         | Barn og søker                         |
      | 01.04.2038 |            | Opphør             | Kun søker                             |


  Scenario: Skal gi opphør i periode barn ikke har lovlig opphold selv om mor har lovlig opphold

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                  | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, BOR_MED_SØKER | 13.04.2020 |            | Oppfylt  |
      | 3456    | LOVLIG_OPPHOLD                                  | 13.04.2020 | 21.07.2021 | Oppfylt  |
      | 3456    | LOVLIG_OPPHOLD                                  | 17.05.2023 |            | Oppfylt  |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.07.2021 | 1245  | 1            |
      | 3456    | 01.06.2023 | 31.03.2038 | 1245  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar     |
      | 01.05.2020 | 31.07.2021 | Utbetaling         | Barn og søker |
      | 01.08.2021 | 31.05.2023 | Opphør             | Kun søker     |
      | 01.06.2023 | 31.03.2038 | Utbetaling         | Barn og søker |
      | 01.04.2038 |            | Opphør             | Kun søker     |


  Scenario: Skal kun gi utbetalingsperioder for utvidet om både søker og ett barn har oppfylt de ordinære vilkårene

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 | 13.04.2021 | Oppfylt  |
      | 1234    | UTVIDET_BARNETRYGD                                              | 13.04.2020 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, BOR_MED_SØKER, LOVLIG_OPPHOLD | 13.04.2020 | 13.04.2022 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, BOR_MED_SØKER, LOVLIG_OPPHOLD | 01.01.2030 |            | Oppfylt  |
      | 1234    | BOSATT_I_RIKET                                                  | 13.04.2022 |            | Oppfylt  |
      | 1234    | LOVLIG_OPPHOLD                                                  | 13.04.2022 |            | Oppfylt  |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 1234    | 01.05.2020 | 30.04.2021 | 678   | 1            |
      | 1234    | 01.01.2030 | 30.04.2038 | 678   | 1            |
      | 3456    | 01.05.2020 | 30.04.2021 | 1245  | 1            |
      | 3456    | 01.01.2030 | 30.04.2038 | 1245  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar                        |
      | 01.05.2020 | 30.04.2021 | Utbetaling         | Barn og søker                    |
      | 01.02.2030 | 31.03.2038 | Utbetaling         | Barn og søker                    |
      | 01.05.2021 | 31.01.2030 | Opphør             | Søker har ikke oppfylt vilkårene |
      | 01.04.2038 |            | Opphør             |                                  |

  Scenario: Skal ikke dra med splitter fra forrige behandling inn i behandlingen
    Gitt følgende vedtak
      | BehandlingId | ForrigeBehandlingId |
      | 1            |                     |
      | 2            | 1                   |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1, 2         | 1234    | SØKER      | 13.07.1987  |
      | 1, 2         | 3456    | BARN       | 26.01.2021  |

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 13.07.1987 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 26.01.2021 | 25.01.2039 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOR_MED_SØKER, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 26.01.2021 |            | Oppfylt  |

    Og lag personresultater for behandling 2
    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET                                   | 13.07.1987 |            | Oppfylt  |
      | 1234    | LOVLIG_OPPHOLD                                   | 13.07.1987 | 09.01.2023 | Oppfylt  |
      | 1234    | LOVLIG_OPPHOLD                                   | 30.03.2023 |            | Oppfylt  |

      | 3456    | UNDER_18_ÅR                                      | 26.01.2021 | 25.01.2039 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 26.01.2021 |            | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 26.01.2021 | 21.03.2023 | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 05.01.2030 |            | Oppfylt  |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.02.2021 | 31.08.2021 | 1354  | 1            |
      | 3456    | 01.09.2021 | 31.12.2021 | 1654  | 1            |
      | 3456    | 01.01.2022 | 28.02.2023 | 1676  | 1            |
      | 3456    | 01.03.2023 | 30.02.2039 | 1723  | 1            |

      | 3456    | 01.02.2021 | 31.08.2021 | 1354  | 2            |
      | 3456    | 01.09.2021 | 31.12.2021 | 1654  | 2            |
      | 3456    | 01.01.2022 | 31.03.2023 | 1676  | 2            |
      | 3456    | 01.01.2030 | 30.02.2039 | 1676  | 2            |

    Og med overstyrt endringstidspunkt
      | Endringstidspunkt | BehandlingId |
      | 01.01.2021        | 2            |

    Når vedtaksperioder med begrunnelser genereres for behandling 2

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype |
      | 2021-02-01 | 2021-08-31 | UTBETALING         |
      | 2021-09-01 | 2021-12-31 | UTBETALING         |
      | 2022-01-01 | 2023-01-31 | UTBETALING         |
      | 2030-02-01 | 2038-12-31 | UTBETALING         |
      | 2023-02-01 | 2030-01-31 | OPPHØR             |
      | 2039-01-01 |            | OPPHØR             |


  Scenario: Skal lage periode selv om det ikke finnes barn når det er eksplisitt avslag på søker
    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | LOVLIG_OPPHOLD                                                  | 11.01.1970 | 14.08.2022 | Oppfylt      |                      |
      | 1234    | BOSATT_I_RIKET                                                  | 11.01.1970 |            | Oppfylt      |                      |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt      |                      |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2020 |            | Oppfylt      |                      |
      | 1234    | UTVIDET_BARNETRYGD                                              |            |            | Ikke_oppfylt | Ja                   |
      | 1234    | LOVLIG_OPPHOLD                                                  | 15.08.2022 | 02.02.2023 | Ikke_oppfylt | Ja                   |
      | 1234    | LOVLIG_OPPHOLD                                                  | 03.02.2023 |            | Oppfylt      |                      |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.08.2022 | 1354  | 1            |
      | 3456    | 01.03.2023 | 31.03.2038 | 1354  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar                                 |
      |            |            | Avslag             | Søker avslag utvidet                      |
      | 01.05.2020 | 31.08.2022 | Utbetaling         | Barn og søker har ordinære vilkår oppfylt |
      | 01.09.2022 | 28.02.2023 | Avslag             | Søker avslag utvidet og lovlig opphold    |
      | 01.03.2023 | 31.03.2038 | Utbetaling         | Barn og søker har ordinære vilkår oppfylt |
      | 01.04.2038 |            | Opphør             | Barn over 18                              |




