package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.integrasjoner.ef.EfSakRestClient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlagRepository
import no.nav.familie.kontrakter.felles.ef.Datakilde
import no.nav.familie.kontrakter.felles.ef.EksternPeriode
import no.nav.familie.kontrakter.felles.ef.EksternePerioderResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SmåbarnstilleggServiceTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val efSakRestClient = mockk<EfSakRestClient>()
    private val periodeOvergangsstønadGrunnlagRepository = mockk<PeriodeOvergangsstønadGrunnlagRepository>()
    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService =
        mockk<AndelerTilkjentYtelseOgEndreteUtbetalingerService>()

    private lateinit var småbarnstilleggService: SmåbarnstilleggService

    @BeforeEach
    fun setUp() {
        småbarnstilleggService = SmåbarnstilleggService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            efSakRestClient = efSakRestClient,
            periodeOvergangsstønadGrunnlagRepository = periodeOvergangsstønadGrunnlagRepository,
            tilkjentYtelseRepository = tilkjentYtelseRepository,
            persongrunnlagService = persongrunnlagService,
            andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
        )

        every { periodeOvergangsstønadGrunnlagRepository.deleteByBehandlingId(any()) } just Runs
    }

    @Test
    fun `Ved satsendring skal gamle perioder kopieres`() {
        val søker = lagPerson(type = PersonType.SØKER)
        val fagsak = Fagsak(aktør = søker.aktør)
        val forrigeBehandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD, fagsak = fagsak)
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SATSENDRING, fagsak = fagsak)

        val perioderForrigeBehandling = listOf(
            PeriodeOvergangsstønadGrunnlag(
                fom = LocalDate.now().minusYears(1),
                tom = LocalDate.now().minusMonths(8),
                aktør = søker.aktør,
                datakilde = Datakilde.EF,
                behandlingId = forrigeBehandling.id,
            ),
            PeriodeOvergangsstønadGrunnlag(
                fom = LocalDate.now().minusMonths(7),
                tom = LocalDate.now().minusMonths(1),
                aktør = søker.aktør,
                datakilde = Datakilde.EF,
                behandlingId = forrigeBehandling.id,
            ),
        )

        val forventetNyePerioder = perioderForrigeBehandling.map {
            PeriodeOvergangsstønadGrunnlag(
                fom = it.fom,
                tom = it.tom,
                aktør = it.aktør,
                datakilde = it.datakilde,
                behandlingId = behandling.id,
            )
        }

        every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeBehandling
        every { periodeOvergangsstønadGrunnlagRepository.findByBehandlingId(forrigeBehandling.id) } returns perioderForrigeBehandling

        val slot = slot<List<PeriodeOvergangsstønadGrunnlag>>()
        every { periodeOvergangsstønadGrunnlagRepository.saveAll(capture(slot)) } returnsArgument 0

        småbarnstilleggService.hentOgLagrePerioderMedOvergangsstønadForBehandling(
            søkerAktør = søker.aktør,
            behandling = behandling,
        )

        assertThat(slot.captured).containsAll(forventetNyePerioder)
        verify(exactly = 1) { periodeOvergangsstønadGrunnlagRepository.saveAll(forventetNyePerioder) }
        verify(exactly = 0) { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) }
    }

    @Test
    fun `Vanlige behandlinger skal hente perioder fra EF`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.NYE_OPPLYSNINGER)
        val søker = lagPerson(type = PersonType.SØKER)

        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns EksternePerioderResponse(
            perioder = listOf(
                EksternPeriode(
                    personIdent = søker.aktør.aktivFødselsnummer(),
                    fomDato = LocalDate.now().minusMonths(5),
                    tomDato = LocalDate.now().minusMonths(1),
                    datakilde = Datakilde.EF,
                ),
            ),
        )
        val forventetPeriode = PeriodeOvergangsstønadGrunnlag(
            behandlingId = behandling.id,
            fom = LocalDate.now().minusMonths(5),
            tom = LocalDate.now().minusMonths(1),
            datakilde = Datakilde.EF,
            aktør = søker.aktør,
        )

        val slot = slot<List<PeriodeOvergangsstønadGrunnlag>>()
        every { periodeOvergangsstønadGrunnlagRepository.saveAll(capture(slot)) } returnsArgument 0

        småbarnstilleggService.hentOgLagrePerioderMedOvergangsstønadForBehandling(
            søkerAktør = søker.aktør,
            behandling = behandling,
        )

        verify(exactly = 1) { efSakRestClient.hentPerioderMedFullOvergangsstønad(søker.aktør.aktivFødselsnummer()) }
        assertThat(slot.captured).containsExactly(
            forventetPeriode,
        )
    }
}
