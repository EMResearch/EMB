# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser ved opphør der vilkår blir innvilget og opphørt innenfor samme måned.

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           |
      | 2            | 1        | 1                   | OPPHØRT             | NYE_OPPLYSNINGER |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 18.09.1984  |
      | 1            | 3456    | BARN       | 16.06.2015  |
      | 1            | 5678    | BARN       | 30.11.2016  |

      | 2            | 1234    | SØKER      | 18.09.1984  |
      | 2            | 3456    | BARN       | 16.06.2015  |
      | 2            | 5678    | BARN       | 30.11.2016  |

  Scenario: Vilkårresultat er oppfylt for kun én måned. Forventer ikke utbetaling
    Og følgende dagens dato 28.09.2023
    Og lag personresultater for begrunnelse for behandling 1
    Og lag personresultater for begrunnelse for behandling 2

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                       | Fra dato   | Til dato   | Resultat |
      | 1234    | LOVLIG_OPPHOLD                                               | 18.09.1984 |            | OPPFYLT  |
      | 1234    | BOSATT_I_RIKET                                               | 15.08.2020 |            | OPPFYLT  |

      | 3456    | BOSATT_I_RIKET,GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOR_MED_SØKER | 16.06.2015 |            | OPPFYLT  |
      | 3456    | UNDER_18_ÅR                                                  | 16.06.2015 | 15.06.2033 | OPPFYLT  |

      | 5678    | LOVLIG_OPPHOLD,BOSATT_I_RIKET,GIFT_PARTNERSKAP,BOR_MED_SØKER | 30.11.2016 |            | OPPFYLT  |
      | 5678    | UNDER_18_ÅR                                                  | 30.11.2016 | 29.11.2034 | OPPFYLT  |

    Og legg til nye vilkårresultater for begrunnelse for behandling 2
      | AktørId | Vilkår                                                       | Fra dato   | Til dato   | Resultat |
      | 1234    | LOVLIG_OPPHOLD                                               | 18.09.1984 |            | OPPFYLT  |
      | 1234    | BOSATT_I_RIKET                                               | 15.08.2020 | 16.08.2020 | OPPFYLT  |

      | 3456    | BOSATT_I_RIKET,BOR_MED_SØKER,GIFT_PARTNERSKAP,LOVLIG_OPPHOLD | 16.06.2015 |            | OPPFYLT  |
      | 3456    | UNDER_18_ÅR                                                  | 16.06.2015 | 15.06.2033 | OPPFYLT  |

      | 5678    | LOVLIG_OPPHOLD,BOR_MED_SØKER,GIFT_PARTNERSKAP,BOSATT_I_RIKET | 30.11.2016 |            | OPPFYLT  |
      | 5678    | UNDER_18_ÅR                                                  | 30.11.2016 | 29.11.2034 | OPPFYLT  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3456    | 1            | 01.09.2020 | 31.05.2021 | 1354  | ORDINÆR_BARNETRYGD | 100     | 1354 |
      | 3456    | 1            | 01.06.2021 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3456    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3456    | 1            | 01.07.2023 | 31.05.2033 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 5678    | 1            | 01.09.2020 | 31.08.2021 | 1354  | ORDINÆR_BARNETRYGD | 100     | 1354 |
      | 5678    | 1            | 01.09.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     | 1654 |
      | 5678    | 1            | 01.01.2022 | 31.10.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 5678    | 1            | 01.11.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5678    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 5678    | 1            | 01.07.2023 | 31.10.2034 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Når begrunnelsetekster genereres for behandling 2

    Og med vedtaksperioder for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser       | Eøsbegrunnelser | Fritekster |
      | 01.09.2020 |          | OPPHØR_IKKE_BOSATT_I_NORGE |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.09.2020 til -
      | Begrunnelse                | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | OPPHØR_IKKE_BOSATT_I_NORGE | Ja            | 16.06.15 og 30.11.16 | 2           | august 2020                          | NB      | 0     |                  | SØKER_HAR_IKKE_RETT     |

