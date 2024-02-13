package no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode

import no.nav.familie.tilbake.vilkårsvurdering.domain.SærligGrunn

data class HbSærligeGrunner(
    val størrelse: Boolean = false,
    val annet: Boolean = false,
    val navfeil: Boolean = false,
    val tid: Boolean = false,
    val fritekst: String? = null,
    val fritekstAnnet: String? = null,
) {

    constructor(
        grunner: Collection<SærligGrunn>,
        fritekst: String? = null,
        fritekstAnnet: String? = null,
    ) : this(
        grunner.contains(SærligGrunn.STØRRELSE_BELØP),
        grunner.contains(SærligGrunn.ANNET),
        grunner.contains(SærligGrunn.HELT_ELLER_DELVIS_NAVS_FEIL),
        grunner.contains(SærligGrunn.TID_FRA_UTBETALING),
        fritekst,
        fritekstAnnet,
    )
}
