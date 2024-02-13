# language: no
# encoding: UTF-8

Egenskap: Vedtaksperiode for behandling som opphører perioder fra forrige behandling

  Bakgrunn:
    Gitt følgende vedtak
      | BehandlingId | ForrigeBehandlingId |
      | 1            |                     |
      | 2            | 1                   |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1,2          | 1234    | SØKER      | 11.01.1970  |
      | 1,2          | 3456    | BARN       | 13.04.2020  |

  Scenario: Skal lage vedtaksperioder for revurdering mot forrige behandling hvor det viser seg at barnet ikke bodde hos mor det første året.
    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | BOR_MED_SØKER, GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |

    Og lag personresultater for behandling 2
    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 12.01.2021 |            | Oppfylt  |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2038 | 1354  | 1            |
      | 3456    | 01.02.2021 | 31.03.2038 | 1354  | 2            |

    Når vedtaksperioder med begrunnelser genereres for behandling 2

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar                                                |
      | 01.05.2020 | 31.01.2021 | Opphør             | Barnetrygd for Barn 3456 opphører fra forrige behandling |
      | 01.02.2021 | 31.03.2038 | Utbetaling         | Barn og søker                                            |
      | 01.04.2038 |            | Opphør             | Kun søker                                                |

  Scenario: Skal lage vedtaksperioder for revurdering mot forrige behandling hvor ett barn mister andel i perioden

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1,2          | 1234    | SØKER      | 11.01.1970  |
      | 1,2          | 3456    | BARN       | 13.04.2020  |
      | 1,2          | 5678    | BARN       | 13.04.2021  |

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | BOR_MED_SØKER, GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |
      | 5678    | UNDER_18_ÅR                                                     | 13.04.2021 | 12.04.2039 | Oppfylt  |
      | 5678    | BOR_MED_SØKER, GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2021 |            | Oppfylt  |

    Og lag personresultater for behandling 2
    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2020 |            | Oppfylt  |
      | 5678    | UNDER_18_ÅR                                                     | 13.04.2021 | 12.04.2039 | Oppfylt  |
      | 5678    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD                | 13.04.2021 |            | Oppfylt  |
      | 5678    | BOR_MED_SØKER                                                   | 12.01.2022 |            | Oppfylt  |


    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2038 | 1354  | 1            |
      | 3456    | 01.05.2020 | 31.03.2038 | 1354  | 2            |
      | 5678    | 01.05.2021 | 31.03.2039 | 1354  | 1            |
      | 5678    | 01.02.2022 | 31.03.2039 | 1354  | 2            |

    Og med overstyrt endringstidspunkt
      | Endringstidspunkt | BehandlingId |
      | 01.01.2021        | 2            |

    Når vedtaksperioder med begrunnelser genereres for behandling 2

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype                                      | Kommentar                                                                                                                          |
      | 01.05.2020 | 30.04.2021 | Utbetaling                                              | Barn 3456 og søker har ordinære vilkår oppfylt                                                                                     |
      | 01.05.2021 | 31.01.2022 | UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING | Reduksjon. Barn 5678 mister utbetaling fra forrige behandling. TODO: Typen skal være Utbetaling når ny begrunnelsesløsning er inne |
      | 01.02.2022 | 31.03.2038 | Utbetaling                                              | Utbetaling begge barn                                                                                                              |
      | 01.04.2038 | 31.03.2039 | Utbetaling                                              | Kun barn 5678                                                                                                                      |
      | 01.04.2039 |            | Opphør                                                  | Kun søker har vilkår oppfylt                                                                                                       |

  Scenario: Skal lage vedtaksperioder for revurdering mot forrige behandling hvor gjeldende behandling har opphør av flere grunner enn forrige.
    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat     |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 11.01.1970 |            | Oppfylt      |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt      |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt      |
      | 3456    | BOR_MED_SØKER                                    | 13.04.2020 | 31.12.2020 | Oppfylt      |

      | 3456    | BOR_MED_SØKER                                    | 01.01.2021 | 31.12.2021 | ikke_oppfylt |
      | 3456    | BOR_MED_SØKER                                    | 01.01.2022 |            | Oppfylt      |

    Og lag personresultater for behandling 2
    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                           | Fra dato   | Til dato   | Resultat     |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD   | 11.01.1970 |            | Oppfylt      |
      | 3456    | UNDER_18_ÅR                      | 13.04.2020 | 12.04.2038 | Oppfylt      |
      | 3456    | GIFT_PARTNERSKAP, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt      |
      | 3456    | BOSATT_I_RIKET                   | 13.04.2020 | 15.07.2021 | Oppfylt      |
      | 3456    | BOR_MED_SØKER                    | 13.04.2020 | 31.12.2020 | Oppfylt      |

      | 3456    | BOR_MED_SØKER                    | 01.01.2021 | 31.12.2021 | ikke_oppfylt |
      | 3456    | BOSATT_I_RIKET                   | 16.07.2021 | 31.12.2021 | ikke_oppfylt |
      | 3456    | BOSATT_I_RIKET                   | 01.01.2022 |            | Oppfylt      |
      | 3456    | BOR_MED_SØKER                    | 01.01.2022 |            | Oppfylt      |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.12.2020 | 1354  | 1            |
      | 3456    | 01.02.2022 | 31.03.2038 | 1354  | 1            |
      | 3456    | 01.05.2020 | 31.12.2020 | 1354  | 2            |
      | 3456    | 01.02.2022 | 31.03.2038 | 1354  | 2            |

    Og med overstyrt endringstidspunkt
      | Endringstidspunkt | BehandlingId |
      | 01.01.2020        | 2            |

    Når vedtaksperioder med begrunnelser genereres for behandling 2

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar     |
      | 01.05.2020 | 31.12.2020 | Utbetaling         |               |
      | 01.01.2021 | 31.01.2022 | Opphør             | Kun søker     |
      | 01.02.2022 | 31.03.2038 | Utbetaling         | Barn og søker |
      | 01.04.2038 |            | Opphør             | Kun søker     |

