# Behandlingsresultat
Behandlingsresultatet skal gjenspeile hva som har skjedd i en behandling, og er et resultat av vurderinger og endringer som er gjort i denne behandlingen. Behandlingsresultatet er styrende for hvilken brevmal som skal brukes.

For å utlede behandlingsresultat er det tre ting som peker seg ut som spesielt viktig:
- **Søknad**: Har vi mottatt en søknad eller er det fremstilt krav for noen personer? Isåfall, må vi gi et svar på søknaden i form av innvilgelse/avslag/delvis innvilget. 
- **Endringer**: Har noe endret seg siden sist? 
- **Opphør**: Har barnetrygden opphørt i denne behandlingen?

Den tekniske løsningen vi har gått for prøver å utlede de tre aspektene hver for seg, før man til slutt sitter igjen med ett søknadsresultat, ett endringsresultat og ett opphørsresultat som man kan kombinere til et behandlingsresultat.

## 1. Søknadsresultat
Søknadsresultat skal kun genereres for behandlinger med årsak søknad, fødselshendelse, klage eller grunnet manuell migrering. En viktig ting å legge merke til er også at søknadsresultat ikke utledes for _alle_ personer i disse behandlingene, men kun personene det er fremstilt krav for.

### Personer fremstilt krav for
Det er ulik utledning for hvilke personer det er fremstilt krav for avhengig av type sak:
- **Søknad**: barn som er krysset av på "Registrer søknad"-steget + søker hvis det er søkt om utvidet barnetrygd
- **Fødselshendelse**: barn som er nye på behandlingen siden forrige gang
- **Manuell migrering** eller **klage**: alle personer i persongrunnlaget

### Mulige søknadsresultater

| Resultat                  | Forklaring                                                                                                                                                                                                                                                                                                                                        |
|---------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Innvilget                 | Flere muligheter (gjelder kun personer fremstilt krav for):<br>1. Det er lagt til en ny andel med beløp > 0<br>2. Det er lagt til en ny andel med beløp satt til 0 kr pga. differanseberegning/delt bosted   <br>3. Andel har endret beløp siden sist, hvor det nye beløpet er større enn 0                                                       |
| Avslått                   | Flere muligheter:<br>1. Eksplisitt avslag for person fremstilt krav for<br>2. Lagt til ny andel med beløp satt til 0 kr pga. etterbetaling 3 år/allerede utbetalt/endre mottaker (for person fremstilt krav for) <br> 3. Det finnes uregistrerte barn <br> 4. Fødselshendelse hvor det finnes vilkår som enten er ikke vurdert eller ikke oppfylt |
| Delvis innvilget          | Vi har både innvilget og avslått (trenger ikke være på samme person).                                                                                                                                                                                                                                                                             |
| Ingen relevante endringer | Ingen av alternativene over. <br>F.eks. hvis en andel er fjernet, eller at andel har samme beløp nå som forrige gang.                                                                                                                                                                                                                             |
| null                      | Ikke søknad/fødselshendelse (dermed ingen personer fremstilt krav for) eller manuell migrering.                                                                                                                                                                                                                                                   |


## 2. Endringer
Skal utledes for **alle** behandlinger når det finnes en forrige behandling. Målet med endringsresultatet er å vise om det har vært en endring i behandlingen siden sist. 
Dette kan være både endringer i beløp og endringer i andre ting som ikke påvirker beløpet (som lovverk, kompetanse osv.). 


| Resultater      | Forklaring                                                                                                                                                                                                                                                                                                               |
|-----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| null            | Ikke søknad/fødselshendelse (dermed ingen personer fremstilt krav for) eller manuell migrering.                                                                                                                                                                                                                          |
| Endringer       | Flere muligheter:<br>1. Endring i beløp <br>&nbsp; a) For personer fremstilt krav for: kun hvis beløp var større enn 0, men nå er andelen fjernet eller satt til 0kr <br>&nbsp; b) Ellers: alle endringer i beløp <br>2. Endring i vilkårsvurdering<br>3. Endring i endret utbetaling andeler<br>4. Endring i kompetanse |
| Ingen endringer | Ingen endring i det som er nevnt i raden over.                                                                                                                                                                                                                                                                           |

 
 **OBS! Det er viktig å ikke ta med endringer som også fører til opphørsresultat eller søknadsresultat.** F.eks. det eneste som er gjort på vilkårsvurderingen er å sette sluttdato på et vilkår, noe som fører til opphør. Dette skal ikke utløse resultatet "endring" også.

