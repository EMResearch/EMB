package no.nav.familie.tilbake.behandling.event

import org.springframework.context.ApplicationEvent
import java.util.UUID

class EndretPersonIdentEvent(source: Any, val fagsakId: UUID) : ApplicationEvent(source)
