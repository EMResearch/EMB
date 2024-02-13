# language: no
# encoding: UTF-8

Egenskap: Utbetalingsoppdrag: Opphør


  Scenario: Opphør en periode

    Gitt følgende tilkjente ytelser
      | BehandlingId | Uten andeler | Fra dato | Til dato | Beløp |
      | 1            |              | 03.2021  | 03.2021  | 700   |
      | 2            | Ja           |          |          |       |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 2            | 03.2021  | 03.2021  | 03.2021     | 700   | ENDR         | Ja         | 0          |                    |

    Så forvent følgende utbetalingsoppdrag med ny utbetalingsgenerator
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 2            | 03.2021  | 03.2021  | 03.2021     | 700   | ENDR         | Ja         | 0          |                    |

  Scenario: Iverksetter på nytt etter opphør

    Gitt følgende tilkjente ytelser
      | BehandlingId | Uten andeler | Fra dato | Til dato | Beløp |
      | 1            |              | 03.2021  | 03.2021  | 700   |
      | 2            | Ja           |          |          |       |
      | 3            |              | 03.2021  | 03.2021  | 700   |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 2            | 03.2021  | 03.2021  | 03.2021     | 700   | ENDR         | Ja         | 0          |                    |
      | 3            | 03.2021  | 03.2021  |             | 700   | ENDR         | Nei        | 1          | 0                  |

    Så forvent følgende utbetalingsoppdrag med ny utbetalingsgenerator
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 2            | 03.2021  | 03.2021  | 03.2021     | 700   | ENDR         | Ja         | 0          |                    |
      | 3            | 03.2021  | 03.2021  |             | 700   | ENDR         | Nei        | 1          | 0                  |

  Scenario: Opphør en av 2 perioder

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp |
      | 1            | 03.2021  | 03.2021  | 700   |
      | 1            | 04.2021  | 04.2021  | 800   |
      | 2            | 03.2021  | 03.2021  | 700   |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 1            | 04.2021  | 04.2021  |             | 800   | NY           | Nei        | 1          | 0                  |
      | 2            | 04.2021  | 04.2021  | 04.2021     | 800   | ENDR         | Ja         | 1          | 0                  |

    Så forvent følgende utbetalingsoppdrag med ny utbetalingsgenerator
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 1            | 04.2021  | 04.2021  |             | 800   | NY           | Nei        | 1          | 0                  |
      | 2            | 04.2021  | 04.2021  | 04.2021     | 800   | ENDR         | Ja         | 1          | 0                  |

  Scenario: Opphører en lang periode

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp |
      | 1            | 03.2021  | 06.2021  | 700   |
      | 2            | 03.2021  | 04.2021  | 700   |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 06.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 2            | 03.2021  | 06.2021  | 03.2021     | 700   | ENDR         | Ja         | 0          |                    |
      | 2            | 03.2021  | 04.2021  |             | 700   | ENDR         | Nei        | 1          | 0                  |

    Så forvent følgende utbetalingsoppdrag med ny utbetalingsgenerator
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 06.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 2            | 03.2021  | 06.2021  | 05.2021     | 700   | ENDR         | Ja         | 0          |                    |

  Scenario: Opphør en tidligere periode da vi kun har med den andre av 2 perioder

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp |
      | 1            | 03.2021  | 03.2021  | 700   |
      | 1            | 04.2021  | 04.2021  | 700   |
      | 2            | 04.2021  | 04.2021  | 700   |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    | 1               |
      | 1            | 04.2021  | 04.2021  |             | 700   | NY           | Nei        | 1          | 0                  | 1               |

      # kildebehandling på den første raden burde peke til den første behandlingen. EF gjør det samme her
      | 2            | 04.2021  | 04.2021  | 03.2021     | 700   | ENDR         | Ja         | 1          | 0                  | 2               |
      | 2            | 04.2021  | 04.2021  |             | 700   | ENDR         | Nei        | 2          | 1                  | 2               |

    Så forvent følgende utbetalingsoppdrag med ny utbetalingsgenerator
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |                 |
      | 1            | 04.2021  | 04.2021  |             | 700   | NY           | Nei        | 1          | 0                  |                 |

      | 2            | 04.2021  | 04.2021  | 03.2021     | 700   | ENDR         | Ja         | 1          | 0                  |                 |
      | 2            | 04.2021  | 04.2021  |             | 700   | ENDR         | Nei        | 2          | 1                  |                 |

  Scenario: Endrer en tidligere periode til 0-utbetaling

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp |
      | 1            | 03.2021  | 03.2021  | 700   |
      | 1            | 04.2021  | 04.2021  | 700   |
      | 2            | 03.2021  | 03.2021  | 0     |
      | 2            | 04.2021  | 04.2021  | 700   |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 1            | 04.2021  | 04.2021  |             | 700   | NY           | Nei        | 1          | 0                  |

      | 2            | 04.2021  | 04.2021  | 03.2021     | 700   | ENDR         | Ja         | 1          | 0                  |
      | 2            | 04.2021  | 04.2021  |             | 700   | ENDR         | Nei        | 2          | 1                  |

    Så forvent følgende utbetalingsoppdrag med ny utbetalingsgenerator
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 1            | 04.2021  | 04.2021  |             | 700   | NY           | Nei        | 1          | 0                  |

      | 2            | 04.2021  | 04.2021  | 03.2021     | 700   | ENDR         | Ja         | 1          | 0                  |
      | 2            | 04.2021  | 04.2021  |             | 700   | ENDR         | Nei        | 2          | 1                  |


  Scenario: 2 opphør etter hverendre på ulike perioder

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp |
      | 1            | 03.2021  | 03.2021  | 700   |
      | 1            | 04.2021  | 04.2021  | 800   |
      | 1            | 05.2021  | 05.2021  | 900   |
      | 2            | 03.2021  | 03.2021  | 700   |
      | 2            | 04.2021  | 04.2021  | 800   |
      | 3            | 03.2021  | 03.2021  | 700   |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 1            | 04.2021  | 04.2021  |             | 800   | NY           | Nei        | 1          | 0                  |
      | 1            | 05.2021  | 05.2021  |             | 900   | NY           | Nei        | 2          | 1                  |

      | 2            | 05.2021  | 05.2021  | 05.2021     | 900   | ENDR         | Ja         | 2          | 1                  |

      | 3            | 05.2021  | 05.2021  | 04.2021     | 900   | ENDR         | Ja         | 2          | 1                  |

    Så forvent følgende utbetalingsoppdrag med ny utbetalingsgenerator
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 1            | 04.2021  | 04.2021  |             | 800   | NY           | Nei        | 1          | 0                  |
      | 1            | 05.2021  | 05.2021  |             | 900   | NY           | Nei        | 2          | 1                  |

      | 2            | 05.2021  | 05.2021  | 05.2021     | 900   | ENDR         | Ja         | 2          | 1                  |

      | 3            | 05.2021  | 05.2021  | 04.2021     | 900   | ENDR         | Ja         | 2          | 1                  |


  Scenario: Opphør mellom 2 andeler

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp |
      | 1            | 03.2021  | 08.2021  | 700   |
      | 2            | 03.2021  | 04.2021  | 700   |
      | 2            | 07.2021  | 08.2021  | 700   |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 08.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 2            | 03.2021  | 08.2021  | 03.2021     | 700   | ENDR         | Ja         | 0          |                    |
      | 2            | 03.2021  | 04.2021  |             | 700   | ENDR         | Nei        | 1          | 0                  |
      | 2            | 07.2021  | 08.2021  |             | 700   | ENDR         | Nei        | 2          | 1                  |

    Så forvent følgende utbetalingsoppdrag med ny utbetalingsgenerator
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 08.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 2            | 03.2021  | 08.2021  | 05.2021     | 700   | ENDR         | Ja         | 0          |                    |
      | 2            | 07.2021  | 08.2021  |             | 700   | ENDR         | Nei        | 1          | 0                  |

  Scenario: Avkorter en periode, som man sen opphører. Her må opphøret ha peiling på siste andelen med riktig tom

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp |
      | 1            | 03.2021  | 03.2021  | 700   |
      | 1            | 04.2021  | 08.2021  | 700   |
      | 2            | 03.2021  | 03.2021  | 700   |
      | 2            | 04.2021  | 05.2021  | 700   |
      | 3            | 03.2021  | 03.2021  | 700   |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 1            | 04.2021  | 08.2021  |             | 700   | NY           | Nei        | 1          | 0                  |
      | 2            | 04.2021  | 08.2021  | 04.2021     | 700   | ENDR         | Ja         | 1          | 0                  |
      | 2            | 04.2021  | 05.2021  |             | 700   | ENDR         | Nei        | 2          | 1                  |
      | 3            | 04.2021  | 05.2021  | 04.2021     | 700   | ENDR         | Ja         | 2          | 1                  |

    Så forvent følgende utbetalingsoppdrag med ny utbetalingsgenerator
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 1            | 04.2021  | 08.2021  |             | 700   | NY           | Nei        | 1          | 0                  |
      | 2            | 04.2021  | 08.2021  | 06.2021     | 700   | ENDR         | Ja         | 1          | 0                  |
      | 3            | 04.2021  | 08.2021  | 04.2021     | 700   | ENDR         | Ja         | 1          | 0                  |

