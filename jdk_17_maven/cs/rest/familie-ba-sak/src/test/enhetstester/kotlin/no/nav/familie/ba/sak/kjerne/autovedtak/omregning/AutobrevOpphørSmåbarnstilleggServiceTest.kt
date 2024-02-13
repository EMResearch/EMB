package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.StartSatsendring
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.kontrakter.felles.ef.Datakilde
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class AutobrevOpphørSmåbarnstilleggServiceTest {
    private val autovedtakService = mockk<AutovedtakService>()
    private val autovedtakStegService = mockk<AutovedtakStegService>()
    private val fagsakService = mockk<FagsakService>(relaxed = true)
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val behandlingService = mockk<BehandlingService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val infotrygdService = mockk<InfotrygdService>(relaxed = true)
    private val stegService = mockk<StegService>()
    private val vedtakService = mockk<VedtakService>(relaxed = true)
    private val taskRepository = mockk<TaskRepositoryWrapper>(relaxed = true)
    private val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    private val periodeOvergangsstønadGrunnlagRepository = mockk<PeriodeOvergangsstønadGrunnlagRepository>()
    private val startSatsendring = mockk<StartSatsendring>(relaxed = true)

    private val autovedtakBrevService = AutovedtakBrevService(
        fagsakService = fagsakService,
        behandlingService = behandlingService,
        behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        infotrygdService = infotrygdService,
        autovedtakService = autovedtakService,
        vedtakService = vedtakService,
        vedtaksperiodeService = vedtaksperiodeService,
        taskRepository = taskRepository,
    )

    private val autobrevOpphørSmåbarnstilleggService = AutobrevOpphørSmåbarnstilleggService(
        autovedtakBrevService = autovedtakBrevService,
        persongrunnlagService = persongrunnlagService,
        behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        periodeOvergangsstønadGrunnlagRepository = periodeOvergangsstønadGrunnlagRepository,
        autovedtakStegService = autovedtakStegService,
        startSatsendring = startSatsendring,
    )

    @Test
    fun `Verifiser at løpende fagsak med småbarnstillegg sender opphørsbrev måneden etter yngste barn ble 3 år`() {
        val behandling = lagBehandling()
        val barn3ÅrForrigeMåned = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(3).minusMonths(1))
        val personopplysningGrunnlag: PersonopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandlingId = behandlingId, barn3ÅrForrigeMåned)

        every { behandlingHentOgPersisterService.finnAktivForFagsak(any()) } returns behandling
        every { behandlingHentOgPersisterService.hentBehandlinger(any()) } returns listOf(behandling)
        every { behandlingService.harBehandlingsårsakAlleredeKjørt(any(), any(), any()) } returns false
        every { persongrunnlagService.hentAktivThrows(any()) } returns personopplysningGrunnlag
        every { periodeOvergangsstønadGrunnlagRepository.findByBehandlingId(any()) } returns emptyList()
        every { stegService.håndterVilkårsvurdering(any()) } returns behandling
        every { stegService.håndterNyBehandling(any()) } returns behandling
        every { vedtaksperiodeService.oppdaterFortsattInnvilgetPeriodeMedAutobrevBegrunnelse(any(), any()) } just runs
        every { autovedtakStegService.kjørBehandlingOmregning(any(), any()) } returns ""

        autobrevOpphørSmåbarnstilleggService
            .kjørBehandlingOgSendBrevForOpphørAvSmåbarnstillegg(fagsakId = behandling.fagsak.id)

        verify(exactly = 1) {
            autovedtakStegService.kjørBehandlingOmregning(
                any(),
                any(),
            )
        }
    }

    @Test
    fun `Skal ikke sende lage autobrevbehandling om det i forrige måned ble vedtatt en reduksjon på småbarnstillegg`() {
        val behandling = lagBehandling().apply {
            status = BehandlingStatus.AVSLUTTET
        }
        val vedtak = lagVedtak(behandling = behandling)

        val barn3ÅrForrigeMåned = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(3).minusMonths(1))
        val personopplysningGrunnlag: PersonopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandlingId = behandlingId, barn3ÅrForrigeMåned)

        val vedtaksperioder = listOf(
            VedtaksperiodeMedBegrunnelser(
                fom = LocalDate.now().førsteDagIInneværendeMåned(),
                vedtak = vedtak,
                type = Vedtaksperiodetype.UTBETALING,
            ).apply {
                begrunnelser.addAll(
                    listOf(
                        Standardbegrunnelse.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_BARN_UNDER_TRE_ÅR,
                    ).map { begrunnelse ->
                        Vedtaksbegrunnelse(
                            vedtaksperiodeMedBegrunnelser = this,
                            standardbegrunnelse = begrunnelse,
                        )
                    },
                )
            },
        )

        every { behandlingHentOgPersisterService.finnAktivForFagsak(any()) } returns behandling
        every { behandlingHentOgPersisterService.hentBehandlinger(behandling.fagsak.id) } returns listOf(behandling)
        every { vedtaksperiodeService.hentPersisterteVedtaksperioder(any()) } returns vedtaksperioder
        every { behandlingService.harBehandlingsårsakAlleredeKjørt(any(), any(), any()) } returns false
        every { persongrunnlagService.hentAktivThrows(any()) } returns personopplysningGrunnlag
        every { periodeOvergangsstønadGrunnlagRepository.findByBehandlingId(any()) } returns listOf(
            lagPeriodeOvergangsstønadGrunnlag(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1)),
        )

        autobrevOpphørSmåbarnstilleggService
            .kjørBehandlingOgSendBrevForOpphørAvSmåbarnstillegg(fagsakId = behandling.fagsak.id)

        verify(exactly = 0) {
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                any(),
                any(),
                any(),
                any(),
            )
        }

        verify(exactly = 0) { taskRepository.save(any()) }
    }

    @Test
    fun `Verifiser at behandling ikke blir opprettet om behandling allerede har kjørt`() {
        val behandling = lagBehandling()
        val barn3ÅrForrigeMåned = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(3).minusMonths(1))
        val personopplysningGrunnlag: PersonopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandlingId = behandlingId, barn3ÅrForrigeMåned)

        every { behandlingHentOgPersisterService.finnAktivForFagsak(any()) } returns behandling
        every { behandlingService.harBehandlingsårsakAlleredeKjørt(any(), any(), any()) } returns true
        every { persongrunnlagService.hentAktivThrows(any()) } returns personopplysningGrunnlag
        every { periodeOvergangsstønadGrunnlagRepository.findByBehandlingId(any()) } returns emptyList()
        every { stegService.håndterVilkårsvurdering(any()) } returns behandling
        every { stegService.håndterNyBehandling(any()) } returns behandling
        every { vedtaksperiodeService.oppdaterFortsattInnvilgetPeriodeMedAutobrevBegrunnelse(any(), any()) } just runs

        autobrevOpphørSmåbarnstilleggService
            .kjørBehandlingOgSendBrevForOpphørAvSmåbarnstillegg(fagsakId = behandling.fagsak.id)

        verify(exactly = 0) {
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                any(),
                any(),
                any(),
                any(),
            )
        }

        verify(exactly = 0) { taskRepository.save(any()) }
    }

    @Test
    fun `overgangstønadOpphørteForrigeMåned - en periode med opphør denne måneden gir false`() {
        val fom = LocalDate.now().minusYears(1)
        val tom = LocalDate.now()
        val input: List<PeriodeOvergangsstønadGrunnlag> = listOf(
            lagPeriodeOvergangsstønadGrunnlag(fom, tom),
        )
        val overgangstønadOpphørteForrigeMåned =
            autobrevOpphørSmåbarnstilleggService.overgangstønadOpphørteForrigeMåned(input)
        assertFalse(overgangstønadOpphørteForrigeMåned)
    }

    @Test
    fun `overgangstønadOpphørteForrigeMåned - tom liste gir false`() {
        val input: List<PeriodeOvergangsstønadGrunnlag> = emptyList()
        val overgangstønadOpphørteForrigeMåned =
            autobrevOpphørSmåbarnstilleggService.overgangstønadOpphørteForrigeMåned(input)
        assertFalse(overgangstønadOpphørteForrigeMåned)
    }

    @Test
    fun `overgangstønadOpphørteForrigeMåned - neste måned gir false`() {
        val fom = LocalDate.now().minusYears(1)
        val tom = LocalDate.now().førsteDagINesteMåned()
        val input: List<PeriodeOvergangsstønadGrunnlag> = listOf(
            lagPeriodeOvergangsstønadGrunnlag(fom, tom),
        )
        val overgangstønadOpphørteForrigeMåned =
            autobrevOpphørSmåbarnstilleggService.overgangstønadOpphørteForrigeMåned(input)
        assertFalse(overgangstønadOpphørteForrigeMåned)
    }

    @Test
    fun `overgangstønadOpphørteForrigeMåned - forrige måned gir true`() {
        val fom = LocalDate.now().minusYears(1)
        val tom = LocalDate.now().minusMonths(1)
        val input: List<PeriodeOvergangsstønadGrunnlag> = listOf(
            lagPeriodeOvergangsstønadGrunnlag(fom, tom),
        )
        val overgangstønadOpphørteForrigeMåned =
            autobrevOpphørSmåbarnstilleggService.overgangstønadOpphørteForrigeMåned(input)
        assertTrue(overgangstønadOpphørteForrigeMåned)
    }

    @Test
    fun `overgangstønadOpphørteForrigeMåned - ett år siden gir false`() {
        val fom = LocalDate.now().minusYears(1)
        val tom = LocalDate.now().minusYears(1)
        val input: List<PeriodeOvergangsstønadGrunnlag> = listOf(
            lagPeriodeOvergangsstønadGrunnlag(fom, tom),
        )
        val overgangstønadOpphørteForrigeMåned =
            autobrevOpphørSmåbarnstilleggService.overgangstønadOpphørteForrigeMåned(input)
        assertFalse(overgangstønadOpphørteForrigeMåned)
    }

    val behandlingId: Long = 1

    @Test
    fun `minsteBarnFylteTreÅrForrigeMåned - et barn som fylte tre forrige måned gir true`() {
        val barn3ForrigeMåned = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(3).minusMonths(1))
        val peronsopplysningGrunnalg: PersonopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandlingId = behandlingId, barn3ForrigeMåned)

        assertTrue(autobrevOpphørSmåbarnstilleggService.yngsteBarnFylteTreÅrForrigeMåned(peronsopplysningGrunnalg))
    }

    @Test
    fun `minsteBarnFylteTreÅrForrigeMåned - et barn som fylte tre forrige måned og et eldre gir true`() {
        val barn3ForrigeMåned = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(3).minusMonths(1))
        val barnOverTre = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(4))
        val peronsopplysningGrunnalg: PersonopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandlingId = behandlingId, barn3ForrigeMåned, barnOverTre)

        assertTrue(autobrevOpphørSmåbarnstilleggService.yngsteBarnFylteTreÅrForrigeMåned(peronsopplysningGrunnalg))
    }

    @Test
    fun `minsteBarnFylteTreÅrForrigeMåned - to barn som fylte tre forrige måned gir true`() {
        val barn3ForrigeMåned = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(3).minusMonths(1))
        val ekstraBarn3ForrigeMåned = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(3).minusMonths(1))
        val peronsopplysningGrunnalg: PersonopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandlingId = behandlingId, barn3ForrigeMåned, ekstraBarn3ForrigeMåned)

        assertTrue(autobrevOpphørSmåbarnstilleggService.yngsteBarnFylteTreÅrForrigeMåned(peronsopplysningGrunnalg))
    }

    @Test
    fun `minsteBarnFylteTreÅrForrigeMåned - to barn over tre gir false`() {
        val barnOverTre = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(4))
        val ekstraBarnOverTre = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(4))
        val peronsopplysningGrunnalg: PersonopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandlingId = behandlingId, barnOverTre, ekstraBarnOverTre)

        assertFalse(autobrevOpphørSmåbarnstilleggService.yngsteBarnFylteTreÅrForrigeMåned(peronsopplysningGrunnalg))
    }

    @Test
    fun `minsteBarnFylteTreÅrForrigeMåned - to barn under tre gir false`() {
        val barnUnderTre = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(2))
        val ekstraBarnUnderTre = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(3))
        val peronsopplysningGrunnalg: PersonopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandlingId = behandlingId, barnUnderTre, ekstraBarnUnderTre)

        assertFalse(autobrevOpphørSmåbarnstilleggService.yngsteBarnFylteTreÅrForrigeMåned(peronsopplysningGrunnalg))
    }

    @Test
    fun `minsteBarnFylteTreÅrForrigeMåned - et barn under tre og et barn 3 forrige måned gir false`() {
        val barnUnderTre = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(2))
        val barn3ForrigeMåned = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(3).minusMonths(1))
        val peronsopplysningGrunnalg: PersonopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandlingId = behandlingId, barnUnderTre, barn3ForrigeMåned)

        assertFalse(autobrevOpphørSmåbarnstilleggService.yngsteBarnFylteTreÅrForrigeMåned(peronsopplysningGrunnalg))
    }

    private fun lagPeriodeOvergangsstønadGrunnlag(
        fom: LocalDate,
        tom: LocalDate,
    ) = PeriodeOvergangsstønadGrunnlag(
        id = 1,
        behandlingId = 1,
        aktør = randomAktør(),
        fom = fom,
        tom = tom,
        datakilde = Datakilde.EF,
    )
}
