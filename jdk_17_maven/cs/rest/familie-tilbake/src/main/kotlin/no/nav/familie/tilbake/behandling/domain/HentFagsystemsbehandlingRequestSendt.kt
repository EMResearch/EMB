package no.nav.familie.tilbake.behandling.domain

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class HentFagsystemsbehandlingRequestSendt(
    @Id
    val id: UUID = UUID.randomUUID(),
    val eksternFagsakId: String,
    val ytelsestype: Ytelsestype,
    val eksternId: String,
    val respons: String? = null,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
