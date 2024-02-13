# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for fortsatt innvilget

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | ENDRET_UTBETALING   | NYE_OPPLYSNINGER |
      | 2            | 1        | 1                   | FORTSATT_INNVILGET  | NYE_OPPLYSNINGER |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 31.10.1987  |
      | 1            | 2       | BARN       | 19.02.2011  |
      | 2            | 1       | SØKER      | 31.10.1987  |
      | 2            | 2       | BARN       | 19.02.2011  |

  Scenario: Skal gi begrunnelser som passer med
    Og følgende dagens dato 20.09.2023
    Og lag personresultater for begrunnelse for behandling 1
    Og lag personresultater for begrunnelse for behandling 2

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                        | Utdypende vilkår   | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD                                |                    | 31.10.1987 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                                |                    | 31.10.1987 | 14.06.2023 | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                                | VURDERT_MEDLEMSKAP | 15.06.2023 |            | OPPFYLT  | Nei                  |

      | 2       | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOR_MED_SØKER |                    | 19.02.2011 |            | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET                                |                    | 19.02.2011 | 14.06.2023 | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                   |                    | 19.02.2011 | 18.02.2029 | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET                                | VURDERT_MEDLEMSKAP | 15.06.2023 |            | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for begrunnelse for behandling 2
      | AktørId | Vilkår                                        | Utdypende vilkår   | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD                                |                    | 31.10.1987 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                                |                    | 31.10.1987 | 14.06.2023 | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                                | VURDERT_MEDLEMSKAP | 15.06.2023 |            | OPPFYLT  | Nei                  |

      | 2       | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOR_MED_SØKER |                    | 19.02.2011 |            | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET                                |                    | 19.02.2011 | 14.06.2023 | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                   |                    | 19.02.2011 | 18.02.2029 | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET                                | VURDERT_MEDLEMSKAP | 15.06.2023 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.03.2011 | 28.02.2019 | 970   | ORDINÆR_BARNETRYGD | 100     | 970  |
      | 2       | 1            | 01.03.2019 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 31.01.2029 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 2            | 01.03.2011 | 28.02.2019 | 970   | ORDINÆR_BARNETRYGD | 100     | 970  |
      | 2       | 2            | 01.03.2019 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 2            | 01.07.2023 | 31.01.2029 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Når begrunnelsetekster genereres for behandling 2

    Så forvent følgende standardBegrunnelser
      | Fra dato | Til dato | VedtaksperiodeType | Regelverk Inkluderte Begrunnelser | Inkluderte Begrunnelser                  | Ekskluderte Begrunnelser                                                                                                      |
      |          |          | FORTSATT_INNVILGET |                                   | FORTSATT_INNVILGET_MEDLEM_I_FOLKETRYGDEN | FORTSATT_INNVILGET_SØKER_BOSATT_I_RIKET, FORTSATT_INNVILGET_FORVARING_GIFT, FORTSATT_INNVILGET_FORTSATT_AVTALE_OM_DELT_BOSTED |
      |          |          | FORTSATT_INNVILGET | EØS_FORORDNINGEN                  |                                          | FORTSATT_INNVILGET_PRIMÆRLAND_STANDARD                                                                                        |