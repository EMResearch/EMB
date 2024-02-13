# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder for Satsendring

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId  | Fagsaktype |
      | 200053601 | NORMAL     |

    Gitt følgende vedtak
      | BehandlingId | FagsakId  | ForrigeBehandlingId |
      | 100175851    | 200053601 |                     |

    Og følgende persongrunnlag
      | BehandlingId | AktørId       | Persontype | Fødselsdato |
      | 100175851    | 2499861499383 | SØKER      | 24.12.1987  |
      | 100175851    | 2435441739050 | BARN       | 10.07.2012  |

  Scenario: Skal ikke lage splitt på satsendring
    Og følgende dagens dato 2023-09-13
    Og lag personresultater for behandling 100175851

    Og legg til nye vilkårresultater for behandling 100175851
      | AktørId       | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2499861499383 | LOVLIG_OPPHOLD   |                              | 01.07.2019 |            | OPPFYLT  | Nei                  |
      | 2499861499383 | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 01.07.2019 |            | OPPFYLT  | Nei                  |

      | 2435441739050 | UNDER_18_ÅR      |                              | 10.07.2012 | 09.07.2030 | OPPFYLT  | Nei                  |
      | 2435441739050 | GIFT_PARTNERSKAP |                              | 09.06.2017 |            | OPPFYLT  | Nei                  |
      | 2435441739050 | LOVLIG_OPPHOLD   |                              | 01.07.2019 |            | OPPFYLT  | Nei                  |
      | 2435441739050 | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 01.07.2019 |            | OPPFYLT  | Nei                  |
      | 2435441739050 | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 01.07.2019 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId       | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2435441739050 | 100175851    | 01.08.2019 | 31.12.2019 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2435441739050 | 100175851    | 01.01.2020 | 31.12.2020 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2435441739050 | 100175851    | 01.01.2021 | 31.12.2021 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2435441739050 | 100175851    | 01.01.2022 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2435441739050 | 100175851    | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2435441739050 | 100175851    | 01.07.2023 | 30.06.2030 | 187   | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med kompetanser
      | AktørId       | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2435441739050 | 01.08.2019 |          | NORGE_ER_SEKUNDÆRLAND | 100175851    | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |

    Når vedtaksperioder med begrunnelser genereres for behandling 100175851

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype |
      | 01.08.2019 | 30.06.2023 | UTBETALING         |
      | 01.07.2023 | 30.06.2030 | UTBETALING         |
      | 01.07.2030 |            | OPPHØR             |
