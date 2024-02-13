package no.nav.familie.ba.sak.kjerne.brev.mottaker

import io.mockk.mockk
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.ekstern.restDomene.RestBrevmottaker
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BrevmottakerControllerTest(
    @Autowired private val brevmottakerService: BrevmottakerService,
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
) : AbstractSpringIntegrationTest() {

    val brevmottakerController = BrevmottakerController(
        brevmottakerService = brevmottakerService,
        tilgangService = mockk(relaxed = true),
        utvidetBehandlingService = mockk(relaxed = true),
    )

    @Test
    @Tag("integration")
    fun kanLagreOgSlette() {
        val fagsak =
            defaultFagsak(aktør = randomAktør().also { aktørIdRepository.save(it) }).let { fagsakRepository.save(it) }
        val behandling = lagBehandling(fagsak = fagsak).let { behandlingRepository.save(it) }

        val brevmottaker = RestBrevmottaker(
            null,
            MottakerType.FULLMEKTIG,
            "navn",
            "adresse",
            null,
            "postnummer",
            "poststed",
            "NO",

        )
        brevmottakerController.leggTilBrevmottaker(behandling.id, brevmottaker)

        brevmottakerController.leggTilBrevmottaker(behandling.id, brevmottaker.copy(type = MottakerType.VERGE))
        brevmottakerController.hentBrevmottakere(behandling.id).body?.data!!.apply {
            Assertions.assertThat(this).hasSize(2)
            forEach { lagretBrevmottaker ->
                brevmottakerController.fjernBrevmottaker(behandling.id, lagretBrevmottaker.id!!)
            }
        }
        Assertions.assertThat(brevmottakerController.hentBrevmottakere(behandling.id).body?.data!!).hasSize(0)
    }
}
