package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype

data class HendelseMedUndertype(
    val hendelsestype: Hendelsestype,
    val hendelsesundertype: Hendelsesundertype,
)
