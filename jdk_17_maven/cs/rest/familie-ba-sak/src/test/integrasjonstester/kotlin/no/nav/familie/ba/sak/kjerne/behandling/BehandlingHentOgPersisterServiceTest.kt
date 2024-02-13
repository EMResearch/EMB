package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BehandlingHentOgPersisterServiceTest(
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val fagsakService: FagsakService,
) : AbstractSpringIntegrationTest() {

    @Test
    fun `skal hente aktiv fødselsnummere`() {
        val fødselsnummere = listOf(randomFnr(), randomFnr())
        val fagsak1 = fagsakService.hentEllerOpprettFagsakForPersonIdent(fødselsnummere[0])
        fagsakService.oppdaterStatus(fagsak1, FagsakStatus.LØPENDE)
        val behandling1 = behandlingHentOgPersisterService.lagreEllerOppdater(lagBehandling(fagsak1), false)

        val fagsak2 = fagsakService.hentEllerOpprettFagsakForPersonIdent(fødselsnummere[1])
        fagsakService.oppdaterStatus(fagsak2, FagsakStatus.LØPENDE)
        val behandling2 = behandlingHentOgPersisterService.lagreEllerOppdater(lagBehandling(fagsak2), false)

        val aktivFødselsnummere = behandlingHentOgPersisterService.hentAktivtFødselsnummerForBehandlinger(
            listOf(
                behandling1.id,
                behandling2.id,
            ),
        )
        assertEquals(fødselsnummere[0], aktivFødselsnummere[behandling1.id])
        assertEquals(fødselsnummere[1], aktivFødselsnummere[behandling2.id])
    }

    @Test
    fun `skal hente status på behandling`() {
        val fnr = randomFnr()
        val fagsak1 = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling1 = behandlingHentOgPersisterService.lagreEllerOppdater(lagBehandling(fagsak1), false)
        assertThat(behandlingHentOgPersisterService.hentStatus(behandling1.id)).isEqualTo(behandling1.status)
    }
}