Endringer i **vilkårsvurdering** innebærer:
- Endringer i utdypende vilkårsvurdering
- Endringer i lovverk/regelverk
- Nye splitter i vilkår

Vi ser kun på perioder som var oppfylt både i forrige behandling og i nåværende behandling. Dvs. hvis det eneste som er gjort er å sette tom-dato på et vilkår tidligere for å opphøre ytelsen, så blir ikke det regnet som en endring.

På **kompetanser** regner man endring som endring av:
- Søkers aktivitet
- Søkers aktivitetsland
- Annen forelders aktivitet
- Annen forelders aktivitetsland
- Barnets bostedsland
- Resultat (primærland/sekundærland osv.)

Hvis forrige kompetanse ikke var fylt ut ordentlig (som skjer ved migrering + evt autovedtak) så blir det returnert ingen endring.

For **endret utbetaling andeler** bryr vi oss kun om endringer av:
- Avtaletidspunkt delt bosted
- Årsak
- Søknadstidspunkt

_Eksempel: Forrige behandling og nåværende behandling ser helt like ut, med unntak av kompetansen som har endret annen forelders aktivitetsland fra Polen til Spania._

## 3. Opphør
Skal utledes for **alle** behandlinger. Opphørsresultatet reflekterer om det løper barnetrygd (finnes utbetalinger i fremtiden) eller ikke, og om opphøret skjedde i inneværende behandling. 

| Resultater       | Forklaring                                                                                                                                                             |
|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Opphørt          | To muligheter:<br>1. Ikke opphørt i forrige behandling, opphørt i denne behandlingen<br>2. Opphør i forrige behandling, men tidligere opphørsdato i denne behandlingen |
| Fortsatt opphørt | Barnetrygden var opphørt forrige behandling og har samme opphørsdato inneværende behandling                                                                            |
| Ikke opphørt     | Ikke opphør i denne behandlingen, det løper fortsatt barnetrygd                                                                                                        |


## Kombinasjon av resultater
Behandlingsresultat = søknadsresultat + endringsresultat + opphørsresultat

De fleste resultatene forklarer seg selv, som f.eks. "innvilget" + "endring" + "opphørt" = "innvilget, endret og opphørt".

Vi har noen unntak når resultatet fra søknadssteget er "ingen relevante endringer". Grunnen til dette er fordi man alltid skal gi et resultat på søknaden, men "ingen relevante endringer" gjør ikke det alene. Dermed er man helt avhengig av kombinasjonene denne verdien kommer med. "Ingen relevante endringer" er kun lovlig i noen få kombinasjoner, ellers kastes det feil. Se tabell under for forklaring:

| Søknadsresultat           | Endringsresultat | Opphørsresultat  | Behandlingsresultat                                                                   |
|---------------------------|------------------|------------------|---------------------------------------------------------------------------------------|
| Ingen relevante endringer | Endring          | Opphørt          | Ugyldig - ville ha blitt "endret og opphørt" som er ugyldig på søknad                 |
| Ingen relevante endringer | Endring          | Fortsatt opphørt | Ugyldig - ville ha blitt "endret/endret og fortsatt opphørt" som er ugyldig på søknad |
| Ingen relevante endringer | Endring          | Ikke opphørt     | **Endret og fortsatt innvilget**                                                      |
| Ingen relevante endringer | Ingen endring    | Opphørt          | Ugyldig - ville ha blitt "opphørt" som er ugyldig på søknad                           |
| Ingen relevante endringer | Ingen endring    | Fortsatt opphørt | Ugyldig - ville ha blitt "fortsatt opphørt" som er ugydlig på søknad                  |
| Ingen relevante endringer | Ingen endring    | Ikke opphørt     | **Fortsatt innvilget**                                                                |

En annen ting det er verdt å være obs på er:
- Fortsatt opphørt i kombinasjon med noe annet som er av betydning (f.eks. "Endret") tar ikke med fortsatt opphørt i resultatet. Vi ønsker kun å snakke om det som skjer i _denne_ behandlingen, og kommuniserer derfor kun ut "fortsatt opphørt" om det er det eneste som gjelder.

## Valideringer
- Ikke lov med eksplisitt avslag for personer det ikke er fremstilt krav for (som ikke er søker)
- Søknadsresultat-steget må returnere et resultat (altså ikke null) hvis det er søknad/fødselshendelse/manuell migrering