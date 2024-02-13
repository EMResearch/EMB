package no.nav.familie.tilbake.behandling.domain

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Fagsak(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Embedded(prefix = "bruker_", onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val bruker: Bruker,
    val eksternFagsakId: String,
    val fagsystem: Fagsystem,
    val ytelsestype: Ytelsestype,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    @Embedded(prefix = "institusjon_", onEmpty = Embedded.OnEmpty.USE_NULL)
    val institusjon: Institusjon? = null,
) {

    val ytelsesnavn
        get() = ytelsestype.navn[bruker.språkkode]
            ?: throw IllegalStateException("Programmeringsfeil: Språkkode lagt til uten støtte")
}
