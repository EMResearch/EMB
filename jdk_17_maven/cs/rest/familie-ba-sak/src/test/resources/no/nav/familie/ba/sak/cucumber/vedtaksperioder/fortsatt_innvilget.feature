# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder for fortsatt innvilget

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId  | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende vedtak
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           |
      | 2            | 1        | 1                   | FORTSATT_INNVILGET  | ÅRLIG_KONTROLL   |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 27.04.1991  |
      | 1            | 2       | BARN       | 07.10.2021  |
      | 2            | 1       | SØKER      | 27.04.1991  |
      | 2            | 2       | BARN       | 07.10.2021  |

  Scenario: Skal gi riktige perioder når behandlingsresultatet er fortsatt innvilget
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                          | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2       | LOVLIG_OPPHOLD,GIFT_PARTNERSKAP |                              | 07.10.2021 |            | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER                   | BARN_BOR_I_EØS_MED_SØKER     | 07.10.2021 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                     |                              | 07.10.2021 | 06.10.2039 | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET                  | BARN_BOR_I_EØS               | 07.10.2021 |            | OPPFYLT  | Nei                  |

      | 1       | LOVLIG_OPPHOLD                  |                              | 27.04.1991 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                  | OMFATTET_AV_NORSK_LOVGIVNING | 11.05.2021 |            | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                          | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD                  |                              | 27.04.1991 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                  | OMFATTET_AV_NORSK_LOVGIVNING | 11.05.2021 |            | OPPFYLT  | Nei                  |

      | 2       | BOR_MED_SØKER                   | BARN_BOR_I_EØS_MED_SØKER     | 07.10.2021 |            | OPPFYLT  | Nei                  |
      | 2       | LOVLIG_OPPHOLD,GIFT_PARTNERSKAP |                              | 07.10.2021 |            | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET                  | BARN_BOR_I_EØS               | 07.10.2021 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                     |                              | 07.10.2021 | 06.10.2039 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 2       | 1            | 01.11.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     |
      | 2       | 1            | 01.07.2023 | 30.09.2027 | 1766  | ORDINÆR_BARNETRYGD | 100     |
      | 2       | 1            | 01.10.2027 | 30.09.2039 | 1310  | ORDINÆR_BARNETRYGD | 100     |
      | 2       | 1            | 01.03.2022 | 30.11.2022 | 553   | ORDINÆR_BARNETRYGD | 100     |
      | 2       | 1            | 01.01.2022 | 28.02.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     |
      | 2       | 1            | 01.12.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     |
      | 2       | 2            | 01.11.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     |
      | 2       | 2            | 01.07.2023 | 30.09.2027 | 1766  | ORDINÆR_BARNETRYGD | 100     |
      | 2       | 2            | 01.10.2027 | 30.09.2039 | 1310  | ORDINÆR_BARNETRYGD | 100     |
      | 2       | 2            | 01.01.2022 | 28.02.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     |
      | 2       | 2            | 01.03.2022 | 30.11.2022 | 553   | ORDINÆR_BARNETRYGD | 100     |
      | 2       | 2            | 01.12.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.11.2021 | 28.02.2022 | NORGE_ER_PRIMÆRLAND   | 1            | ARBEIDER         | INAKTIV                   | NO                    | PL                             | PL                  |
      | 2       | 01.03.2022 | 30.11.2022 | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |
      | 2       | 01.12.2022 |            | NORGE_ER_PRIMÆRLAND   | 1            | ARBEIDER         | INAKTIV                   | NO                    | PL                             | PL                  |
      | 2       | 01.11.2021 | 28.02.2022 | NORGE_ER_PRIMÆRLAND   | 2            | ARBEIDER         | INAKTIV                   | NO                    | PL                             | PL                  |
      | 2       | 01.03.2022 | 30.11.2022 | NORGE_ER_SEKUNDÆRLAND | 2            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |
      | 2       | 01.12.2022 |            | NORGE_ER_PRIMÆRLAND   | 2            | ARBEIDER         | INAKTIV                   | NO                    | PL                             | PL                  |

    Når vedtaksperioder med begrunnelser genereres for behandling 2

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato | Til dato | Vedtaksperiodetype |
      |          |          | FORTSATT_INNVILGET |
