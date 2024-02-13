# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder skal filtrere vekk irrelevante perioder

  Bakgrunn:
    Gitt følgende vedtak
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 13.04.2020  |

  Scenario: Skal kun ta med første opphørsperiode etter siste utbetalingsperiode. Eksplisitte avslag skal med uansett.

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                           | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD   | 11.01.1970 |            | Oppfylt      |                      |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET | 13.04.2020 | 06.06.2021 | Oppfylt      |                      |
      | 3456    | LOVLIG_OPPHOLD                   | 13.04.2020 | 08.08.2021 | Oppfylt      |                      |
      | 3456    | BOR_MED_SØKER                    | 13.04.2020 | 08.08.2021 | Oppfylt      |                      |
      | 3456    | UNDER_18_ÅR                      | 13.04.2020 | 12.04.2038 | Oppfylt      |                      |
      | 3456    | BOR_MED_SØKER                    | 01.09.2021 | 04.10.2022 | ikke_oppfylt | Ja                   |
      | 3456    | BOR_MED_SØKER                    | 05.10.2022 |            | ikke_oppfylt |                      |


    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2038 | 1054  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar                                                               |
      | 01.05.2020 | 30.06.2021 | Utbetaling         | Barn1 og søker                                                          |
      | 01.07.2021 |            | Opphør             | Første opphør etter siste utbetalingsperiode                            |
      | 01.10.2021 | 31.10.2022 | Avslag             | Eksplisitt avslag skal med selv om de er etter siste utbetalingsperiode |


  Scenario: Skal ikke fjerne perioder når siste periode er ikke-innvilget

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |                      |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2020 |            | Oppfylt  |                      |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |                      |


    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2038 | 1054  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar      |
      | 01.05.2020 | 31.03.2038 | Utbetaling         | Barn1 og søker |
      | 01.04.2038 |            | Opphør             | Første opphør  |


  Scenario: Skal ikke fjerne perioder når det kun er innvilgete perioder

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |                      |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2020 |            | Oppfylt  |                      |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |                      |


    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2038 | 1054  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar      |
      | 01.05.2020 | 31.03.2038 | Utbetaling         | Barn1 og søker |
      | 01.04.2038 |            | Opphør             | Første opphør  |










