package no.nav.familie.ba.sak.task.dto

class IverksettingTaskDTO(
    val behandlingsId: Long,
    val vedtaksId: Long,
    val saksbehandlerId: String,
    personIdent: String,
) : DefaultTaskDTO(personIdent)

const val FAGSYSTEM = "BA"
