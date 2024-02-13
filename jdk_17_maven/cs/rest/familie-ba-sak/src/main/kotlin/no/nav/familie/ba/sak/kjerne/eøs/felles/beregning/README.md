# Beregning på skjemaer for Kompetanse, Valutakurs og Utenlandsk periodebeløp

## Hva er de ulike skjemaene

Under vilkårsvurdering avgjør saksbehandler om et vilkår skal vurderes etter _EØS-forordningen_ eller _nasjonale regler_
. Hvis vilkårene er oppfylt og et gitt sett av vilkår er vurdert etter EØS-forordning for søker og ett eller flere barn,
oppstår det _EØS-perioder_ på barnet

**Kompetanse** er et skjema som saksbehandler fyller ut for å avgjøre om Norge er _primærland_ eller _sekundærland_ i
barnas EØS-perioder.

* Norge som primærland betyr at utbetaling av barnetrygd skal skje som vanlig etter nasjonale regler
* Norge som sekundærland betyr at utbetalingen mot differanseberegnes mot tilsvarende ytelse i EØS

Hvis Norge er sekundærland, må to andre skjeamer fylles ut:

* **Utenlandsk periodebeløp** inneholder hva brukeren har fått utbetalt i ytelse tilsvarende barnetygd fra annet
  EØS-land. Skjemaet inneholder beløpet, valutaen og intervallet beløpet utbetales med (ukentlig, månedlig, kvartalsvis,
  årlig etc)
* **Valutakurs** inneholder informasjon om hvilken valutkurs som skal benyttes for å omregne utenlandsk periodebeløp til
  norske kroner. Det inneholder også hvilken dato valuktakursen er hentet fra.

## Konsept

Et skjema består av to hoveddeler

* Barn og periode, som er likt for alle
* Skjemafelter, som varier fra skjema til skjema

_Barn_ er ett eller flere barn, representert ved aktør-id'er.

_Periode_ er gitt ved 'fom' og 'tom' og representerer tiden mellom fra-og-med-måned og til-og-med-måned.

Alle skjemaer implementerer `PeriodeOgBarnSkjema`, som ivaretar de grunnleggende konseptene.

For å vise konseptene tar vi utgangspunkt i _Kompetanse_, som blant annet har følgende felter:

* Barnets bostedsland, representert som en String med ISO-kode
* Annen forelders bostedsland (String)
* Søkers aktivitet (enum)
* Annen forelders aktivitet (enum)
* Kompetent land (enum)

_Kompetent land_ er en av:

* NORGE_ER_PRIMÆRLAND,
* NORGE_ER_SEKUNDÆRLAND
* BEGGE_ER_PRIMÆRLAND

Heretter benyttes følgende syntaks for å beskrive kompetanse-skjemaer

```
2020-03
"  PPPPPPP     SSSS->", B1, B2
```

som leses som:

* Startdato er mars 2020
* Kompetanse-skjema er for to barn (B1 og B2).
* Det er en primærland-periode ('P') fra og med mai 2020 (2 måneder etter startdato) og 7 måneder frem (til og med
  november 2020)
* Det er en sekundærland-periode ('S') fra og med mai 2021 og til og med august 2021
* Det er en periode der kompetanse-skjemaet finnes, men ikke er utfylt ('-'). Det er fra og med september 2021.
* '>' betyr "herfra og fremover", dvs at perioden er "åpen", så det ikke utfylte skjemaet gjelder fra-og-med september
  og videre fremover

## Beregningsregler

### Like, etterfølgende skjemaer for ett barn slås sammen

Altså:

```
2020-03
"P", B1
" P", B1
"  P", B1
```

blir til

```
2020-03
"PPP", B1
```

### Like skjemaer i samme periode for flere barn slås sammen

Altså:

```
2020-03
"PPP", B1
"PPP", B2
"PPP", B3
```

blir til

```
2020-03
"PPP", B1, B2, B3
```

### Slå sammen barn foretrekkes over å slå sammen perioder

Altså:

```
2020-03
"PPP",      B1
" PPP",     B2
"  PPP",    B3
```

blir til

```
2020-03
"P",        B1
" P",       B1, B2
"  P",      B1, B2, B3
"   P",     B2, B3
"    P",    B3
```

### Oppdatering vil kunne føre til flere skjemaer for å oppfylle reglene ovenfor

Eksisterende kompetanser som ser slik ut:

```
2020-03
"PPPPPPPP", B1, B2, B3
```

og oppdateres med

```
2020-06
"   SSS", B2
```

vil resultere i:

```
2020-03
"PPP", B1, B2, B3
"   PPP", B1, B3
"   SSS", B2
"      PP", B1, B2, B3

```

### Oppdatering vil respektere eksisterende skjema-grenser

Eksisterende kompetanser som ser slik ut:

```
2020-03
"----   SSSS ---", B1, B2, B3
```

som oppdateres med (primærlanf fra start-dato og uendelig fremover)

```
2020-03
"P>", B1, B2, B3
```

vil resultere i:

```
2020-03
"PPPP   PPPP PPP", B1, B2, B3
```

### Innsnevring av ett skjema fører til at opprettes tomme skjemaer "rundt"

Eksisterende skjema (som altså løper fra 2020-03, men ikke har avslutning)

```
2020-03
"S>", B1, B2, B3
```

som oppdateres med (altså at perioden blir avsluttet, og ett barn fjernes)

```
2020-03
"SSS", B1, B2
```

vil resultere i:

```
2020-03
"SSS->", B1, B2
"->", B3
```

Her blir det altså opprettet et tomt skjema for barn B1 og B2 fra og med juni, mens barn B3 får et tomt skjema for hele
perioden, altså fra og med mars.

### Spesielt for Kompetanse

#### Kompetanser matcher alltid EØS-periodene.

Hvis vilkårsvurderingen fører til at barnets EØS-perioder endrer seg, så vil kompetanse endre seg. Det er et par
tilfeller:

* EØS-perioden reduseres for et barn.
* EØS-perioden utvides for et barn

#### EØS-perioden reduseres for et barn

Barnet beholder skjema-innholdet for den "smalere" perioden. Men det kan føre til at settet av kompetanser endrer seg.

F.eks hvis regleverk-periodene ser slik ut (E = EØS. N = Nasjonalt) for tre barn

```
2020-03
"EEEEEEEEEEEEEE", B1, B2, B3
```

og har fått følgende kompetansevurdering:

```
2020-03
"PPPPPSSSSSSSSS", B1, B2, B3
```

Når EØS-periodene endrer seg for ett barn til dette:

```
2020-03
"NNNEEEEEEEEEEE", B1
```

så vil samlet kompetanse bli

```
2020-03
"PPP", B2, B3
"   PPSSSSSSSSS", B1, B2, B3
```

#### EØS-perioden utvides for et barn

Barnet beholder skjema-innholdet for den orignale perioden, men får ett eller to uutfylte kompetanse-skjemaer for de(n)
ekstra tidsperioden(e)

F.eks hvis regleverk-periodene ser slik ut

```
2020-03
"EEEEEEEEEEEEEE", B1, B2, B3
```

og har fått følgende kompetansevurdering:

```
2020-03
"PPPPPSSSSSSSSS", B1, B2, B3
```

Når EØS-periodene endrer seg for ett barn til dette (starter 2 mnd tidligere og slutter 3 mnd senere):

```
2020-01
"EEEEEEEEEEEEEEEE", B1
```

så vil samlet kompetanse bli

```
2020-01
"--", B1
"  PPPPPSSSSSSSSS", B1, B2, B3
"                ---", B1
```
