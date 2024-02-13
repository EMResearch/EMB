package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse

object AndelTilkjentYtelsePraktiskLikhet {
    internal fun Iterable<AndelTilkjentYtelse>.erIPraksisLik(oppdaterteAndeler: Iterable<AndelTilkjentYtelse>): Boolean {
        val venstre = this.andelerSomKanSammenliknes().toSet()
        val høyre = oppdaterteAndeler.andelerSomKanSammenliknes().toSet()

        return venstre == høyre
    }

    internal fun Iterable<AndelTilkjentYtelse>.inneholderIPraksis(andelTilkjentYtelse: AndelTilkjentYtelse): Boolean {
        return this.andelerSomKanSammenliknes().contains(andelTilkjentYtelse.andelSomKanSammenliknes())
    }

    private fun Iterable<AndelTilkjentYtelse>.andelerSomKanSammenliknes() = this.map { it.andelSomKanSammenliknes() }

    private fun AndelTilkjentYtelse.andelSomKanSammenliknes() =
        copy(
            id = 0, // Er med i hashCode, men ikke i equals i AndelTilkjentYtelse
            // Andre felter som er funksjonelt viktige for praktisk likhet er med i equals og hashCode
        )
}
