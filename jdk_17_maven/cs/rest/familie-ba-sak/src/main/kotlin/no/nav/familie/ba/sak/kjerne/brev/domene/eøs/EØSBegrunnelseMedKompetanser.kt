package no.nav.familie.ba.sak.kjerne.brev.domene.eøs

import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertKompetanse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse

data class EØSBegrunnelseMedKompetanser(
    val begrunnelse: EØSStandardbegrunnelse,
    val kompetanser: List<MinimertKompetanse>,
)
