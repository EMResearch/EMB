package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.AndelTilkjentYtelsePraktiskLikhet.erIPraksisLik
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.AndelTilkjentYtelsePraktiskLikhet.inneholderIPraksis
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje

/**
 * En litt risikabel funksjon, som benytter "funksjonell likhet" for å sjekke etter endringer på andel tilkjent ytelse
 */
fun TilkjentYtelseRepository.oppdaterTilkjentYtelse(
    tilkjentYtelse: TilkjentYtelse,
    oppdaterteAndeler: Collection<AndelTilkjentYtelse>,
): TilkjentYtelse {
    if (tilkjentYtelse.andelerTilkjentYtelse.erIPraksisLik(oppdaterteAndeler)) {
        return tilkjentYtelse
    }

    // Her er det viktig å beholde de originale andelene, som styres av JPA og har alt av innhold
    val skalBeholdes = tilkjentYtelse.andelerTilkjentYtelse
        .filter { oppdaterteAndeler.inneholderIPraksis(it) }

    val skalLeggesTil = oppdaterteAndeler
        .filter { !tilkjentYtelse.andelerTilkjentYtelse.inneholderIPraksis(it) }

    // Forsikring: Sjekk at det ikke oppstår eller forsvinner andeler når de sjekkes for likhet
    if (oppdaterteAndeler.size != (skalBeholdes.size + skalLeggesTil.size)) {
        throw IllegalStateException("Avvik mellom antall innsendte andeler og kalkulerte endringer")
    }

    tilkjentYtelse.andelerTilkjentYtelse.clear()
    tilkjentYtelse.andelerTilkjentYtelse.addAll(skalBeholdes + skalLeggesTil)

    // Ekstra forsikring: Bygger tidslinjene på nytt for å sjekke at det ikke er introdusert duplikater
    // Krasjer med Exception hvis det forekommer perioder per aktør og ytelsetype som overlapper
    // Bør fjernes hvis det ikke forekommer feil
    tilkjentYtelse.andelerTilkjentYtelse.sjekkForDuplikater()

    return this.saveAndFlush(tilkjentYtelse)
}

@Deprecated("Brukes som sikkerhetsnett for å sjekke at det ikke oppstår duplikater. Burde være unødvendig")
private fun Iterable<AndelTilkjentYtelse>.sjekkForDuplikater() {
    try {
        // Det skal ikke være overlapp i andeler for en gitt ytelsestype og aktør
        this.groupBy { it.aktør.aktørId + it.type }
            .mapValues { (_, andeler) -> tidslinje { andeler.map { it.tilPeriode() } } }
            .values.forEach { it.perioder() }
    } catch (throwable: Throwable) {
        throw IllegalStateException(
            "Endring av andeler tilkjent ytelse i differanseberegning holder på å introdusere duplikater",
            throwable,
        )
    }
}
