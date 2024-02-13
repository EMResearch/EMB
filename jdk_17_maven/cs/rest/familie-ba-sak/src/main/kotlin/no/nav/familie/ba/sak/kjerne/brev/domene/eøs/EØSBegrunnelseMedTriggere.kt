package no.nav.familie.ba.sak.kjerne.brev.domene.eøs

import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse

data class EØSBegrunnelseMedTriggere(
    val eøsBegrunnelse: EØSStandardbegrunnelse,
    val sanityEØSBegrunnelse: SanityEØSBegrunnelse,
)
