package no.nav.familie.tilbake.micrometer.domain

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype

class VedtakPerUke(
    val år: Int,
    val uke: Int,
    val fagsystem: Fagsystem,
    val vedtakstype: Behandlingsresultatstype,
    val antall: Int,
)
