# language: no
# encoding: UTF-8


Egenskap: Vedtaksperioder med mor og to barn

  Bakgrunn:
    Gitt følgende vedtak
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 24.12.1987  |
      | 1            | 3456    | BARN       | 01.02.2016  |


  Scenario: Skal kun lage én avslagsperiode når det er avslag på søker hele perioden og ingen andre avslag

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET                                                  | 24.12.1987 |            | Oppfylt      |                      |
      | 1234    | LOVLIG_OPPHOLD                                                  | 24.12.1987 |            | ikke_oppfylt | Ja                   |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 01.12.2016 |            | Oppfylt      |                      |
      | 3456    | UNDER_18_ÅR                                                     | 01.12.2016 | 30.11.2034 | Oppfylt      |                      |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar |
      | 1988-01-01 |          | AVSLAG             | Kun søker |


  Scenario: Skal kun lage én avslagsperiode når det er avslag på søker hele perioden og ingen andre avslag. Ingen startdato

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET                                                  | 24.12.1987 |            | Oppfylt      |                      |
      | 1234    | LOVLIG_OPPHOLD                                                  |            |            | ikke_oppfylt | Ja                   |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 01.12.2016 |            | Oppfylt      |                      |
      | 3456    | UNDER_18_ÅR                                                     | 01.12.2016 | 30.11.2034 | Oppfylt      |                      |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato | Til dato | Vedtaksperiodetype | Kommentar |
      |          |          | AVSLAG             | Kun søker |

  Scenario: Skal lage to avslagsperioder når søker har konstant avslag og barn har en avslagsperiode med fom og tom

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET                                   | 24.12.1987 |            | Oppfylt      |                      |
      | 1234    | LOVLIG_OPPHOLD                                   |            |            | ikke_oppfylt | Ja                   |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 01.12.2016 |            | Oppfylt      |                      |
      | 3456    | BOR_MED_SØKER                                    | 01.12.2016 | 01.12.2020 | Oppfylt      |                      |
      | 3456    | UNDER_18_ÅR                                      | 01.12.2016 | 30.11.2034 | Oppfylt      |                      |
      | 3456    | BOR_MED_SØKER                                    | 02.12.2020 | 30.09.2021 | ikke_oppfylt | Ja                   |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar            |
      |            |            | AVSLAG             | Kun søker har avslag |
      | 01.01.2021 | 30.09.2021 | AVSLAG             | Barn har avslag      |


  Scenario: Skal lage avslagsperioder når søker har konstant avslag og barn har vilkår med overlappende avslag

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 24.12.1987  |
      | 1            | 3456    | BARN       | 01.12.2016  |
      | 1            | 5678    | BARN       | 01.02.2017  |

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                           | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET                   | 24.12.1987 |            | Oppfylt      |                      |
      | 1234    | LOVLIG_OPPHOLD                   |            |            | ikke_oppfylt | Ja                   |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET | 01.12.2016 |            | Oppfylt      |                      |
      | 3456    | LOVLIG_OPPHOLD                   | 01.12.2016 | 30.05.2020 | Oppfylt      |                      |
      | 3456    | BOR_MED_SØKER                    | 01.12.2016 | 01.12.2020 | Oppfylt      |                      |
      | 3456    | UNDER_18_ÅR                      | 01.12.2016 | 30.11.2034 | Oppfylt      |                      |
      | 5678    | GIFT_PARTNERSKAP, BOSATT_I_RIKET | 01.12.2017 |            | Oppfylt      |                      |
      | 5678    | LOVLIG_OPPHOLD                   | 01.12.2017 | 30.05.2021 | Oppfylt      |                      |
      | 5678    | BOR_MED_SØKER                    | 01.12.2017 | 01.12.2021 | Oppfylt      |                      |
      | 5678    | UNDER_18_ÅR                      | 01.12.2017 | 30.11.2035 | Oppfylt      |                      |

      | 3456    | LOVLIG_OPPHOLD                   | 08.06.2020 | 30.09.2021 | ikke_oppfylt | Ja                   |
      | 3456    | BOR_MED_SØKER                    | 02.12.2020 | 31.12.2021 | ikke_oppfylt | Ja                   |

      | 5678    | LOVLIG_OPPHOLD                   | 08.06.2021 | 30.09.2022 | ikke_oppfylt | Ja                   |
      | 5678    | BOR_MED_SØKER                    | 02.12.2021 | 31.12.2022 | ikke_oppfylt | Ja                   |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar                          |
      |            |            | AVSLAG             | Søker har avslag                   |

      | 01.07.2020 | 31.12.2020 | AVSLAG             | Barn1 har avslag på LOVLIG_OPPHOLD |
      | 01.01.2021 | 30.09.2021 | AVSLAG             | Barn1 har avslag på to vilkår      |
      | 01.10.2021 | 31.12.2021 | AVSLAG             | Barn1 har avslag på BOR_MED_SØKER  |

      | 01.07.2021 | 31.12.2021 | AVSLAG             | Barn2 har avslag på LOVLIG_OPPHOLD |
      | 01.01.2022 | 30.09.2022 | AVSLAG             | Barn2 har avslag på to vilkår      |
      | 01.10.2022 | 31.12.2022 | AVSLAG             | Barn2 har avslag på BOR_MED_SØKER  |


  Scenario: Skal lage opphørsperiode når det er overlapp i opphør og avslag på tvers av tvillinger der en har eksplisitt avslag

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 24.12.1987  |
      | 1            | 3456    | BARN       | 02.12.2016  |
      | 1            | 5678    | BARN       | 02.12.2016  |

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET                                   | 24.12.1987 |            | Oppfylt      |                      |
      | 1234    | LOVLIG_OPPHOLD                                   | 24.12.1987 | 01.12.2020 | Oppfylt      |                      |

      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 02.12.2016 |            | Oppfylt      |                      |
      | 3456    | BOR_MED_SØKER                                    | 02.12.2016 | 01.12.2020 | Oppfylt      |                      |
      | 3456    | UNDER_18_ÅR                                      | 02.12.2016 | 30.11.2034 | Oppfylt      |                      |

      | 5678    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 02.12.2016 |            | Oppfylt      |                      |
      | 5678    | BOR_MED_SØKER                                    | 02.12.2016 | 01.12.2020 | Oppfylt      |                      |
      | 5678    | UNDER_18_ÅR                                      | 02.12.2016 | 30.11.2034 | Oppfylt      |                      |

      | 1234    | LOVLIG_OPPHOLD                                   | 02.12.2020 | 30.09.2021 | ikke_oppfylt | Ja                   |
      | 1234    | LOVLIG_OPPHOLD                                   | 01.10.2021 |            | Oppfylt      |                      |

      | 3456    | BOR_MED_SØKER                                    | 02.12.2020 | 30.09.2021 | ikke_oppfylt | Ja                   |
      | 3456    | BOR_MED_SØKER                                    | 01.10.2021 |            | Oppfylt      |                      |

      | 5678    | BOR_MED_SØKER                                    | 02.12.2020 | 30.09.2021 | ikke_oppfylt |                      |
      | 5678    | BOR_MED_SØKER                                    | 01.10.2021 |            | Oppfylt      |                      |


    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.12.2016 | 31.12.2020 | 1234  | 1            |
      | 3456    | 01.10.2021 | 30.11.2034 | 1234  | 1            |
      | 5678    | 01.12.2016 | 31.12.2020 | 1234  | 1            |
      | 5678    | 01.10.2021 | 30.11.2034 | 1234  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar                                           |
      | 01.01.2017 | 31.12.2020 | Utbetaling         |                                                     |
      | 01.01.2021 | 31.10.2021 | Avslag             | Søker har opphør som overlapper med avslag hos barn |
      | 01.11.2021 | 30.11.2034 | Utbetaling         |                                                     |
      | 01.12.2034 |            | Opphør             | Barn er over 18                                     |


  Scenario: Skal lage opphørsperiode når ett barn har eksplisitt avslag og det andre har ingen utbetaling i samme periode

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 24.12.1987  |
      | 1            | 3456    | BARN       | 02.12.2016  |
      | 1            | 5678    | BARN       | 02.12.2016  |

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET                                   | 24.12.1987 |            | Oppfylt      |                      |
      | 1234    | LOVLIG_OPPHOLD                                   | 24.12.1987 |            | Oppfylt      |                      |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 02.12.2016 |            | Oppfylt      |                      |
      | 3456    | BOR_MED_SØKER                                    | 02.12.2016 | 01.12.2020 | Oppfylt      |                      |
      | 3456    | UNDER_18_ÅR                                      | 02.12.2016 | 30.11.2034 | Oppfylt      |                      |
      | 5678    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 02.12.2016 |            | Oppfylt      |                      |
      | 5678    | BOR_MED_SØKER                                    | 02.12.2016 | 01.12.2020 | Oppfylt      |                      |
      | 5678    | UNDER_18_ÅR                                      | 02.12.2016 | 30.11.2034 | Oppfylt      |                      |

      | 3456    | BOR_MED_SØKER                                    | 02.12.2020 | 30.09.2021 | ikke_oppfylt | Ja                   |
      | 3456    | BOR_MED_SØKER                                    | 01.10.2021 |            | Oppfylt      |                      |
      | 5678    | BOR_MED_SØKER                                    | 02.12.2020 | 30.09.2021 | ikke_oppfylt |                      |
      | 5678    | BOR_MED_SØKER                                    | 01.10.2021 |            | Oppfylt      |                      |


    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.12.2016 | 31.12.2020 | 1234  | 1            |
      | 3456    | 01.10.2021 | 30.11.2034 | 1234  | 1            |
      | 5678    | 01.12.2016 | 31.12.2020 | 1234  | 1            |
      | 5678    | 01.10.2021 | 30.11.2034 | 1234  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1
    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar                                             |
      | 01.01.2017 | 31.12.2020 | Utbetaling         |                                                       |
      | 01.01.2021 | 31.10.2021 | OPPHØR             | Barn 1 har avslag som overlapper med opphør hos barn2 |
      | 01.11.2021 | 30.11.2034 | Utbetaling         |                                                       |
      | 01.12.2034 |            | Opphør             | Barna er over 18                                      |