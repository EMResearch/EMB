package no.nav.familie.tilbake.avstemming.domain

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import java.util.UUID

interface AvstemmingsfilRepository : RepositoryInterface<Avstemmingsfil, UUID>, InsertUpdateRepository<Avstemmingsfil>
