# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for fagsaktype

  Scenario: Skal ikke gi institusjonsbegrunnelser når vi har normal fagsak
    Gitt følgende fagsaker for begrunnelse
      | FagsakId  | Fagsaktype |
      | 200057161 | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId  | ForrigeBehandlingId |
      | 100175168    | 200057161 |                     |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId       | Persontype | Fødselsdato |
      | 100175168    | 2578520707923 | SØKER      | 05.01.1983  |
      | 100175168    | 2034260303343 | BARN       | 02.09.2004  |

    Og lag personresultater for begrunnelse for behandling 100175168

    Og legg til nye vilkårresultater for begrunnelse for behandling 100175168
      | AktørId       | Vilkår                                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2578520707923 | LOVLIG_OPPHOLD,BOSATT_I_RIKET                 |                  | 05.01.1983 |            | OPPFYLT  | Nei                  |

      | 2034260303343 | UNDER_18_ÅR                                   |                  | 02.09.2004 | 01.09.2022 | OPPFYLT  | Nei                  |
      | 2034260303343 | BOR_MED_SØKER,GIFT_PARTNERSKAP,BOSATT_I_RIKET |                  | 02.09.2004 |            | OPPFYLT  | Nei                  |
      | 2034260303343 | LOVLIG_OPPHOLD                                |                  | 01.07.2022 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId       | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 2034260303343 | 100175168    | 01.08.2022 | 31.08.2022 | 1054  | ORDINÆR_BARNETRYGD | 100     |

    Når begrunnelsetekster genereres for behandling 100175168

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser | Ekskluderte Begrunnelser           |
      | 01.08.2022 | 31.08.2022 | UTBETALING         |           |                         |                                    |
      | 01.09.2022 |            | OPPHØR             |           | OPPHØR_UNDER_18_ÅR      | OPPHØR_BARNET_ER_18_ÅR_INSTITUSJON |

  Scenario: Skal gi institusjonsbegrunnelser for institusjonssak
    Gitt følgende fagsaker for begrunnelse
      | FagsakId  | Fagsaktype  |
      | 200057108 | INSTITUSJON |

    Gitt følgende behandling
      | BehandlingId | FagsakId  | ForrigeBehandlingId |
      | 100175169    | 200057108 |                     |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId       | Persontype | Fødselsdato |
      | 100175169    | 2034260303343 | BARN       | 02.09.2004  |

    Og lag personresultater for begrunnelse for behandling 100175169

    Og legg til nye vilkårresultater for begrunnelse for behandling 100175169
      | AktørId       | Vilkår                                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2034260303343 | BOSATT_I_RIKET,GIFT_PARTNERSKAP,BOR_MED_SØKER |                  | 02.09.2004 |            | OPPFYLT  | Nei                  |
      | 2034260303343 | UNDER_18_ÅR                                   |                  | 02.09.2004 | 01.09.2022 | OPPFYLT  | Nei                  |
      | 2034260303343 | LOVLIG_OPPHOLD                                |                  | 01.07.2022 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId       | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 2034260303343 | 100175169    | 01.08.2022 | 31.08.2022 | 1054  | ORDINÆR_BARNETRYGD | 100     |

    Når begrunnelsetekster genereres for behandling 100175169

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser            | Ekskluderte Begrunnelser |
      | 01.08.2022 | 31.08.2022 | UTBETALING         |           |                                    |                          |
      | 01.09.2022 |            | OPPHØR             |           | OPPHØR_BARNET_ER_18_ÅR_INSTITUSJON |                          |
