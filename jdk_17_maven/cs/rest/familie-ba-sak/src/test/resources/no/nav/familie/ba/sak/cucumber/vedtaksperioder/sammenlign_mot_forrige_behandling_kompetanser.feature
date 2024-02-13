# language: no
# encoding: UTF-8

Egenskap: Vedtaksperiode for behandling som revurderer kompetanse eller endret utbetaling

  Bakgrunn:
    Gitt følgende vedtak
      | BehandlingId | ForrigeBehandlingId |
      | 1            |                     |
      | 2            | 1                   |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1,2          | 1234    | SØKER      | 11.01.1970  |
      | 1,2          | 3456    | BARN       | 13.04.2020  |

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | BOR_MED_SØKER, GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |

    Og lag personresultater for behandling 2
    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | BOR_MED_SØKER, GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2038 | 1354  | 1            |
      | 3456    | 01.05.2020 | 31.03.2038 | 1354  | 2            |

  Scenario: Skal lage vedtaksperioder for revurdering mot forrige behandling med endring i kompetanse.

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId |
      | 3456    | 01.05.2020 | 30.04.2021 | NORGE_ER_PRIMÆRLAND   | 1            |
      | 3456    | 01.05.2021 | 31.03.2038 | NORGE_ER_SEKUNDÆRLAND | 1            |
      | 3456    | 01.05.2020 | 30.06.2021 | NORGE_ER_PRIMÆRLAND   | 2            |
      | 3456    | 01.07.2021 | 31.03.2038 | NORGE_ER_SEKUNDÆRLAND | 2            |

    Når vedtaksperioder med begrunnelser genereres for behandling 2

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar          |
      | 01.05.2020 | 30.06.2021 | Utbetaling         | Norge primærland   |
      | 01.07.2021 | 31.03.2038 | Utbetaling         | Norge sekundærland |
      | 01.04.2038 |            | Opphør             | Kun søker          |

  Scenario: Skal lage vedtaksperioder for revurdering mot forrige behandling med endring i endrede utbetalinger.

    Og med endrede utbetalinger
      | AktørId | Fra dato   | Til dato   | BehandlingId |
      | 3456    | 01.05.2021 | 31.03.2038 | 1            |
      | 3456    | 01.07.2021 | 31.03.2038 | 2            |

    Når vedtaksperioder med begrunnelser genereres for behandling 2

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar          |
      | 01.05.2020 | 30.06.2021 | Utbetaling         | Norge primærland   |
      | 01.07.2021 | 31.03.2038 | Utbetaling         | Norge sekundærland |
      | 01.04.2038 |            | Opphør             | Kun søker          |
