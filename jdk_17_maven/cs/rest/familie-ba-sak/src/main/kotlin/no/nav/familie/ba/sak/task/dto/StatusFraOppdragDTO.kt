package no.nav.familie.ba.sak.task.dto

import no.nav.familie.kontrakter.felles.oppdrag.OppdragId

data class StatusFraOppdragDTO(
    val fagsystem: String,
    // OppdragId trenger personIdent
    val personIdent: String,
    val aktørId: String,
    val behandlingsId: Long,
    val vedtaksId: Long,
) {

    val oppdragId
        get() = OppdragId(fagsystem, personIdent, behandlingsId.toString())
}
