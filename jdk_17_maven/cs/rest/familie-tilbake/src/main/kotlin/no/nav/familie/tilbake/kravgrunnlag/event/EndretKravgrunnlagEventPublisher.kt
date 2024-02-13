package no.nav.familie.tilbake.kravgrunnlag.event

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EndretKravgrunnlagEventPublisher(val applicationEventPublisher: ApplicationEventPublisher) {

    fun fireEvent(behandlingId: UUID) {
        val endretKravgrunnlagEvent = EndretKravgrunnlagEvent(this, behandlingId)
        applicationEventPublisher.publishEvent(endretKravgrunnlagEvent)
    }
}
