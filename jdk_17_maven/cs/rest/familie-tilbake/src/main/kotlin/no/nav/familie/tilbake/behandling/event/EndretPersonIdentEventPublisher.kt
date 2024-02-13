package no.nav.familie.tilbake.behandling.event

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EndretPersonIdentEventPublisher(val applicationEventPublisher: ApplicationEventPublisher) {

    fun fireEvent(nyIdent: String, fagsakId: UUID) {
        val endretPersonIdentEvent = EndretPersonIdentEvent(nyIdent, fagsakId)
        applicationEventPublisher.publishEvent(endretPersonIdentEvent)
    }
}
