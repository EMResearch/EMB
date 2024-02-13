# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for hendelser

  Bakgrunn:
    Gitt følgende behandling
      | BehandlingId |
      | 1            |

  Scenario: Skal ta med 6-års begrunnelse når barn blir 6 år
    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1234    | SØKER      | 11.01.1970  |              |
      | 1            | 3456    | BARN       | 13.04.2017  |              |

    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2017 | 12.04.2035 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2017 |            | Oppfylt  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2017 | 31.03.2023 | 1354  | 1            |
      | 3456    | 01.04.2023 | 31.03.2035 | 1054  | 1            |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Inkluderte Begrunnelser | Ekskluderte Begrunnelser |
      | 01.05.2017 | 31.03.2023 | UTBETALING         |                         |                          |
      | 01.04.2023 | 31.03.2035 | UTBETALING         | REDUKSJON_UNDER_6_ÅR    |                          |
      | 01.04.2035 |            | OPPHØR             | OPPHØR_UNDER_18_ÅR      |                          |

  Scenario: Skal ta med dødsfallbegrunnelse om barnet er dødt
    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1234    | SØKER      | 11.01.1970  |              |
      | 1            | 5678    | BARN       | 13.04.2017  | 02.03.2024   |

    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 5678    | UNDER_18_ÅR                                                     | 13.04.2017 | 12.04.2035 | Oppfylt  |
      | 5678    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2017 | 02.03.2024 | Oppfylt  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 5678    | 01.05.2017 | 31.03.2023 | 1354  | 1            |
      | 5678    | 01.04.2023 | 31.03.2024 | 1054  | 1            |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Inkluderte Begrunnelser | Ekskluderte Begrunnelser |
      | 01.05.2017 | 31.03.2023 | UTBETALING         |                         |                          |
      | 01.04.2023 | 31.03.2024 | UTBETALING         | REDUKSJON_UNDER_6_ÅR    |                          |
      | 01.04.2024 |            | OPPHØR             | OPPHØR_BARN_DØD         |                          |

  Scenario: Skal ta med satsendringbegrunnelse ved satsendring
    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1234    | SØKER      | 11.01.1970  |              |
      | 1            | 3456    | BARN       | 13.04.2017  |              |

    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2017 | 12.04.2035 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2017 |            | Oppfylt  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2017 | 28.02.2023 | 1676  | 1            |
      | 3456    | 01.03.2023 | 31.03.2035 | 1083  | 1            |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Inkluderte Begrunnelser                     | Ekskluderte Begrunnelser |
      | 01.05.2017 | 28.02.2023 | UTBETALING         |                                             |                          |
      | 01.03.2023 | 31.03.2035 | UTBETALING         | REDUKSJON_UNDER_6_ÅR, REDUKSJON_SATSENDRING |                          |
      | 01.04.2035 |            | OPPHØR             | OPPHØR_UNDER_18_ÅR                          |                          |
