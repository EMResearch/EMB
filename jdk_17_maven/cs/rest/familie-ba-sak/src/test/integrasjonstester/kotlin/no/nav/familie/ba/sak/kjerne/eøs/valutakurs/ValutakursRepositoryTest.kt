package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagValutakurs
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class ValutakursRepositoryTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val valutakursRepository: ValutakursRepository,
) : AbstractSpringIntegrationTest() {

    @Test
    fun `Skal lagre flere valutakurser med gjenbruk av flere aktører`() {
        val søker = aktørIdRepository.save(randomAktør())
        val barn1 = aktørIdRepository.save(randomAktør())
        val barn2 = aktørIdRepository.save(randomAktør())

        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        val behandling = behandlingRepository.save(lagBehandling(fagsak))

        val valutakurs = valutakursRepository.save(
            lagValutakurs(
                barnAktører = setOf(barn1, barn2),
            ).also { it.behandlingId = behandling.id },
        )

        val valutakurs2 = valutakursRepository.save(
            lagValutakurs(
                barnAktører = setOf(barn1, barn2),
            ).also { it.behandlingId = behandling.id },
        )

        assertEquals(valutakurs.barnAktører, valutakurs2.barnAktører)
    }

    @Test
    fun `Skal lagre skjema-feltene`() {
        val søker = aktørIdRepository.save(randomAktør())
        val barn1 = aktørIdRepository.save(randomAktør())

        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        val behandling = behandlingRepository.save(lagBehandling(fagsak))

        val valutakurs = valutakursRepository.save(
            lagValutakurs(
                behandlingId = behandling.id,
                barnAktører = setOf(barn1),
                fom = YearMonth.of(2020, 1),
                tom = YearMonth.of(2021, 12),
                valutakode = "EUR",
                valutakursdato = LocalDate.of(2020, 2, 17),
                kurs = BigDecimal.valueOf(10.453),
            ),
        )

        val hentedeValutakurser =
            valutakursRepository.finnFraBehandlingId(behandlingId = behandling.id)

        assertEquals(1, hentedeValutakurser.size)
        assertEquals(valutakurs, hentedeValutakurser.first())
    }
}
