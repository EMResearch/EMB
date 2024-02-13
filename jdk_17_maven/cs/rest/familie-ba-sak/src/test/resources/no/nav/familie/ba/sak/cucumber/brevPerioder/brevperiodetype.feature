# language: no
# encoding: UTF-8

Egenskap: Brevperioder: Brevperiodetype

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | BARN       | 16.03.2022  |
      | 1            | 2       | SØKER      | 28.07.1985  |

  Scenario: Skal få brevperiode av typen utbetaling når vi har eøs nullutbetaling
    Og følgende dagens dato 29.09.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                          | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2       | BOSATT_I_RIKET                  | OMFATTET_AV_NORSK_LOVGIVNING | 28.07.1985 |            | OPPFYLT  | Nei                  |
      | 2       | LOVLIG_OPPHOLD                  |                              | 28.07.1985 |            | OPPFYLT  | Nei                  |

      | 1       | UNDER_18_ÅR                     |                              | 16.03.2022 | 15.03.2040 | OPPFYLT  | Nei                  |
      | 1       | BOR_MED_SØKER                   | BARN_BOR_I_EØS_MED_SØKER     | 16.03.2022 |            | OPPFYLT  | Nei                  |
      | 1       | LOVLIG_OPPHOLD,GIFT_PARTNERSKAP |                              | 16.03.2022 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                  | BARN_BOR_I_NORGE             | 16.03.2022 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.04.2022 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 1       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 1       | 1            | 01.07.2023 | 29.02.2028 | 23    | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 1       | 1            | 01.03.2028 | 29.02.2040 | 0     | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med kompetanser for begrunnelse
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 1       | 01.04.2022 |          | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |

    Og med vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser | Eøsbegrunnelser                 | Fritekster |
      | 01.04.2022 | 30.06.2023 |                      | INNVILGET_SEKUNDÆRLAND_STANDARD |            |

    Så forvent følgende brevperioder for behandling 1
      | Brevperiodetype | Fra dato   | Til dato      | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |
      | UTBETALING      | april 2022 | til juni 2023 | 0     | 0                          |                     | du                     |
