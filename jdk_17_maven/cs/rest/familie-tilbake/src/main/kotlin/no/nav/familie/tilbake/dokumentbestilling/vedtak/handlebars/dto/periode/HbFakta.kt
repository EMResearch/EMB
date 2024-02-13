package no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode

import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.HendelsesundertypePerHendelsestype

data class HbFakta(
    val hendelsestype: Hendelsestype,
    val hendelsesundertype: Hendelsesundertype,
    val fritekstFakta: String? = null,
) {

    init {
        require(HendelsesundertypePerHendelsestype.getHendelsesundertyper(hendelsestype).contains(hendelsesundertype))
    }
}
