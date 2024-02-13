# language: no
# encoding: UTF-8


Egenskap: Vedtaksperioder med overgangsstønad

  Bakgrunn:
    Gitt følgende vedtak
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 24.12.1987  |
      | 1            | 3456    | BARN       | 01.07.2023  |


  Scenario: Skal ikke splitte på overgangsstønad dersom det ikke påvirker fagsaken

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | LOVLIG_OPPHOLD, BOSATT_I_RIKET                                  | 24.12.1987 |            | Oppfylt  |                      |
      | 1234    | UTVIDET_BARNETRYGD                                              | 01.07.2023 | 30.06.2029 | Oppfylt  |                      |

      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 01.07.2023 |            | Oppfylt  |                      |
      | 3456    | UNDER_18_ÅR                                                     | 01.07.2023 | 30.06.2041 | Oppfylt  |                      |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId | Ytelse type        |
      | 1234    | 01.08.2023 | 30.06.2029 | 2516  | 1            | UTVIDET_BARNETRYGD |
      | 1234    | 01.08.2023 | 30.06.2026 | 696   | 1            | SMÅBARNSTILLEGG    |

      | 3456    | 01.08.2023 | 30.06.2029 | 1766  | 1            | ORDINÆR_BARNETRYGD |
      | 3456    | 01.07.2029 | 30.11.2041 | 1310  | 1            | ORDINÆR_BARNETRYGD |

    Og med overgangsstønad
      | AktørId | Fra dato   | Til dato   | BehandlingId |
      | 1234    | 01.08.2023 | 30.06.2024 | 1            |
      | 1234    | 01.08.2024 | 30.06.2031 | 1            |
      | 1234    | 01.09.2034 | 30.06.2036 | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1
    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar                                     |
      | 01.08.2023 | 30.06.2024 | Utbetaling         |                                               |
      | 01.07.2024 | 31.07.2024 | Utbetaling         | På grunn av splitt i overgangsstønaden        |
      | 01.08.2024 | 30.06.2026 | Utbetaling         | På grunn av splitt i overgangsstønaden        |
      | 01.07.2026 | 30.06.2029 | Utbetaling         | Yngste barn over 3 år. Mister småbarnstillegg |
      | 01.07.2029 | 30.06.2041 | Utbetaling         | Barn over 6 år                                |
      | 01.07.2041 |            | Opphør             | Barn er over 18                               |


  Scenario: Skal ikke splitte på overgangsstønad dersom andelene på småbarnstillegg er satt til 0 prosent

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | LOVLIG_OPPHOLD, BOSATT_I_RIKET                                  | 24.12.1987 |            | Oppfylt  |                      |
      | 1234    | UTVIDET_BARNETRYGD                                              | 01.07.2023 | 30.06.2029 | Oppfylt  |                      |

      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 01.07.2023 |            | Oppfylt  |                      |
      | 3456    | UNDER_18_ÅR                                                     | 01.07.2023 | 30.06.2041 | Oppfylt  |                      |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId | Ytelse type        |
      | 1234    | 01.08.2023 | 30.06.2029 | 2516  | 1            | UTVIDET_BARNETRYGD |
      | 1234    | 01.08.2023 | 30.06.2026 | 0     | 1            | SMÅBARNSTILLEGG    |

      | 3456    | 01.08.2023 | 30.06.2029 | 0  | 1            | ORDINÆR_BARNETRYGD |
      | 3456    | 01.07.2029 | 30.11.2041 | 1310  | 1            | ORDINÆR_BARNETRYGD |

    Og med overgangsstønad
      | AktørId | Fra dato   | Til dato   | BehandlingId |
      | 1234    | 01.08.2023 | 30.06.2024 | 1            |
      | 1234    | 01.08.2024 | 30.06.2031 | 1            |
      | 1234    | 01.09.2034 | 30.06.2036 | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1
    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar                                     |
      | 01.08.2023 | 30.06.2026 | Utbetaling         |                                               |
      | 01.07.2026 | 30.06.2029 | Utbetaling         | Yngste barn over 3 år. Mister småbarnstillegg |
      | 01.07.2029 | 30.06.2041 | Utbetaling         | Barn over 6 år                                |
      | 01.07.2041 |            | Opphør             | Barn er over 18                               |

