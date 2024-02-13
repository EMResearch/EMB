# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for rolle ved endring av vilkår

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype             |
      | 1        | NORMAL                 |
      | 2        | BARN_ENSLIG_MINDREÅRIG |

    Gitt følgende behandling
      | BehandlingId | FagsakId |
      | 1            | 1        |
      | 2            | 2        |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 13.04.2020  |

      | 2            | 4567    | BARN       | 13.04.2020  |

  Scenario: Skal få med begrunnelse som kun gjelder søker når søker sine vilkår endrer seg
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat | Utdypende vilkår         |
      | 1234    | LOVLIG_OPPHOLD                                                  | 11.01.1970 |            | Oppfylt  |                          |
      | 1234    | BOSATT_I_RIKET                                                  | 11.01.1970 | 10.05.2020 | Oppfylt  | VURDERING_ANNET_GRUNNLAG |

      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |                          |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2020 |            | Oppfylt  |                          |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2038 | 1354  | 1            |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Inkluderte Begrunnelser    | Ekskluderte Begrunnelser |
      | 01.05.2020 | 31.05.2020 | UTBETALING         |                            |                          |
      | 01.06.2020 |            | OPPHØR             | OPPHØR_UGYLDIG_KONTONUMMER |                          |


  Scenario: Skal ikke få med begrunnelse som kun gjelder søker når barn sine vilkår endrer seg
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                          | Fra dato   | Til dato   | Resultat | Utdypende vilkår             |
      | 1234    | LOVLIG_OPPHOLD,   BOSATT_I_RIKET                | 11.01.1970 |            | Oppfylt  |                              |
      | 3456    | UNDER_18_ÅR                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |                              |
      | 3456    | GIFT_PARTNERSKAP, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2020 |            | Oppfylt  |                              |
      | 3456    | BOSATT_I_RIKET                                  | 13.04.2020 | 10.05.2020 | Oppfylt  | OMFATTET_AV_NORSK_LOVGIVNING |
      | 3456    | BOSATT_I_RIKET                                  | 11.05.2020 |            | Oppfylt  |                              |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2038 | 1354  | 1            |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Inkluderte Begrunnelser | Ekskluderte Begrunnelser                |
      | 01.05.2020 | 31.05.2020 | UTBETALING         |                         | FORTSATT_INNVILGET_SØKER_BOSATT_I_RIKET |
      | 01.06.2020 | 31.03.2038 | UTBETALING         |                         | FORTSATT_INNVILGET_SØKER_BOSATT_I_RIKET |
      | 01.04.2038 |            | OPPHØR             |                         |                                         |

  Scenario: Skal få med begrunnelse som kun gjelder søker når barn sine vilkår endrer seg om barn er søker
    Og lag personresultater for begrunnelse for behandling 2

    Og legg til nye vilkårresultater for begrunnelse for behandling 2
      | AktørId | Vilkår                                          | Fra dato   | Til dato   | Resultat | Utdypende vilkår         |
      | 4567    | UNDER_18_ÅR                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |                          |
      | 4567    | GIFT_PARTNERSKAP, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2020 |            | Oppfylt  |                          |
      | 4567    | BOSATT_I_RIKET                                  | 13.04.2020 | 10.05.2022 | Oppfylt  | VURDERING_ANNET_GRUNNLAG |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 4567    | 01.05.2020 | 31.05.2022 | 1354  | 2            |

    Når begrunnelsetekster genereres for behandling 2

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Inkluderte Begrunnelser    | Ekskluderte Begrunnelser |
      | 01.05.2020 | 31.05.2022 | UTBETALING         |                            |                          |
      | 01.06.2022 |            | OPPHØR             | OPPHØR_UGYLDIG_KONTONUMMER |                          |
