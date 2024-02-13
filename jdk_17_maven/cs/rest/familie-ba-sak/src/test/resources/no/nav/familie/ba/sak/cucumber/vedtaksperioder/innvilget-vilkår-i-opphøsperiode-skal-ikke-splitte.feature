# language: no
# encoding: UTF-8


Egenskap: Vedtaksperioder for opphørsperioder skal håndtere vilkårsendringer

  Bakgrunn:
    Gitt følgende vedtak
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 24.12.1987  |
      | 1            | 3456    | BARN       | 01.02.2016  |

  Scenario: Skal kun lage én opphørsperiode selv om et vilkår blir oppfylt og det fortsatt er opphør

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET                                   | 24.12.1987 |            | Oppfylt  |
      | 1234    | LOVLIG_OPPHOLD                                   | 24.12.1987 | 04.04.2019 | Oppfylt  |
      | 1234    | LOVLIG_OPPHOLD                                   | 07.07.2021 |            | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 01.12.2016 |            | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 01.12.2016 | 04.04.2019 | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 08.08.2022 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                      | 01.12.2016 | 30.11.2034 | Oppfylt  |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.12.2016 | 31.04.2019 | 1054  | 1            |
      | 3456    | 01.05.2019 | 31.08.2022 | 1354  | 1            |
      | 3456    | 01.09.2022 | 30.11.2034 | 1354  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype |
      | 01.01.2017 | 30.04.2019 | Utbetaling         |
      | 01.05.2019 | 31.08.2022 | Opphør             |
      | 01.09.2022 | 31.01.2034 | Utbetaling         |
      | 01.02.2034 |            | Opphør             |
