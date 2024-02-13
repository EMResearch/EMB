package no.nav.familie.ba.sak.kjerne.behandling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingMigreringsinfoRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

class LagreMigreringsdatoTest {
    val behandlingstemaService = mockk<BehandlingstemaService>()
    val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>(relaxed = true)
    val behandlingSøknadsinfoService = mockk<BehandlingSøknadsinfoService>()
    val beregningService = mockk<BeregningService>()
    val personopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    val behandlingMetrikker = mockk<BehandlingMetrikker>()
    val fagsakRepository = mockk<FagsakRepository>()
    val vedtakRepository = mockk<VedtakRepository>()
    val loggService = mockk<LoggService>()
    val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    val saksstatistikkEventPublisher = mockk<SaksstatistikkEventPublisher>()
    val infotrygdService = mockk<InfotrygdService>()
    val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    val taskRepository = mockk<TaskRepositoryWrapper>()
    val behandlingMigreringsinfoRepository = mockk<BehandlingMigreringsinfoRepository>()
    val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    val simuleringService = mockk<SimuleringService>()

    private val behandlingService = BehandlingService(
        behandlingHentOgPersisterService,
        behandlingstemaService,
        behandlingSøknadsinfoService,
        behandlingMigreringsinfoRepository,
        behandlingMetrikker,
        saksstatistikkEventPublisher,
        fagsakRepository,
        vedtakRepository,
        andelTilkjentYtelseRepository,
        loggService,
        arbeidsfordelingService,
        infotrygdService,
        vedtaksperiodeService,
        taskRepository,
        vilkårsvurderingService,
    )

    @Test
    fun `Lagre første migreringstidspunkt skal ikke kaste feil`() {
        every { behandlingMigreringsinfoRepository.finnSisteMigreringsdatoPåFagsak(any()) } returns null
        every { behandlingMigreringsinfoRepository.save(any()) } returns mockk()
        every { vilkårsvurderingService.hentTidligsteVilkårsvurderingKnyttetTilMigrering(any()) } returns YearMonth.now()
        every { behandlingHentOgPersisterService.hentBehandlinger(any()) } returns emptyList()

        assertDoesNotThrow {
            behandlingService.lagreNedMigreringsdato(
                migreringsdato = LocalDate.now(),
                behandling = lagBehandling(),
            )
        }
    }

    @Test
    fun `Lagre likt migreringstidspunkt skal kaste feil`() {
        every { behandlingMigreringsinfoRepository.finnSisteMigreringsdatoPåFagsak(any()) } returns LocalDate.now()
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns null
        every { behandlingMigreringsinfoRepository.save(any()) } returns mockk()

        val feil = assertThrows<FunksjonellFeil> {
            behandlingService.lagreNedMigreringsdato(
                migreringsdato = LocalDate.now(),
                behandling = lagBehandling(
                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                    årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
                ),
            )
        }
        assertEquals(
            "Migreringsdatoen du har lagt inn er lik eller senere enn eksisterende migreringsdato. Du må velge en tidligere migreringsdato for å fortsette.",
            feil.melding,
        )
    }

    @Test
    fun `Lagre tidligere migreringstidspunkt skal ikke kaste feil`() {
        every { behandlingMigreringsinfoRepository.finnSisteMigreringsdatoPåFagsak(any()) } returns LocalDate.now()
        every { behandlingHentOgPersisterService.hentBehandlinger(any()) } returns emptyList()
        every { behandlingMigreringsinfoRepository.save(any()) } returns mockk()

        assertDoesNotThrow {
            behandlingService.lagreNedMigreringsdato(
                migreringsdato = LocalDate.now().minusMonths(1),
                behandling = lagBehandling(
                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                    årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
                ),
            )
        }
    }

    @Test
    fun `Lagre tidligere migreringstidspunkt skal kaste feil dersom forrige behandling ikke har lagret migreringsdato`() {
        every { behandlingMigreringsinfoRepository.finnSisteMigreringsdatoPåFagsak(any()) } returns null
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns
            lagBehandling(behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD).also {
                it.status = BehandlingStatus.AVSLUTTET
                it.resultat = Behandlingsresultat.INNVILGET
            }
        every { vilkårsvurderingService.hentTidligsteVilkårsvurderingKnyttetTilMigrering(any()) } returns YearMonth.now()

        every { behandlingMigreringsinfoRepository.save(any()) } returns mockk()

        val feil = assertThrows<FunksjonellFeil> {
            behandlingService.lagreNedMigreringsdato(
                migreringsdato = LocalDate.now(),
                behandling = lagBehandling(
                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                    årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
                ),
            )
        }
        assertEquals(
            "Migreringsdatoen du har lagt inn er lik eller senere enn eksisterende migreringsdato. Du må velge en tidligere migreringsdato for å fortsette.",
            feil.melding,
        )
    }

    @Test
    fun `Lagre tidligere migreringstidspunkt skal ikke kaste feil dersom forrige behandling ikke er migreringsbehandling`() {
        every { behandlingMigreringsinfoRepository.finnSisteMigreringsdatoPåFagsak(any()) } returns null
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns
            lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING).also {
                it.status = BehandlingStatus.AVSLUTTET
                it.resultat = Behandlingsresultat.INNVILGET
            }
        every { vilkårsvurderingService.hentTidligsteVilkårsvurderingKnyttetTilMigrering(any()) } returns YearMonth.now()

        every { behandlingMigreringsinfoRepository.save(any()) } returns mockk()

        assertDoesNotThrow {
            behandlingService.lagreNedMigreringsdato(
                migreringsdato = LocalDate.now(),
                behandling = lagBehandling(
                    behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                    årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
                ),
            )
        }
    }
}
