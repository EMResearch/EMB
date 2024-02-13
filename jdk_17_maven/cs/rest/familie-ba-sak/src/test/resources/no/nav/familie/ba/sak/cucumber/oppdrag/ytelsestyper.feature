# language: no
# encoding: UTF-8

Egenskap: Utbetalingsoppdrag: Ulike ytelsestyper på andelene


  Scenario: Søker med utvidet og småbarnstillegg

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Ident | Ytelse             |
      | 1            | 03.2021  | 03.2021  | 700   | 1     | UTVIDET_BARNETRYGD |
      | 1            | 03.2021  | 03.2021  | 800   | 1     | SMÅBARNSTILLEGG    |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Ytelse             | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | UTVIDET_BARNETRYGD | NY           | Nei        | 0          |                    |
      | 1            | 03.2021  | 03.2021  |             | 800   | SMÅBARNSTILLEGG    | NY           | Nei        | 1          |                    |

  Scenario: Revurdering endrer beløp på småbarnstillegg fra april

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Ident | Ytelse             |
      | 1            | 03.2021  | 05.2021  | 700   | 1     | UTVIDET_BARNETRYGD |
      | 1            | 03.2021  | 05.2021  | 800   | 1     | SMÅBARNSTILLEGG    |
      | 2            | 03.2021  | 05.2021  | 700   | 1     | UTVIDET_BARNETRYGD |
      | 2            | 03.2021  | 03.2021  | 800   | 1     | SMÅBARNSTILLEGG    |
      | 2            | 04.2021  | 05.2021  | 800   | 1     | SMÅBARNSTILLEGG    |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Ytelse             | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 05.2021  |             | 700   | UTVIDET_BARNETRYGD | NY           | Nei        | 0          |                    |
      | 1            | 03.2021  | 05.2021  |             | 800   | SMÅBARNSTILLEGG    | NY           | Nei        | 1          |                    |
      | 2            | 03.2021  | 05.2021  | 03.2021     | 800   | SMÅBARNSTILLEGG    | ENDR         | Ja         | 1          |                    |
      | 2            | 03.2021  | 03.2021  |             | 800   | SMÅBARNSTILLEGG    | ENDR         | Nei        | 2          | 1                  |
      | 2            | 04.2021  | 05.2021  |             | 800   | SMÅBARNSTILLEGG    | ENDR         | Nei        | 3          | 2                  |

  Scenario: Forelder og barn har flere stønadstyper som alle blir egne kjeder. Øker hvert beløp med 100kr i revurderingen for å verifisere at det fortsatt blir 4 ulike kjeder

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Ytelse             | Ident |
      | 1            | 03.2021  | 03.2021  | 100   | UTVIDET_BARNETRYGD | 1     |
      | 1            | 03.2021  | 03.2021  | 200   | SMÅBARNSTILLEGG    | 1     |
      | 1            | 03.2021  | 03.2021  | 300   | ORDINÆR_BARNETRYGD | 2     |
      | 1            | 03.2021  | 03.2021  | 400   | UTVIDET_BARNETRYGD | 2     |

      | 2            | 03.2021  | 03.2021  | 200   | UTVIDET_BARNETRYGD | 1     |
      | 2            | 03.2021  | 03.2021  | 300   | SMÅBARNSTILLEGG    | 1     |
      | 2            | 03.2021  | 03.2021  | 400   | ORDINÆR_BARNETRYGD | 2     |
      | 2            | 03.2021  | 03.2021  | 500   | UTVIDET_BARNETRYGD | 2     |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Ytelse             | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 100   | UTVIDET_BARNETRYGD | NY           | Nei        | 0          |                    |
      | 1            | 03.2021  | 03.2021  |             | 200   | SMÅBARNSTILLEGG    | NY           | Nei        | 1          |                    |
      | 1            | 03.2021  | 03.2021  |             | 300   | ORDINÆR_BARNETRYGD | NY           | Nei        | 2          |                    |
      | 1            | 03.2021  | 03.2021  |             | 400   | UTVIDET_BARNETRYGD | NY           | Nei        | 3          |                    |

      | 2            | 03.2021  | 03.2021  | 03.2021     | 100   | UTVIDET_BARNETRYGD | ENDR         | Ja         | 0          |                    |
      | 2            | 03.2021  | 03.2021  | 03.2021     | 200   | SMÅBARNSTILLEGG    | ENDR         | Ja         | 1          |                    |
      | 2            | 03.2021  | 03.2021  | 03.2021     | 300   | ORDINÆR_BARNETRYGD | ENDR         | Ja         | 2          |                    |
      | 2            | 03.2021  | 03.2021  | 03.2021     | 400   | UTVIDET_BARNETRYGD | ENDR         | Ja         | 3          |                    |

      | 2            | 03.2021  | 03.2021  |             | 200   | UTVIDET_BARNETRYGD | ENDR         | Nei        | 4          | 0                  |
      | 2            | 03.2021  | 03.2021  |             | 300   | SMÅBARNSTILLEGG    | ENDR         | Nei        | 5          | 1                  |
      | 2            | 03.2021  | 03.2021  |             | 400   | ORDINÆR_BARNETRYGD | ENDR         | Nei        | 6          | 2                  |
      | 2            | 03.2021  | 03.2021  |             | 500   | UTVIDET_BARNETRYGD | ENDR         | Nei        | 7          | 3                  |
