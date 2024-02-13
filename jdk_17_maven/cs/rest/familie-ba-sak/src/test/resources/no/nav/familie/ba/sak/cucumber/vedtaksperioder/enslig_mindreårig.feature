# language: no
# encoding: UTF-8


Egenskap: Vedtaksperioder for enslig mindreårig

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype             |
      | 1        | BARN_ENSLIG_MINDREÅRIG |

    Gitt følgende vedtak
      | BehandlingId | FagsakId |
      | 1            | 1        |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 3456    | BARN       | 02.12.2016  |


  Scenario: Enslig barn har utvidet oppfylt

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 02.12.2016 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 02.12.2016 | 01.12.2034 | Oppfylt  |
      | 3456    | UTVIDET_BARNETRYGD                                              | 02.12.2016 |            | Oppfylt  |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId | Ytelse type        |
      | 3456    | 01.12.2016 | 30.11.2034 | 1234  | 1            | Ordinær_barnetrygd |
      | 3456    | 01.12.2016 | 30.11.2034 | 2000  | 1            | Utvidet_barnetrygd |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar       |
      | 01.01.2017 | 30.11.2034 | Utbetaling         |                 |
      | 01.12.2034 |            | Opphør             | Barn er over 18 |


  Scenario: Enslig barn har rett til barnetrygd oppfylt, men avslag på utvidet barnetrygd

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 02.12.2016 |            | Oppfylt      |                      |
      | 3456    | UNDER_18_ÅR                                                     | 02.12.2016 | 01.12.2034 | Oppfylt      |                      |
      | 3456    | UTVIDET_BARNETRYGD                                              |            |            | ikke_oppfylt | Ja                   |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId | Ytelse type        |
      | 3456    | 01.12.2016 | 30.11.2034 | 1234  | 1            | Ordinær_barnetrygd |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar       |
      | 01.01.2017 | 30.11.2034 | Utbetaling         |                 |
      | 01.12.2034 |            | Opphør             | Barn er over 18 |
      |            |            | Avslag             |                 |
