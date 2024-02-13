package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import no.nav.familie.ba.sak.common.nyRevurdering
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

@TestMethodOrder(MethodOrderer.MethodName::class)
class KonsistensavstemmingUtplukkingIntegrationTest : AbstractSpringIntegrationTest() {

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var avstemmingService: AvstemmingService

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var personidentService: PersonidentService

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @Autowired
    private lateinit var databaseCleanupService: DatabaseCleanupService

    @BeforeEach
    fun cleanUp() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Skal plukke iverksatt FGB`() {
        val forelderIdent = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(forelderIdent).also {
            fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE)
        }
        val førstegangsbehandling =
            opprettOgLagreBehandlingMedAndeler(
                personIdent = forelderIdent,
                kildeOgOffsetPåAndeler = listOf(KildeOgOffsetPåAndel(null, 1L)),
                fagsakId = fagsak.id,
            )
        val iverksattOgLøpendeBehandlinger = avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker()

        val behandlingerMedRelevanteAndeler =
            andelTilkjentYtelseRepository
                .finnAndelerTilkjentYtelseForBehandlinger(iverksattOgLøpendeBehandlinger)
                .map { it.kildeBehandlingId }
                .distinct()

        Assertions.assertTrue(behandlingerMedRelevanteAndeler.any { it == førstegangsbehandling.id })
    }

    @Test
    fun `Skal plukke både iverksatt FGB og revurdering når periode legges til`() {
        val forelderIdent = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(forelderIdent).also {
            fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE)
        }

        val førstegangsbehandling =
            opprettOgLagreBehandlingMedAndeler(
                personIdent = forelderIdent,
                kildeOgOffsetPåAndeler = listOf(KildeOgOffsetPåAndel(null, 1L)),
                medStatus = BehandlingStatus.AVSLUTTET,
                fagsakId = fagsak.id,
            )
        val revurdering =
            opprettOgLagreRevurderingMedAndeler(
                personIdent = forelderIdent,
                kildeOgOffsetPåAndeler = listOf(
                    KildeOgOffsetPåAndel(førstegangsbehandling.id, 1L),
                    KildeOgOffsetPåAndel(null, 2L),
                ),
                fagsakId = fagsak.id,
            )

        val iverksattOgLøpendeBehandlinger = avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker()
        val behandlingerMedRelevanteAndeler =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(iverksattOgLøpendeBehandlinger)
                .map { it.kildeBehandlingId }
                .sortedBy { it }
                .distinct()

        Assertions.assertEquals(2, behandlingerMedRelevanteAndeler.size)
        Assertions.assertEquals(førstegangsbehandling.id, behandlingerMedRelevanteAndeler[0])
        Assertions.assertEquals(revurdering.id, behandlingerMedRelevanteAndeler[1])
    }

    @Test
    fun `Skal kun plukke revurdering når periode på førstegangsbehandling blir erstattet`() {
        val forelderIdent = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(forelderIdent).also {
            fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE)
        }
        opprettOgLagreBehandlingMedAndeler(
            personIdent = forelderIdent,
            kildeOgOffsetPåAndeler = listOf(KildeOgOffsetPåAndel(null, 1L)),
            medStatus = BehandlingStatus.AVSLUTTET,
            fagsakId = fagsak.id,
        )
        val revurdering =
            opprettOgLagreRevurderingMedAndeler(
                personIdent = forelderIdent,
                kildeOgOffsetPåAndeler = listOf(KildeOgOffsetPåAndel(null, 2L)),
                fagsakId = fagsak.id,
            )

        val iverksattOgLøpendeBehandlinger = avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker()

        val behandlingerMedRelevanteAndeler =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(iverksattOgLøpendeBehandlinger)
                .map { it.kildeBehandlingId }
                .distinct()

        Assertions.assertEquals(1, behandlingerMedRelevanteAndeler.size)
        Assertions.assertEquals(revurdering.id, behandlingerMedRelevanteAndeler[0])
    }

    @Test
    fun `Skal ikke plukke noe ved opphør`() {
        val forelderIdent = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(forelderIdent).also {
            fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE)
        }

        opprettOgLagreBehandlingMedAndeler(
            personIdent = forelderIdent,
            kildeOgOffsetPåAndeler = listOf(KildeOgOffsetPåAndel(null, 1L)),
            medStatus = BehandlingStatus.AVSLUTTET,
            fagsakId = fagsak.id,
        )
        opprettOgLagreRevurderingMedAndeler(
            personIdent = forelderIdent,
            kildeOgOffsetPåAndeler = emptyList(),
            fagsakId = fagsak.id,
        )

        val iverksattOgLøpendeBehandlinger = avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker()

        val behandlingerMedRelevanteAndeler =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(iverksattOgLøpendeBehandlinger)
                .map { it.kildeBehandlingId }
                .distinct()

        Assertions.assertTrue(behandlingerMedRelevanteAndeler.isEmpty())
    }

    @Test
    fun `Skal ikke plukke behandling som ikke er iverksatt`() {
        val forelderIdent = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(forelderIdent).also {
            fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE)
        }
        val iverksattBehandling =
            opprettOgLagreBehandlingMedAndeler(
                personIdent = forelderIdent,
                kildeOgOffsetPåAndeler = listOf(KildeOgOffsetPåAndel(null, 1L)),
                medStatus = BehandlingStatus.AVSLUTTET,
                fagsakId = fagsak.id,
            )

        opprettOgLagreRevurderingMedAndeler(
            personIdent = forelderIdent,
            kildeOgOffsetPåAndeler = listOf(KildeOgOffsetPåAndel(null, 2L)),
            erIverksatt = false,
            fagsakId = fagsak.id,
        )

        val iverksattOgLøpendeBehandlinger = avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker()
        val behandlingerMedRelevanteAndeler =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(iverksattOgLøpendeBehandlinger)
                .map { it.kildeBehandlingId }
                .distinct()

        Assertions.assertEquals(1, behandlingerMedRelevanteAndeler.size)
        Assertions.assertEquals(iverksattBehandling.id, behandlingerMedRelevanteAndeler[0])
    }

    private fun opprettOgLagreBehandlingMedAndeler(
        personIdent: String,
        kildeOgOffsetPåAndeler: List<KildeOgOffsetPåAndel> = emptyList(),
        erIverksatt: Boolean = true,
        medStatus: BehandlingStatus = BehandlingStatus.UTREDES,
        fagsakId: Long,
    ): Behandling {
        val behandling =
            behandlingService.opprettBehandling(nyOrdinærBehandling(søkersIdent = personIdent, fagsakId = fagsakId))
        behandling.status = medStatus
        behandlingRepository.save(behandling)
        val tilkjentYtelse = tilkjentYtelse(behandling = behandling, erIverksatt = erIverksatt)
        tilkjentYtelseRepository.save(tilkjentYtelse)
        val personFnr = randomFnr()
        val aktør = personidentService.hentOgLagreAktør(personFnr, true)
        kildeOgOffsetPåAndeler.forEach {
            andelTilkjentYtelseRepository.save(
                andelPåTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelse,
                    kildeBehandlingId = it.kilde ?: behandling.id,
                    periodeOffset = it.offset,
                    aktør = aktør,
                ),
            )
        }
        return behandling
    }

    private fun opprettOgLagreRevurderingMedAndeler(
        personIdent: String,
        kildeOgOffsetPåAndeler: List<KildeOgOffsetPåAndel> = emptyList(),
        erIverksatt: Boolean = true,
        fagsakId: Long,
    ): Behandling {
        val behandling =
            behandlingService.opprettBehandling(nyRevurdering(søkersIdent = personIdent, fagsakId = fagsakId))
        val tilkjentYtelse = tilkjentYtelse(behandling = behandling, erIverksatt = erIverksatt)
        tilkjentYtelseRepository.save(tilkjentYtelse)
        val personFnr = randomFnr()
        val aktør = personidentService.hentOgLagreAktør(personFnr, true)
        kildeOgOffsetPåAndeler.forEach {
            andelTilkjentYtelseRepository.save(
                andelPåTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelse,
                    kildeBehandlingId = it.kilde ?: behandling.id,
                    periodeOffset = it.offset,
                    aktør = aktør,
                ),
            )
        }
        return behandling
    }

    private fun tilkjentYtelse(behandling: Behandling, erIverksatt: Boolean) = TilkjentYtelse(
        behandling = behandling,
        opprettetDato = LocalDate.now(),
        endretDato = LocalDate.now(),
        utbetalingsoppdrag = if (erIverksatt) "Skal ikke være null" else null,
    )

    // Kun offset og kobling til behandling/tilkjent ytelse som er relevant når man skal plukke ut til konsistensavstemming
    private fun andelPåTilkjentYtelse(
        tilkjentYtelse: TilkjentYtelse,
        kildeBehandlingId: Long,
        periodeOffset: Long,
        aktør: Aktør = randomAktør(),
    ) = AndelTilkjentYtelse(
        behandlingId = tilkjentYtelse.behandling.id,
        tilkjentYtelse = tilkjentYtelse,
        kalkulertUtbetalingsbeløp = 1054,
        nasjonaltPeriodebeløp = 1054,
        stønadFom = LocalDate.now()
            .minusMonths(12)
            .toYearMonth(),
        stønadTom = LocalDate.now()
            .plusMonths(12)
            .toYearMonth(),
        type = YtelseType.ORDINÆR_BARNETRYGD,
        kildeBehandlingId = kildeBehandlingId,
        periodeOffset = periodeOffset,
        forrigePeriodeOffset = null,
        sats = 1054,
        prosent = BigDecimal(100),
        aktør = aktør,
    )
}

data class KildeOgOffsetPåAndel(
    val kilde: Long?, // Hvis denne er null setter vi til behandling som opprettes, for å unngå loop-avhengighet
    val offset: Long,
)
