package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.HentFagsystemsbehandlingRequestSendt
import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface HentFagsystemsbehandlingRequestSendtRepository :
    RepositoryInterface<HentFagsystemsbehandlingRequestSendt, UUID>,
    InsertUpdateRepository<HentFagsystemsbehandlingRequestSendt> {

    fun findByEksternFagsakIdAndYtelsestypeAndEksternId(
        eksternFagsakId: String,
        ytelsestype: Ytelsestype,
        eksternId: String,
    ): HentFagsystemsbehandlingRequestSendt?
}
