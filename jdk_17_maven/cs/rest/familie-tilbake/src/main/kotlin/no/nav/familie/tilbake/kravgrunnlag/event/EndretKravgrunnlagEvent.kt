package no.nav.familie.tilbake.kravgrunnlag.event

import org.springframework.context.ApplicationEvent
import java.util.UUID

class EndretKravgrunnlagEvent(source: Any, val behandlingId: UUID) : ApplicationEvent(source)
