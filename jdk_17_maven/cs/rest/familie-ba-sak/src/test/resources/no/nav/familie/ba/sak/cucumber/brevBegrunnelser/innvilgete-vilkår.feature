# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser med riktig fletting av personer med innvilgede vilkår

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 31.01.1985  |
      | 1            | 3456    | BARN       | 02.02.2015  |
      | 1            | 5678    | BARN       | 07.09.2019  |

  Scenario: Du og barna - skal ha med begge barn og søker
    Og følgende dagens dato 26.09.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                        | Fra dato   | Til dato   | Resultat |
      | 1234    | LOVLIG_OPPHOLD                                | 31.01.1985 |            | OPPFYLT  |
      | 1234    | BOSATT_I_RIKET                                | 11.11.2022 |            | OPPFYLT  |

      | 3456    | LOVLIG_OPPHOLD,BOR_MED_SØKER,GIFT_PARTNERSKAP | 02.02.2015 |            | OPPFYLT  |
      | 3456    | UNDER_18_ÅR                                   | 02.02.2015 | 01.02.2033 | OPPFYLT  |
      | 3456    | BOSATT_I_RIKET                                | 11.11.2022 |            | OPPFYLT  |

      | 5678    | BOR_MED_SØKER,GIFT_PARTNERSKAP,LOVLIG_OPPHOLD | 07.09.2019 |            | OPPFYLT  |
      | 5678    | UNDER_18_ÅR                                   | 07.09.2019 | 06.09.2037 | OPPFYLT  |
      | 5678    | BOSATT_I_RIKET                                | 11.11.2022 |            | OPPFYLT  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3456    | 1            | 01.12.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3456    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3456    | 1            | 01.07.2023 | 31.01.2033 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 5678    | 1            | 01.12.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 5678    | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 5678    | 1            | 01.07.2023 | 31.08.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 5678    | 1            | 01.09.2025 | 31.08.2037 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser      | Eøsbegrunnelser | Fritekster |
      | 01.12.2022 | 28.02.2023 | INNVILGET_BOSATT_I_RIKTET |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.12.2022 til 28.02.2023
      | Begrunnelse               | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | INNVILGET_BOSATT_I_RIKTET | Ja            | 02.02.15 og 07.09.19 | 2           | november 2022                        | NB      | 2 730 |                  | SØKER_HAR_IKKE_RETT     |


  Scenario: barnet - skal kun ta med et barn når det bare er et barn som har endring i vilkår
    Og følgende dagens dato 27.09.2023
    Og lag personresultater for begrunnelse for behandling 1


    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                         | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET,LOVLIG_OPPHOLD                  | 31.01.1985 |            | OPPFYLT  |

      | 3456    | GIFT_PARTNERSKAP,BOSATT_I_RIKET,LOVLIG_OPPHOLD | 02.02.2015 |            | OPPFYLT  |
      | 3456    | UNDER_18_ÅR                                    | 02.02.2015 | 01.02.2033 | OPPFYLT  |
      | 3456    | BOR_MED_SØKER                                  | 15.05.2023 |            | OPPFYLT  |

      | 5678    | LOVLIG_OPPHOLD,GIFT_PARTNERSKAP,BOR_MED_SØKER  | 07.09.2019 |            | OPPFYLT  |
      | 5678    | UNDER_18_ÅR                                    | 07.09.2019 | 06.09.2037 | OPPFYLT  |
      | 5678    | BOSATT_I_RIKET                                 | 11.11.2022 |            | OPPFYLT  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3456    | 1            | 01.06.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3456    | 1            | 01.07.2023 | 31.01.2033 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 5678    | 1            | 01.12.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 5678    | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 5678    | 1            | 01.07.2023 | 31.08.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 5678    | 1            | 01.09.2025 | 31.08.2037 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser      | Eøsbegrunnelser | Fritekster |
      | 01.12.2022 | 28.02.2023 | INNVILGET_BOSATT_I_RIKTET |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.12.2022 til 28.02.2023
      | Begrunnelse               | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | INNVILGET_BOSATT_I_RIKTET | Nei           | 07.09.19             | 1           | november 2022                        | NB      | 1 676 |                  | SØKER_HAR_IKKE_RETT     |


  Scenario: Back to back perioder - ønsker kun å begrunne barnet som har flyttet til søker i INNVILGET_BOR_HOS_SØKER
    Og følgende dagens dato 27.09.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 5678    | BOSATT_I_RIKET,GIFT_PARTNERSKAP,LOVLIG_OPPHOLD |                  | 07.09.2019 |            | OPPFYLT  | Nei                  |
      | 5678    | UNDER_18_ÅR                                    |                  | 07.09.2019 | 06.09.2037 | OPPFYLT  | Nei                  |
      | 5678    | BOR_MED_SØKER                                  |                  | 11.11.2022 | 10.05.2023 | OPPFYLT  | Nei                  |
      | 5678    | BOR_MED_SØKER                                  | DELT_BOSTED      | 11.05.2023 |            | OPPFYLT  | Nei                  |

      | 3456    | GIFT_PARTNERSKAP,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 02.02.2015 |            | OPPFYLT  | Nei                  |
      | 3456    | UNDER_18_ÅR                                    |                  | 02.02.2015 | 01.02.2033 | OPPFYLT  | Nei                  |
      | 3456    | BOR_MED_SØKER                                  |                  | 11.05.2023 |            | OPPFYLT  | Nei                  |

      | 1234    | BOSATT_I_RIKET,LOVLIG_OPPHOLD                  |                  | 26.11.1984 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3456    | 1            | 01.06.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3456    | 1            | 01.07.2023 | 31.01.2033 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 5678    | 1            | 01.12.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 5678    | 1            | 01.03.2023 | 31.05.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 5678    | 1            | 01.06.2023 | 30.06.2023 | 862   | ORDINÆR_BARNETRYGD | 50      | 1723 |
      | 5678    | 1            | 01.07.2023 | 31.08.2025 | 883   | ORDINÆR_BARNETRYGD | 50      | 1766 |
      | 5678    | 1            | 01.09.2025 | 31.08.2037 | 655   | ORDINÆR_BARNETRYGD | 50      | 1310 |

    Og med vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                                  | Eøsbegrunnelser | Fritekster |
      | 01.12.2022 | 28.02.2023 | INNVILGET_BOR_HOS_SØKER                               |                 |            |
      | 01.06.2023 | 30.06.2023 | INNVILGET_BOR_HOS_SØKER, REDUKSJON_AVTALE_FAST_BOSTED |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.12.2022 til 28.02.2023
      | Begrunnelse             | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | INNVILGET_BOR_HOS_SØKER | Nei           | 07.09.19             | 1           | november 2022                        | NB      | 1 676 |                  | SØKER_HAR_IKKE_RETT     |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.06.2023 til 30.06.2023
      | Begrunnelse                  | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | INNVILGET_BOR_HOS_SØKER      | Nei           | 02.02.15             | 1           | mai 2023                             | NB      | 1 083 |                  | SØKER_HAR_IKKE_RETT     |
      | REDUKSJON_AVTALE_FAST_BOSTED | Nei           | 07.09.19             | 1           | mai 2023                             | NB      | 862   |                  | SØKER_HAR_IKKE_RETT     |


  Scenario:Endret for 1 av 2 - Skal kun flette inn barnet som det er utbetaling for når det andre barnet etterbetales
    Og følgende dagens dato 27.09.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                       | Fra dato   | Til dato   | Resultat |
      | 5678    | BOSATT_I_RIKET,LOVLIG_OPPHOLD,GIFT_PARTNERSKAP,BOR_MED_SØKER | 07.09.2019 |            | OPPFYLT  |
      | 5678    | UNDER_18_ÅR                                                  | 07.09.2019 | 06.09.2037 | OPPFYLT  |

      | 1234    | LOVLIG_OPPHOLD,BOSATT_I_RIKET                                | 24.09.1984 |            | OPPFYLT  |

      | 3456    | GIFT_PARTNERSKAP,BOSATT_I_RIKET,LOVLIG_OPPHOLD               | 02.02.2015 |            | OPPFYLT  |
      | 3456    | UNDER_18_ÅR                                                  | 02.02.2015 | 01.02.2033 | OPPFYLT  |
      | 3456    | BOR_MED_SØKER                                                | 07.09.2019 |            | OPPFYLT  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3456    | 1            | 01.10.2019 | 30.09.2020 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 3456    | 1            | 01.10.2020 | 31.01.2021 | 1354  | ORDINÆR_BARNETRYGD | 100     | 1354 |
      | 3456    | 1            | 01.02.2021 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3456    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3456    | 1            | 01.07.2023 | 31.01.2033 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 5678    | 1            | 01.10.2019 | 31.08.2020 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5678    | 1            | 01.09.2020 | 31.08.2021 | 1354  | ORDINÆR_BARNETRYGD | 100     | 1354 |
      | 5678    | 1            | 01.09.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     | 1654 |
      | 5678    | 1            | 01.01.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 5678    | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 5678    | 1            | 01.07.2023 | 31.08.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 5678    | 1            | 01.09.2025 | 31.08.2037 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent |
      | 3456    | 1            | 01.10.2019 | 30.09.2020 | ETTERBETALING_3ÅR | 0       |

    Og med vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser    | Eøsbegrunnelser | Fritekster |
      | 01.10.2019 | 31.08.2020 | INNVILGET_BOR_HOS_SØKER |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.10.2019 til 31.08.2020
      | Begrunnelse             | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | INNVILGET_BOR_HOS_SØKER | Nei           | 07.09.19             | 1           | september 2019                       | NB      | 1 054 |                  | SØKER_HAR_IKKE_RETT     |


