# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder med kompetanser

  Bakgrunn:
    Gitt følgende vedtak
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234     | SØKER      | 11.01.1970  |
      | 1            | 3456     | BARN       | 13.04.2020  |

  Scenario: Skal lage vedtaksperioder for mor med ett barn med kompetanser
    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | BOR_MED_SØKER, GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat              |BehandlingId |
      | 3456     | 01.05.2020 | 30.04.2021 | NORGE_ER_PRIMÆRLAND   |1            |
      | 3456     | 01.05.2021 | 31.03.2038 | NORGE_ER_SEKUNDÆRLAND |1            |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456     | 01.05.2020 | 30.04.2021 | 1054  | 1            |
      | 3456     | 01.05.2021 | 31.03.2038 | 1354  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar     |
      | 01.05.2020 | 30.04.2021 | Utbetaling         | Barn og søker |
      | 01.05.2021 | 31.03.2038 | Utbetaling         | Barn og søker |
      | 01.04.2038 |            | Opphør             | Kun søker     |

  Scenario: Skal kunne ha kompetanse uten tom

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234     | SØKER      | 11.01.1970  |
      | 1            | 3456     | BARN       | 04.09.2020  |

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.10.1987 |            | Oppfylt  |
      | 3456    | BOSATT_I_RIKET, BOR_MED_SØKER, LOVLIG_OPPHOLD, GIFT_PARTNERSKAP | 04.09.2020 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 04.09.2020 | 03.09.2038 | Oppfylt  |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId |
      | 3456    | 01.10.2020 |          | NORGE_ER_SEKUNDÆRLAND | 1            |


    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.10.2020 | 31.08.2038 | 0     | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar     |
      | 01.10.2020 | 31.08.2038 | Utbetaling         | Barn og søker |
      | 01.09.2038 |            | Opphør             | Barn over 18  |
