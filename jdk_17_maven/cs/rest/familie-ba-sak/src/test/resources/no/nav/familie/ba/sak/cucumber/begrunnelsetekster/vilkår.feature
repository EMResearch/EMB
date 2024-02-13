# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser ved endring av vilkår

  Bakgrunn:
    Gitt følgende behandling
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 13.04.2020  |

  Scenario: Skal lage vedtaksperioder for mor med et barn med vilkår
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 13.04.2020 | 10.03.2021 | Oppfylt  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2021 | 1354  | 1            |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Inkluderte Begrunnelser       | Ekskluderte Begrunnelser |
      | 01.05.2020 | 31.03.2021 | UTBETALING         |                               |                          |
      | 01.04.2021 |            | OPPHØR             | OPPHØR_BARN_FLYTTET_FRA_SØKER |                          |

  Scenario: Søker får lovlig opphold etter barn
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 15.01.2021 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 13.04.2020 | 10.03.2021 | Oppfylt  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.02.2021 | 31.03.2021 | 1354  | 1            |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Inkluderte Begrunnelser                  | Ekskluderte Begrunnelser |
      | 01.02.2021 | 31.03.2021 | UTBETALING         | INNVILGET_BOSATT_I_RIKTET_LOVLIG_OPPHOLD |                          |
      | 01.04.2021 |            | OPPHØR             | OPPHØR_BARN_FLYTTET_FRA_SØKER            |                          |

  Scenario: Bor med søker for barn er eneste utgjørende vilkår skal kun gi bor med søker begrunnelser
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 15.01.1990 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 13.04.2021 | 10.03.2022 | Oppfylt  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2021 | 31.03.2022 | 1354  | 1            |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Inkluderte Begrunnelser       | Ekskluderte Begrunnelser      |
      | 01.05.2021 | 31.03.2022 | UTBETALING         | INNVILGET_BOR_HOS_SØKER       | REDUKSJON_IKKE_BOSATT_I_NORGE |
      | 01.04.2022 |            | OPPHØR             | OPPHØR_BARN_FLYTTET_FRA_SØKER |                               |

  Scenario: Skal ikke gi reduksjonsbegrunnelse når det er innvilgelse
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 15.01.1990 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 13.04.2021 | 10.03.2022 | Oppfylt  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2021 | 31.03.2022 | 1354  | 1            |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Inkluderte Begrunnelser       | Ekskluderte Begrunnelser |
      | 01.05.2021 | 31.03.2022 | UTBETALING         | INNVILGET_BOR_HOS_SØKER       | REDUKSJON_FLYTTET_BARN   |
      | 01.04.2022 |            | OPPHØR             | OPPHØR_BARN_FLYTTET_FRA_SØKER |                          |

  Scenario: Skal ikke gi innvilgettekster for mistede vilkår
    Gitt følgende behandling
      | BehandlingId | FagsakId  | ForrigeBehandlingId |
      | 100173051    | 200055501 |                     |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId       | Persontype | Fødselsdato |
      | 100173051    | 2276892299373 | SØKER      | 18.10.1984  |
      | 100173051    | 2799787304865 | BARN       | 02.02.2015  |

    Og lag personresultater for begrunnelse for behandling 100173051

    Og legg til nye vilkårresultater for begrunnelse for behandling 100173051
      | AktørId       | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2799787304865 | UNDER_18_ÅR                                    |                  | 02.02.2015 | 01.02.2033 | OPPFYLT  | Nei                  |
      | 2799787304865 | LOVLIG_OPPHOLD,BOSATT_I_RIKET,GIFT_PARTNERSKAP |                  | 02.02.2015 |            | OPPFYLT  | Nei                  |
      | 2799787304865 | BOR_MED_SØKER                                  |                  | 15.10.2022 |            | OPPFYLT  | Nei                  |

      | 2276892299373 | LOVLIG_OPPHOLD,BOSATT_I_RIKET                  |                  | 18.10.1984 |            | OPPFYLT  | Nei                  |
      | 2276892299373 | UTVIDET_BARNETRYGD                             |                  | 15.01.2023 | 15.05.2023 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId       | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 2799787304865 | 100173051    | 01.11.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     |
      | 2799787304865 | 100173051    | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     |
      | 2799787304865 | 100173051    | 01.07.2023 | 31.01.2033 | 1310  | ORDINÆR_BARNETRYGD | 100     |
      | 2276892299373 | 100173051    | 01.02.2023 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     |
      | 2276892299373 | 100173051    | 01.03.2023 | 31.05.2023 | 2489  | UTVIDET_BARNETRYGD | 100     |

    Når begrunnelsetekster genereres for behandling 100173051

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser | Ekskluderte Begrunnelser           |
      | 01.11.2022 | 31.01.2023 | UTBETALING         |           |                         |                                    |
      | 01.02.2023 | 28.02.2023 | UTBETALING         |           |                         |                                    |
      | 01.03.2023 | 31.05.2023 | UTBETALING         |           |                         |                                    |
      | 01.06.2023 | 30.06.2023 | UTBETALING         |           |                         | INNVILGET_FLYTTET_ETTER_SEPARASJON |
      | 01.07.2023 | 31.01.2033 | UTBETALING         |           |                         |                                    |
      | 01.02.2033 |            | OPPHØR             |           |                         |                                    |

  Scenario: Skal gå ok når søker sine vilkår endrer seg etter opphør
    Gitt følgende behandling
      | BehandlingId | FagsakId  | ForrigeBehandlingId |
      | 100173207    | 200055651 |                     |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId       | Persontype | Fødselsdato |
      | 100173207    | 2005858678161 | BARN       | 02.02.2015  |
      | 100173207    | 2305793738737 | SØKER      | 12.11.1984  |

    Og lag personresultater for begrunnelse for behandling 100173207

    Og legg til nye vilkårresultater for begrunnelse for behandling 100173207
      | AktørId       | Vilkår                          | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2005858678161 | BOSATT_I_RIKET                  | BARN_BOR_I_NORGE             | 02.02.2015 |            | OPPFYLT  | Nei                  |
      | 2005858678161 | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD |                              | 02.02.2015 |            | OPPFYLT  | Nei                  |
      | 2005858678161 | UNDER_18_ÅR                     |                              | 02.02.2015 | 01.02.2033 | OPPFYLT  | Nei                  |
      | 2005858678161 | BOR_MED_SØKER                   | BARN_BOR_I_EØS_MED_SØKER     | 02.02.2015 |            | OPPFYLT  | Nei                  |

      | 2305793738737 | LOVLIG_OPPHOLD                  |                              | 12.11.1984 |            | OPPFYLT  | Nei                  |
      | 2305793738737 | BOSATT_I_RIKET                  | OMFATTET_AV_NORSK_LOVGIVNING | 15.03.2023 | 15.08.2023 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId       | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 2005858678161 | 100173207    | 01.04.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     |
      | 2005858678161 | 100173207    | 01.07.2023 | 31.08.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     |

    Når begrunnelsetekster genereres for behandling 100173207

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser | Ekskluderte Begrunnelser |
      | 01.04.2023 | 30.06.2023 | UTBETALING         |           |                         |                          |
      | 01.07.2023 | 31.08.2023 | UTBETALING         |           |                         |                          |
      | 01.09.2023 |            | OPPHØR             |           |                         |                          |

  Scenario: Skal vise begrunnelse når vi aktivt lager en splitt i vilkåret
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat  | Behandlingsårsak |
      | 1            | 1        |                     | INNVILGET_OG_OPPHØRT | SØKNAD           |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | BARN       | 07.03.2016  |
      | 1            | 2       | SØKER      | 14.02.1972  |

    Og følgende dagens dato 27.09.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                       | Fra dato   | Til dato   | Resultat |
      | 1       | UNDER_18_ÅR                                                  | 07.03.2016 | 06.03.2034 | OPPFYLT  |
      | 1       | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER | 07.03.2016 |            | OPPFYLT  |

      | 2       | LOVLIG_OPPHOLD                                               | 14.02.1972 |            | OPPFYLT  |
      | 2       | BOSATT_I_RIKET                                               | 15.12.2022 | 15.02.2023 | OPPFYLT  |
      | 2       | UTVIDET_BARNETRYGD                                           | 14.02.1972 | 14.01.2023 | OPPFYLT  |
      | 2       | UTVIDET_BARNETRYGD                                           | 15.01.2023 |            | OPPFYLT  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.01.2023 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.01.2023 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser      | Ekskluderte Begrunnelser |
      | 01.01.2023 | 31.01.2023 | UTBETALING         |           |                              |                          |
      | 01.02.2023 | 28.02.2023 | UTBETALING         |           | INNVILGET_BOR_ALENE_MED_BARN |                          |
      | 01.03.2023 |            | OPPHØR             |           |                              |                          |
