# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for opphør fra forrige behandling

  Bakgrunn:
    Gitt følgende behandling
      | BehandlingId | FagsakId  | ForrigeBehandlingId |
      | 1            | 1         |                     |
      | 2            | 1         | 1                   |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId       | Persontype | Fødselsdato |
      | 1            | 1234          | SØKER      | 16.09.1984  |
      | 1            | 3456          | BARN       | 07.09.2019  |
      | 2            | 1234          | SØKER      | 16.09.1984  |
      | 2            | 3456          | BARN       | 07.09.2019  |

  Scenario: Skal gi opphør fra forrige behandling-begrunnelser knyttet til bor med søker, men ikke delt bosted
    Og lag personresultater for begrunnelse for behandling 1
    Og lag personresultater for begrunnelse for behandling 2

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | LOVLIG_OPPHOLD,BOSATT_I_RIKET                  |                  | 16.09.1984 |            | OPPFYLT  | Nei                  |

      | 3456    | LOVLIG_OPPHOLD,BOSATT_I_RIKET,GIFT_PARTNERSKAP |                  | 07.09.2019 |            | OPPFYLT  | Nei                  |
      | 3456    | BOR_MED_SØKER                                  |                  | 07.09.2019 | 31.12.2021 | OPPFYLT  | Nei                  |
      | 3456    | UNDER_18_ÅR                                    |                  | 07.09.2019 | 06.09.2037 | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for begrunnelse for behandling 2
      | AktørId | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET,LOVLIG_OPPHOLD                  |                  | 16.09.1984 |            | OPPFYLT  | Nei                  |

      | 3456    | BOSATT_I_RIKET,LOVLIG_OPPHOLD,GIFT_PARTNERSKAP |                  | 07.09.2019 |            | OPPFYLT  | Nei                  |
      | 3456    | UNDER_18_ÅR                                    |                  | 07.09.2019 | 06.09.2037 | OPPFYLT  | Nei                  |
      | 3456    | BOR_MED_SØKER                                  |                  | 07.09.2020 | 31.12.2021 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 3456    | 1            | 01.10.2019 | 31.08.2020 | 1054  | ORDINÆR_BARNETRYGD | 100     |
      | 3456    | 1            | 01.09.2020 | 31.08.2021 | 1354  | ORDINÆR_BARNETRYGD | 100     |
      | 3456    | 1            | 01.09.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     |
      | 3456    | 2            | 01.10.2020 | 31.08.2021 | 1354  | ORDINÆR_BARNETRYGD | 100     |
      | 3456    | 2            | 01.09.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     |

    Når begrunnelsetekster genereres for behandling 2

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser          | Ekskluderte Begrunnelser              |
      | 01.10.2019 | 30.09.2020 | OPPHØR             |           | OPPHØR_BARN_BODDE_IKKE_MED_SØKER | OPPHØR_AVTALE_DELT_BOSTED_IKKE_GYLDIG |
      | 01.10.2020 | 31.08.2021 | UTBETALING         |           |                                  |                                       |
      | 01.09.2021 | 31.12.2021 | UTBETALING         |           |                                  |                                       |
      | 01.01.2022 |            | OPPHØR             |           |                                  |                                       |

  Scenario: Skal gi opphør fra forrige behandling-begrunnelser knyttet til bor med søker, med delt bosted
    Og lag personresultater for begrunnelse for behandling 1
    Og lag personresultater for begrunnelse for behandling 2

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | LOVLIG_OPPHOLD,BOSATT_I_RIKET                  |                  | 16.09.1984 |            | OPPFYLT  | Nei                  |

      | 3456    | LOVLIG_OPPHOLD,BOSATT_I_RIKET,GIFT_PARTNERSKAP |                  | 07.09.2019 |            | OPPFYLT  | Nei                  |
      | 3456    | UNDER_18_ÅR                                    |                  | 07.09.2019 | 06.09.2037 | OPPFYLT  | Nei                  |
      | 3456    | BOR_MED_SØKER                                  | DELT_BOSTED      | 07.09.2019 | 06.09.2020 | OPPFYLT  | Nei                  |
      | 3456    | BOR_MED_SØKER                                  |                  | 07.09.2020 | 31.12.2021 | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for begrunnelse for behandling 2
      | AktørId | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET,LOVLIG_OPPHOLD                  |                  | 16.09.1984 |            | OPPFYLT  | Nei                  |

      | 3456    | BOSATT_I_RIKET,LOVLIG_OPPHOLD,GIFT_PARTNERSKAP |                  | 07.09.2019 |            | OPPFYLT  | Nei                  |
      | 3456    | UNDER_18_ÅR                                    |                  | 07.09.2019 | 06.09.2037 | OPPFYLT  | Nei                  |
      | 3456    | BOR_MED_SØKER                                  |                  | 07.09.2020 | 31.12.2021 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 3456    | 1            | 01.10.2019 | 31.08.2020 | 1054  | ORDINÆR_BARNETRYGD | 100     |
      | 3456    | 1            | 01.09.2020 | 31.08.2021 | 1354  | ORDINÆR_BARNETRYGD | 100     |
      | 3456    | 1            | 01.09.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     |
      | 3456    | 2            | 01.10.2020 | 31.08.2021 | 1354  | ORDINÆR_BARNETRYGD | 100     |
      | 3456    | 2            | 01.09.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     |

    Når begrunnelsetekster genereres for behandling 2

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser               | Ekskluderte Begrunnelser         |
      | 01.10.2019 | 30.09.2020 | OPPHØR             |           | OPPHØR_AVTALE_DELT_BOSTED_IKKE_GYLDIG | OPPHØR_BARN_BODDE_IKKE_MED_SØKER |
      | 01.10.2020 | 31.08.2021 | UTBETALING         |           |                                       |                                  |
      | 01.09.2021 | 31.12.2021 | UTBETALING         |           |                                       |                                  |
      | 01.01.2022 |            | OPPHØR             |           |                                       |                                  |

  Scenario: Skal gi opphør fra forrige behandling-begrunnelser knyttet til bosatt i riket
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | NYE_OPPLYSNINGER |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 29.05.1988  |
      | 1            | 3456    | BARN       | 28.04.2006  |
      | 1            | 5678    | BARN       | 01.05.2010  |
      | 2            | 1234    | SØKER      | 29.05.1988  |
      | 2            | 3456    | BARN       | 28.04.2006  |
      | 2            | 5678    | BARN       | 01.05.2010  |

    Og følgende dagens dato 15.09.2023
    Og lag personresultater for begrunnelse for behandling 1
    Og lag personresultater for begrunnelse for behandling 2

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | LOVLIG_OPPHOLD                                               |                  | 29.05.1988 |            | OPPFYLT  | Nei                  |
      | 1234    | BOSATT_I_RIKET                                               |                  | 11.11.2021 |            | OPPFYLT  | Nei                  |

      | 3456    | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 28.04.2006 |            | OPPFYLT  | Nei                  |
      | 3456    | UNDER_18_ÅR                                                  |                  | 28.04.2006 | 27.04.2024 | OPPFYLT  | Nei                  |

      | 5678    | GIFT_PARTNERSKAP,BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.05.2010 |            | OPPFYLT  | Nei                  |
      | 5678    | UNDER_18_ÅR                                                  |                  | 01.05.2010 | 30.04.2028 | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for begrunnelse for behandling 2
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | LOVLIG_OPPHOLD                                               |                  | 29.05.1988 |            | OPPFYLT  | Nei                  |
      | 1234    | BOSATT_I_RIKET                                               |                  | 19.01.2022 |            | OPPFYLT  | Nei                  |

      | 3456    | BOSATT_I_RIKET,GIFT_PARTNERSKAP,BOR_MED_SØKER,LOVLIG_OPPHOLD |                  | 28.04.2006 |            | OPPFYLT  | Nei                  |
      | 3456    | UNDER_18_ÅR                                                  |                  | 28.04.2006 | 27.04.2024 | OPPFYLT  | Nei                  |

      | 5678    | BOSATT_I_RIKET,LOVLIG_OPPHOLD,GIFT_PARTNERSKAP,BOR_MED_SØKER |                  | 01.05.2010 |            | OPPFYLT  | Nei                  |
      | 5678    | UNDER_18_ÅR                                                  |                  | 01.05.2010 | 30.04.2028 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3456    | 1            | 01.12.2021 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3456    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3456    | 1            | 01.07.2023 | 31.03.2024 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 5678    | 1            | 01.12.2021 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5678    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 5678    | 1            | 01.07.2023 | 30.04.2028 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

      | 3456    | 2            | 01.02.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3456    | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3456    | 2            | 01.07.2023 | 31.03.2024 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 5678    | 2            | 01.02.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5678    | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 5678    | 2            | 01.07.2023 | 30.04.2028 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Når begrunnelsetekster genereres for behandling 2

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser    | Ekskluderte Begrunnelser |
      | 01.12.2021 | 31.01.2022 | OPPHØR             |           | OPPHØR_IKKE_BOSATT_I_NORGE |                          |
      | 01.02.2022 | 28.02.2023 | UTBETALING         |           |                            |                          |
      | 01.03.2023 | 30.06.2023 | UTBETALING         |           |                            |                          |
      | 01.07.2023 | 31.03.2024 | UTBETALING         |           |                            |                          |
      | 01.04.2024 | 30.04.2028 | UTBETALING         |           |                            |                          |
      | 01.05.2028 |            | OPPHØR             |           |                            |                          |

  Scenario: Skal ikke gi opphør fra forrige behandling, men normalt avslag
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat  | Behandlingsårsak |
      | 1            | 1        |                     | INNVILGET_OG_OPPHØRT | SØKNAD           |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 23.04.1985  |
      | 1            | 3456    | BARN       | 20.03.2015  |
    Og følgende dagens dato 28.09.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår         | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | LOVLIG_OPPHOLD               |                          | 23.04.1985 |            | OPPFYLT  | Nei                  |
      | 1234    | BOSATT_I_RIKET               | VURDERING_ANNET_GRUNNLAG | 01.06.2019 | 28.02.2022 | OPPFYLT  | Nei                  |

      | 3456    | UNDER_18_ÅR                  |                          | 20.03.2015 | 19.03.2033 | OPPFYLT  | Nei                  |
      | 3456    | GIFT_PARTNERSKAP             |                          | 20.03.2015 |            | OPPFYLT  | Nei                  |
      | 3456    | BOR_MED_SØKER,LOVLIG_OPPHOLD |                          | 01.06.2019 |            | OPPFYLT  | Nei                  |
      | 3456    | BOSATT_I_RIKET               |                          | 19.11.2021 | 28.02.2022 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3456    | 1            | 01.12.2021 | 28.02.2022 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser | Ekskluderte Begrunnelser   |
      | 01.12.2021 | 28.02.2022 | UTBETALING         |           |                         |                            |
      | 01.03.2022 |            | OPPHØR             |           | AVSLAG_BOSATT_I_RIKET   | OPPHØR_IKKE_BOSATT_I_NORGE |
