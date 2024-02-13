package no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode

import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.vilkårsvurdering.domain.AnnenVurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vurdering
import java.math.BigDecimal
import java.time.LocalDate

data class HbVurderinger(
    val vilkårsvurderingsresultat: Vilkårsvurderingsresultat? = null,
    val fritekst: String? = null,
    val aktsomhetsresultat: Vurdering? = null,
    val unntasInnkrevingPgaLavtBeløp: Boolean = false,
    val særligeGrunner: HbSærligeGrunner? = null,
    val foreldelsevurdering: Foreldelsesvurderingstype = Foreldelsesvurderingstype.IKKE_VURDERT,
    val foreldelsesfrist: LocalDate? = null,
    val oppdagelsesdato: LocalDate? = null,
    val fritekstForeldelse: String? = null,
    val beløpIBehold: BigDecimal? = null,
) {

    val harForeldelsesavsnitt = foreldelsevurdering in setOf(
        Foreldelsesvurderingstype.FORELDET,
        Foreldelsesvurderingstype.TILLEGGSFRIST,
    )

    init {
        if (Foreldelsesvurderingstype.IKKE_VURDERT == foreldelsevurdering ||
            Foreldelsesvurderingstype.IKKE_FORELDET == foreldelsevurdering
        ) {
            requireNotNull<Any>(vilkårsvurderingsresultat) { "Vilkårsvurderingsresultat er ikke satt" }
        } else if (Foreldelsesvurderingstype.FORELDET == foreldelsevurdering) {
            requireNotNull(foreldelsesfrist) { "foreldelsesfrist er ikke satt" }
        } else if (Foreldelsesvurderingstype.TILLEGGSFRIST == foreldelsevurdering) {
            requireNotNull(foreldelsesfrist) { "foreldelsesfrist er ikke satt" }
            requireNotNull(oppdagelsesdato) { "oppdagelsesdato er ikke satt" }
        }
        if (AnnenVurdering.GOD_TRO == aktsomhetsresultat) {
            requireNotNull(beløpIBehold) { "beløp i behold er ikke satt" }
        } else {
            require(beløpIBehold == null) { "beløp i behold skal ikke være satt når aktsomhetsresultat er $aktsomhetsresultat" }
        }
    }
}
