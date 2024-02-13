# Generering av vedtaksperioder

## Bakgrunn
Etter at vi vedtar en behandling ønsker vi å sende ut et dokument til brukeren som forklarer hva de har fått og ikke fått, 
og i hvilke perioder de får hva. [VedtaksperiodeProdusent](VedtaksperiodeProdusent.kt) sin oppgave er å finne ut hvilke datoer
som gjelder for de forskjellige periodene. 

## Hvilke data ser vi på
Den overordnede tanken er at dersom noe av følgende endrer seg, ønsker vi å formidle det til brukeren:
* Vilkår
* Utbetalingsbeløp
* Kompetanse
* Endret utbetaling
* Overgangsstønad

## Hvilke vilkår gjelder for persontypene
I utgangspunktet er det fem vilkår for barn og to vilkår for søker som må være oppfylt for at en periode blir innvilget. 
Det betyr at de to vilkårene på søker står felles for alle barn det er blitt søkt for siden andel tilkjent ytelse blir utbetalt
for barna.

For utvidet barnetrygd er det på søker andelene blir utbetalt og dermed blir det unaturlig å legge vilkåret på barna. Her 
må derimot vilkår til minst ett av barna være oppfylt, så vi baker inn oppfylt-perioder for barna sammen med forelders 
vilkår om utvidet barnetrygd.
![Vilkår relevante for personer sine andeler.png](Vilk%C3%A5r%20relevante%20for%20personer%20sine%20andeler.png)

## Regler for sammenslåing av individuelle personers perioder til perioder for flere personer
Dataene som skal ende opp i dokumentet vi sender til bruker ønsker vi å samle på en måte som gjør det enkelt å forstå 
hva som foregår når. 

* Dersom vilkårene ikke er innvilget er det ikke nødvendig å lagre resterende data som andeler tilkjent ytelse, kompetanse o.l.

* Dersom to personer har perioder med samme fom og tom skal periodene "slås sammen" slik at de begrunnes sammen i dokumentet.

* Dersom to innvilgede perioder følger etter hverandre men andeler tilkjent ytelse er lik for begge periodene skal periodene 
slås sammen, med mindre det er en endring i vilkår eller andre data. 

* Dersom den første perioden i tidslinja ikke er innvilget ønsker vi å strippe denne (med mindre det er et avslag) siden det 
ikke er interessant å snakke om at man ikke har innvilgete perioder f.eks. før barn er født eller dersom barnet ikke bodde
hos søker i den første perioden. 

* Dersom det er eksplisitte avslag skal det "skrive over" ikke-innvilgede perioder. Dersom det er innvilgede perioder i samme 
tidsspenn skal avslagene stå ved siden av. Se tegning under.

> I eksempelet under ser vi at i den første perioden for _barn1_ så slår vi sammen periodene til _barn1_ og _barn2_ til to 
perioder siden begge er innvilget.
Ettersom _barn3_ sin periode ikke har samme start- og sluttdato som innvilgetperiodene, slår vi den ikke sammen med de 
innvilgede periodene.
>
>I _barn1_ sin andre periode slår vi perioden til _barn2_ og _barn3_ sammen siden begge er innvilget.
I dette tilfellet har _barn1_ sin ikke-innvilgede periode samme start- og sluttdato som de kombinerte innvilgetperiodene, 
og vi slår de sammen.
>
> ![Eksempel sammenslåing avslagslagsperioder med andre perioder.png](Eksempel%20sammensl%C3%A5ing%20avslagslagsperioder%20med%20andre%20perioder.png)

## Sammenligning på tvers av behandlinger 
Dersom det er en reduksjon, altså at en periode var innvilget i forrige behandling, men ikke er det nå lenger, skal dette 
tydeliggjøres i dokumentet ved at perioden som nå ikke er godkjent kommer med selv hvis den kommer som første periode. 