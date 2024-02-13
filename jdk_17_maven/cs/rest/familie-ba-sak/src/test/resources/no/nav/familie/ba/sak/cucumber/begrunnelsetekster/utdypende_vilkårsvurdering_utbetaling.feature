# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for utdypende vilkårsvurdering med utbetaling

  Bakgrunn:
    Gitt følgende behandling
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 13.04.2020  |

  Scenario: Skal gi riktige begrunnelser for delt bosted med og uten deling
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat | Utdypende vilkår            |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 11.01.1970 |            | Oppfylt  |                             |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |                             |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |                             |
      | 3456    | BOR_MED_SØKER                                    | 13.04.2020 | 10.03.2021 | Oppfylt  | DELT_BOSTED                 |
      | 3456    | BOR_MED_SØKER                                    | 11.03.2021 | 13.04.2022 | Oppfylt  | DELT_BOSTED_SKAL_IKKE_DELES |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2021 | 500   | 1            |
      | 3456    | 01.04.2021 | 31.05.2022 | 1354  | 1            |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Inkluderte Begrunnelser                                       | Ekskluderte Begrunnelser |
      | 01.05.2020 | 31.03.2021 | UTBETALING         | INNVILGET_AVTALE_DELT_BOSTED_FÅR_FRA_FLYTTETIDSPUNKT          |                          |
      | 01.04.2021 | 31.04.2022 | UTBETALING         | INNVILGET_ANNEN_FORELDER_IKKE_SØKT_DELT_BARNETRYGD_ALLE_BARNA |                          |
      | 01.05.2022 |            | OPPHØR             | OPPHØR_FAST_BOSTED_AVTALE                                     |                          |

  Scenario: Skal gi riktige begrunnelser for utdypende vilkår vurdering annet grunnlag
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                          | Fra dato   | Til dato   | Resultat | Utdypende vilkår         |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                  | 11.01.1970 |            | Oppfylt  |                          |
      | 3456    | UNDER_18_ÅR                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |                          |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, BOR_MED_SØKER | 13.04.2020 |            | Oppfylt  |                          |
      | 3456    | LOVLIG_OPPHOLD                                  | 13.04.2020 | 10.03.2021 | Oppfylt  | VURDERING_ANNET_GRUNNLAG |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2021 | 1354  | 1            |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Inkluderte Begrunnelser                           | Ekskluderte Begrunnelser                    |
      | 01.05.2020 | 31.03.2021 | UTBETALING         | INNVILGET_BOSATT_I_RIKTET                         | INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE |
      | 01.04.2021 |            | OPPHØR             | OPPHØR_IKKE_OPPHOLDSTILLATELSE_MER_ENN_12_MÅNEDER |                                             |

  Scenario: Skal detektere endring i utdypende vilkår
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat | Utdypende vilkår         |
      | 1234    | LOVLIG_OPPHOLD                                                  | 11.01.1970 |            | Oppfylt  |                          |
      | 1234    | BOSATT_I_RIKET                                                  | 11.01.1970 | 10.03.2022 | Oppfylt  | VURDERING_ANNET_GRUNNLAG |

      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |                          |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2020 |            | Oppfylt  |                          |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2022 | 1354  | 1            |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Inkluderte Begrunnelser    | Ekskluderte Begrunnelser |
      | 01.05.2020 | 31.03.2022 | UTBETALING         |                            |                          |
      | 01.04.2022 |            | OPPHØR             | OPPHØR_UGYLDIG_KONTONUMMER |                          |



