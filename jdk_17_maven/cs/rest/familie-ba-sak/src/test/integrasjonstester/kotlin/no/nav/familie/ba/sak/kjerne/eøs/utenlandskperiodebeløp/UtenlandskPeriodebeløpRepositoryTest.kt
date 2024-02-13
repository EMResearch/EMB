package no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.YearMonth

class UtenlandskPeriodebeløpRepositoryTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository,
) : AbstractSpringIntegrationTest() {

    @Test
    fun `Skal lagre flere utenlandske periodebeløp med gjenbruk av flere aktører`() {
        val søker = aktørIdRepository.save(randomAktør())
        val barn1 = aktørIdRepository.save(randomAktør())
        val barn2 = aktørIdRepository.save(randomAktør())

        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        val behandling = behandlingRepository.save(lagBehandling(fagsak))

        val utenlandskPeriodebeløp = utenlandskPeriodebeløpRepository.save(
            lagUtenlandskPeriodebeløp(
                barnAktører = setOf(barn1, barn2),
            ).also { it.behandlingId = behandling.id },
        )

        val utenlandskPeriodebeløp2 = utenlandskPeriodebeløpRepository.save(
            lagUtenlandskPeriodebeløp(
                barnAktører = setOf(barn1, barn2),
            ).also { it.behandlingId = behandling.id },
        )

        assertEquals(utenlandskPeriodebeløp.barnAktører, utenlandskPeriodebeløp2.barnAktører)
    }

    @Test
    fun `Skal lagre skjema-feltene`() {
        val søker = aktørIdRepository.save(randomAktør())
        val barn1 = aktørIdRepository.save(randomAktør())

        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        val behandling = behandlingRepository.save(lagBehandling(fagsak))

        val utenlandskPeriodebeløp = utenlandskPeriodebeløpRepository.save(
            lagUtenlandskPeriodebeløp(
                behandlingId = behandling.id,
                barnAktører = setOf(barn1),
                fom = YearMonth.of(2020, 1),
                tom = YearMonth.of(2021, 12),
                beløp = BigDecimal.valueOf(1_234),
                valutakode = "EUR",
                intervall = Intervall.UKENTLIG,
            ),
        )

        val hentedeUtenlandskePeriodebeløp =
            utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId = behandling.id)

        assertEquals(1, hentedeUtenlandskePeriodebeløp.size)
        assertEquals(utenlandskPeriodebeløp, hentedeUtenlandskePeriodebeløp.first())
    }
}
