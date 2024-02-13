# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser med utvidet barnetrygd

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 3456    | BARN       | 10.12.2016  |
      | 1            | 1234    | SØKER      | 24.12.1987  |

  Scenario: Skal finne alle personer når vi har to etterfølgende innvilgede perioder der vi mister et utdypende vilkår
    Og følgende dagens dato 04.10.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår             | Fra dato   | Til dato   | Resultat |
      | 1234    | LOVLIG_OPPHOLD                              |                              | 24.12.1987 | 31.08.2021 | OPPFYLT  |
      | 1234    | UTVIDET_BARNETRYGD                          |                              | 24.12.1987 |            | OPPFYLT  |
      | 1234    | BOSATT_I_RIKET                              | OMFATTET_AV_NORSK_LOVGIVNING | 01.06.2019 | 31.08.2021 | OPPFYLT  |
      | 1234    | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                              | 01.09.2021 |            | OPPFYLT  |

      | 3456    | BOSATT_I_RIKET                              | BARN_BOR_I_EØS               | 10.12.2016 | 31.08.2021 | OPPFYLT  |
      | 3456    | LOVLIG_OPPHOLD                              |                              | 10.12.2016 | 31.08.2021 | OPPFYLT  |
      | 3456    | UNDER_18_ÅR                                 |                              | 10.12.2016 | 09.12.2034 | OPPFYLT  |
      | 3456    | GIFT_PARTNERSKAP                            |                              | 10.12.2016 |            | OPPFYLT  |
      | 3456    | BOR_MED_SØKER                               | BARN_BOR_I_EØS_MED_SØKER     | 10.12.2016 | 31.08.2021 | OPPFYLT  |
      | 3456    | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                              | 01.09.2021 |            | OPPFYLT  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3456    | 1            | 01.07.2019 | 31.12.2019 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3456    | 1            | 01.01.2020 | 31.08.2020 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3456    | 1            | 01.09.2020 | 31.08.2021 | 1354  | ORDINÆR_BARNETRYGD | 100     | 1354 |
      | 3456    | 1            | 01.09.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     | 1654 |
      | 3456    | 1            | 01.01.2022 | 30.11.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3456    | 1            | 01.12.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3456    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3456    | 1            | 01.07.2023 | 30.11.2034 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 1234    | 1            | 01.07.2019 | 31.12.2019 | 928   | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1234    | 1            | 01.01.2020 | 31.08.2020 | 838   | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1234    | 1            | 01.09.2020 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1234    | 1            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 1234    | 1            | 01.07.2023 | 30.11.2034 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |

    Og med kompetanser for begrunnelse
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet            | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 3456    | 01.07.2019 | 31.08.2020 | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN | NO                    | SE                             | SE                  |
      | 3456    | 01.09.2020 | 31.08.2021 | NORGE_ER_PRIMÆRLAND   | 1            | ARBEIDER         | I_ARBEID                             | NO                    | SE                             | NO                  |

    Og med vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                                      |
      | 01.09.2021 | 31.12.2021 | INNVILGET_OVERGANG_EØS_TIL_NASJONAL_NORSK_NORDISK_FAMILIE |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.09.2021 til 31.12.2021
      | Begrunnelse                                               | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | INNVILGET_OVERGANG_EØS_TIL_NASJONAL_NORSK_NORDISK_FAMILIE | Ja            | 10.12.16             | 1           | august 2021                          | NB      | 2 708 |                  | SØKER_FÅR_UTVIDET       |


