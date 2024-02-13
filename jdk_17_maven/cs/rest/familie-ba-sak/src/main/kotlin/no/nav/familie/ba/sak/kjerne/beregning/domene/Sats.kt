package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.common.Feil
import java.time.LocalDate

data class Sats(
    val type: SatsType,
    val beløp: Int,
    val gyldigFom: LocalDate = LocalDate.MIN,
    val gyldigTom: LocalDate = LocalDate.MAX,
)

enum class SatsType(val beskrivelse: String) {
    ORBA("Ordinær barnetrygd"),
    SMA("Småbarnstillegg"),
    TILLEGG_ORBA("Tillegg til barnetrygd for barn 0-6 år"),
    FINN_SVAL("Finnmark- og Svalbardtillegg"),
    UTVIDET_BARNETRYGD("Utvidet barnetrygd"),
    ;

    fun tilYtelseType(): YtelseType = when (this) {
        ORBA -> YtelseType.ORDINÆR_BARNETRYGD
        SMA -> YtelseType.SMÅBARNSTILLEGG
        TILLEGG_ORBA -> YtelseType.ORDINÆR_BARNETRYGD
        FINN_SVAL -> throw Feil("FINN_SVAL har ikke noen tilsvarende ytelsestype")
        UTVIDET_BARNETRYGD -> YtelseType.UTVIDET_BARNETRYGD
    }
}
