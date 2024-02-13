package no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak

import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import java.math.BigDecimal
import java.time.LocalDate

class VedtakPeriode(
    var fom: LocalDate,
    var tom: LocalDate,
    var hendelsestype: String,
    var hendelsesundertype: String? = null,
    var vilkårsresultat: UtvidetVilkårsresultat,
    var feilutbetaltBeløp: BigDecimal,
    var bruttoTilbakekrevingsbeløp: BigDecimal,
    var rentebeløp: BigDecimal,
    var harBruktSjetteLedd: Boolean = false,
    var aktsomhet: Aktsomhet? = null,
    var særligeGrunner: SærligeGrunner? = null,
)
