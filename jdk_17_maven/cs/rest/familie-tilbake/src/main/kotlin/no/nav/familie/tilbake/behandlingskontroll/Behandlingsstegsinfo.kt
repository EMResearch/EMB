package no.nav.familie.tilbake.behandlingskontroll

import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import java.time.LocalDate

data class Behandlingsstegsinfo(
    val behandlingssteg: Behandlingssteg,
    val behandlingsstegstatus: Behandlingsstegstatus,
    val venteårsak: Venteårsak? = null,
    val tidsfrist: LocalDate? = null,
)
