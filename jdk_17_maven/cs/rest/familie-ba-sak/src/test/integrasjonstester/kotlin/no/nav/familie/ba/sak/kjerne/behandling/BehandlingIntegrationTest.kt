package no.nav.familie.ba.sak.kjerne.behandling

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagPersonResultaterForSøkerOgToBarn
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPersonerMedAndeler
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.SatsTidspunkt
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRequest
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse.Companion.sisteAdresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrMatrikkeladresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrUkjentBosted
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrVegadresse
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.BehandlingStegStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth

class BehandlingIntegrationTest(
    @Autowired
    private val behandlingRepository: BehandlingRepository,

    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,

    @Autowired
    private val personRepository: PersonRepository,

    @Autowired
    private val vedtakService: VedtakService,

    @Autowired
    private val persongrunnlagService: PersongrunnlagService,

    @Autowired
    private val beregningService: BeregningService,

    @Autowired
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,

    @Autowired
    private val vilkårsvurderingService: VilkårsvurderingService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val mockPersonopplysningerService: PersonopplysningerService,

    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,

    @Autowired
    private val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository,

    @Autowired
    private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,

    @Autowired
    private val personidentService: PersonidentService,

    @Autowired
    private val taskRepository: TaskRepositoryWrapper,

    @Autowired
    private val vedtaksperiodeService: VedtaksperiodeService,
) : AbstractSpringIntegrationTest() {

    @BeforeEach
    fun truncate() {
        databaseCleanupService.truncate()
    }

    @BeforeEach
    fun førHverTest() {
        mockkObject(SatsTidspunkt)
        every { SatsTidspunkt.senesteSatsTidspunkt } returns LocalDate.of(2022, 12, 31)
    }

    @AfterEach
    fun etterHverTest() {
        unmockkObject(SatsTidspunkt)
    }

    @Test
    fun `Kjør flyway migreringer og sjekk at behandlingslagerservice klarer å lese å skrive til postgresql`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        behandlingService.opprettBehandling(
            nyOrdinærBehandling(
                søkersIdent = fnr,
                fagsakId = fagsak.id,
            ),
        )
        assertEquals(1, behandlingHentOgPersisterService.hentBehandlinger(fagsak.id).size)
    }

    @Test
    fun `Test at opprettEllerOppdaterBehandling kjører uten feil`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        behandlingService.opprettBehandling(
            nyOrdinærBehandling(
                søkersIdent = fnr,
                fagsakId = fagsak.id,
            ),
        )
        assertEquals(
            1,
            behandlingHentOgPersisterService.hentBehandlinger(fagsak.id).size,
        )
    }

    @Test
    fun `Opprett behandling`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        assertEquals(fagsak.id, behandling.fagsak.id)
    }

    @Test
    fun `Kast feil ved opprettelse av behandling for ny person med åpen sak i Infotrygd`() {
        val fnr = randomFnr()
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)

        every { infotrygdBarnetrygdClient.harÅpenSakIInfotrygd(listOf(fnr)) } returns true

        assertThatThrownBy { behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak)) }
            .hasMessageContaining("sak i Infotrygd")
    }

    @Test
    fun `Kast feil ved opprettelse av behandling for ny person med løpende sak i Infotrygd, utenom migrering`() {
        val fnr = randomFnr()
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)

        every { infotrygdBarnetrygdClient.harLøpendeSakIInfotrygd(listOf(fnr)) } returns true

        assertThatThrownBy { behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak)) }
            .hasMessageContaining("sak i Infotrygd")

        val behandling = behandlingService.opprettBehandling(
            NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                skalBehandlesAutomatisk = true,
                søkersIdent = fnr,
                fagsakId = fagsak.id,
            ),
        )
        assertNotNull(vedtakService.hentAktivForBehandling(behandlingId = behandling.id))
        markerBehandlingSomAvsluttet(behandling)
        assertDoesNotThrow {
            behandlingService.lagreNyOgDeaktiverGammelBehandling(
                lagBehandling(
                    fagsak,
                    behandlingType = BehandlingType.REVURDERING,
                ),
            )
        }
    }

    @Test
    fun `Opprett behandle sak oppgave ved opprettelse av førstegangsbehandling`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        behandlingService.opprettBehandling(nyOrdinærBehandling(søkersIdent = fnr, fagsakId = fagsak.id))

        verify(exactly = 1) {
            taskRepository.save(any())
        }
    }

    @Test
    fun `Opprett aktivt vedtak ved opprettelse av behandling`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling =
            behandlingService.opprettBehandling(nyOrdinærBehandling(søkersIdent = fnr, fagsakId = fagsak.id))

        assertNotNull(vedtakService.hentAktivForBehandling(behandlingId = behandling.id))
    }

    @Test
    fun `Ikke opprett behandle sak oppgave ved opprettelse av fødselshendelsebehandling`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        behandlingService.opprettBehandling(
            NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                søkersIdent = fnr,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                skalBehandlesAutomatisk = true,
                søknadMottattDato = LocalDate.now(),
                fagsakId = fagsak.id,
            ),
        )

        verify(exactly = 0) {
            taskRepository.save(any())
        }
    }

    @Test
    fun `Kast feil om man lager ny behandling på fagsak som har behandling som skal godkjennes`() {
        val morId = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = morId))
        val behandling =
            behandlingService.opprettBehandling(nyOrdinærBehandling(søkersIdent = morId, fagsakId = fagsak.data!!.id))
        behandling.behandlingStegTilstand.forEach { it.behandlingStegStatus = BehandlingStegStatus.UTFØRT }
        behandling.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandling = behandling,
                behandlingSteg = StegType.BESLUTTE_VEDTAK,
            ),
        )
        behandlingRepository.saveAndFlush(behandling)

        assertThrows(Exception::class.java) {
            behandlingService.opprettBehandling(
                NyBehandling(
                    BehandlingKategori.NASJONAL,
                    BehandlingUnderkategori.ORDINÆR,
                    morId,
                    BehandlingType.REVURDERING,
                    BehandlingÅrsak.SØKNAD,
                    fagsakId = fagsak.data!!.id,
                ),
            )
        }
    }

    @Test
    fun `Bruk samme behandling hvis nytt barn kommer på fagsak med aktiv behandling`() {
        val morId = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(morId)
        behandlingService.opprettBehandling(nyOrdinærBehandling(søkersIdent = morId, fagsakId = fagsak.id))

        assertEquals(1, behandlingHentOgPersisterService.hentBehandlinger(fagsakId = fagsak.id).size)

        behandlingService.opprettBehandling(
            NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                søkersIdent = morId,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.SØKNAD,
                søknadMottattDato = LocalDate.now(),
                fagsakId = fagsak.id,
            ),
        )

        val behandlinger = behandlingHentOgPersisterService.hentBehandlinger(fagsakId = fagsak.id)
        assertEquals(1, behandlinger.size)
    }

    @Test
    fun `Opprett barnas beregning på vedtak`() {
        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()

        val søkerAktørId = personidentService.hentAktør(søkerFnr)
        val barn1AktørId = personidentService.hentAktør(barn1Fnr)
        val barn2AktørId = personidentService.hentAktør(barn2Fnr)

        val januar2020 = YearMonth.of(2020, 1)
        val oktober2020 = YearMonth.of(2020, 10)
        val stønadTom = januar2020.plusYears(17)

        val fagsak = fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerFnr))
        val behandling = behandlingService.opprettBehandling(
            nyOrdinærBehandling(
                søkersIdent = søkerFnr,
                fagsakId = fagsak.data!!.id,
            ),
        )

        val barnAktør = personidentService.hentOgLagreAktørIder(listOf(barn1Fnr, barn2Fnr), true)
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                søkerFnr,
                listOf(barn1Fnr, barn2Fnr),
                søkerAktør = behandling.fagsak.aktør,
                barnAktør = barnAktør,
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling = behandling)

        val vilkårsvurdering =
            Vilkårsvurdering(behandling = behandling)
        vilkårsvurdering.personResultater = setOf(
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                person = lagPerson(type = PersonType.SØKER, aktør = søkerAktørId),
                resultat = Resultat.OPPFYLT,
                periodeFom = januar2020.minusMonths(1).toLocalDate(),
                periodeTom = stønadTom.toLocalDate(),
                lagFullstendigVilkårResultat = true,
                personType = PersonType.SØKER,
            ),
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                person = lagPerson(type = PersonType.BARN, aktør = barn1AktørId, fødselsdato = januar2020.minusYears(2).førsteDagIInneværendeMåned()),
                resultat = Resultat.OPPFYLT,
                periodeFom = januar2020.minusMonths(1).toLocalDate(),
                periodeTom = stønadTom.toLocalDate(),
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
            ),
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                person = lagPerson(type = PersonType.BARN, aktør = barn2AktørId, fødselsdato = januar2020.førsteDagIInneværendeMåned()),
                resultat = Resultat.OPPFYLT,
                periodeFom = oktober2020.minusMonths(1).toLocalDate(),
                periodeTom = stønadTom.toLocalDate(),
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
            ),
        )
        vilkårsvurderingRepository.save(vilkårsvurdering)

        val tilkjentYtelse = beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
        val restVedtakBarnMap =
            personopplysningGrunnlag.tilRestPersonerMedAndeler(andelerKnyttetTilPersoner = tilkjentYtelse.andelerTilkjentYtelse.toList())
                .associateBy(
                    { it.personIdent },
                    { restPersonMedAndeler -> restPersonMedAndeler.ytelsePerioder.sortedBy { it.stønadFom } },
                )

        val satsEndringDatoSeptember2020 =
            SatsService.hentDatoForSatsendring(SatsType.TILLEGG_ORBA, 1354)!!.toYearMonth()
        val satsEndringDatoSeptember2021 =
            SatsService.hentDatoForSatsendring(SatsType.TILLEGG_ORBA, 1654)!!.toYearMonth()
        val satsEndringDatoJanuar2022 = SatsService.hentDatoForSatsendring(SatsType.TILLEGG_ORBA, 1676)!!.toYearMonth()
        assertEquals(2, restVedtakBarnMap.size)

        // Barn 1
        val barn1Perioder = restVedtakBarnMap[barn1Fnr]!!.sortedBy { it.stønadFom }
        assertEquals(1054, barn1Perioder[0].beløp)
        assertEquals(januar2020, barn1Perioder[0].stønadFom)
        assertEquals(satsEndringDatoSeptember2020.minusMonths(1), barn1Perioder[0].stønadTom)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, barn1Perioder[0].ytelseType)
        assertEquals(1354, barn1Perioder[1].beløp)
        assertEquals(satsEndringDatoSeptember2020, barn1Perioder[1].stønadFom)
        assertEquals(satsEndringDatoSeptember2021.minusMonths(1), barn1Perioder[1].stønadTom)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, barn1Perioder[1].ytelseType)
        assertEquals(1654, barn1Perioder[2].beløp)
        assertEquals(satsEndringDatoSeptember2021, barn1Perioder[2].stønadFom)
        assertTrue(januar2020 < barn1Perioder[2].stønadTom)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, barn1Perioder[2].ytelseType)
        assertEquals(1676, barn1Perioder[3].beløp)
        assertEquals(satsEndringDatoJanuar2022, barn1Perioder[3].stønadFom)
        assertTrue(januar2020 < barn1Perioder[3].stønadTom)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, barn1Perioder[3].ytelseType)
        assertEquals(1054, barn1Perioder[4].beløp)

        // Barn 2
        val barn2Perioder = restVedtakBarnMap[barn2Fnr]!!.sortedBy { it.stønadFom }
        assertEquals(1354, barn2Perioder[0].beløp)
        assertEquals(oktober2020, barn2Perioder[0].stønadFom)
        assertTrue(oktober2020 < barn2Perioder[0].stønadTom)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, barn2Perioder[0].ytelseType)
        assertEquals(1654, barn2Perioder[1].beløp)
        assertEquals(satsEndringDatoSeptember2021, barn2Perioder[1].stønadFom)
        assertTrue(januar2020 < barn2Perioder[1].stønadTom)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, barn2Perioder[1].ytelseType)
        assertEquals(1676, barn2Perioder[2].beløp)
        assertEquals(satsEndringDatoJanuar2022, barn2Perioder[2].stønadFom)
        assertTrue(januar2020 < barn2Perioder[2].stønadTom)
        assertEquals(YtelseType.ORDINÆR_BARNETRYGD, barn2Perioder[2].ytelseType)
        assertEquals(1054, barn2Perioder[3].beløp)
    }

    @Test
    fun `Endre barnas beregning på vedtak`() {
        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()
        val barn3Fnr = randomFnr()

        val søkerAktørId = personidentService.hentOgLagreAktør(søkerFnr, true)
        val barn1AktørId = personidentService.hentOgLagreAktør(barn1Fnr, true)
        val barn2AktørId = personidentService.hentOgLagreAktør(barn2Fnr, true)
        val barn3AktørId = personidentService.hentOgLagreAktør(barn3Fnr, true)

        val januar2020 = YearMonth.of(2020, 1)
        val januar2021 = YearMonth.of(2021, 1)
        val stønadTom = januar2020.plusYears(17)

        val fagsak = fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerFnr))
        val behandling = behandlingService.opprettBehandling(
            nyOrdinærBehandling(
                søkersIdent = søkerFnr,
                fagsakId = fagsak.data!!.id,
            ),
        )

        val barnAktør = personidentService.hentOgLagreAktørIder(listOf(barn1Fnr, barn2Fnr, barn3Fnr), true)
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                søkerFnr,
                listOf(barn1Fnr, barn2Fnr, barn3Fnr),
                søkerAktør = behandling.fagsak.aktør,
                barnAktør = barnAktør,
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        assertNotNull(personopplysningGrunnlag)

        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling = behandling)

        val vilkårsvurdering =
            Vilkårsvurdering(behandling = behandling)
        vilkårsvurdering.personResultater = lagPersonResultaterForSøkerOgToBarn(
            vilkårsvurdering,
            søkerAktørId,
            barn1AktørId,
            barn2AktørId,
            januar2020.minusMonths(1).toLocalDate(),
            stønadTom.toLocalDate(),
        )
        vilkårsvurderingRepository.save(vilkårsvurdering)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        val vilkårsvurdering2 =
            Vilkårsvurdering(behandling = behandling)
        vilkårsvurdering2.personResultater = lagPersonResultaterForSøkerOgToBarn(
            vilkårsvurdering2,
            søkerAktørId,
            barn1AktørId,
            barn3AktørId,
            januar2021.minusMonths(1).toLocalDate(),
            stønadTom.toLocalDate(),
        )
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering2)

        val satsEndringDatoSeptember2021 =
            SatsService.hentDatoForSatsendring(SatsType.TILLEGG_ORBA, 1654)!!.toYearMonth()
        val satsEndringDatoJanuar2022 = SatsService.hentDatoForSatsendring(SatsType.TILLEGG_ORBA, 1676)!!.toYearMonth()

        val tilkjentYtelse = beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
        val restVedtakBarnMap =
            personopplysningGrunnlag.tilRestPersonerMedAndeler(andelerKnyttetTilPersoner = tilkjentYtelse.andelerTilkjentYtelse.toList())
                .associateBy(
                    { it.personIdent },
                    { restPersonMedAndeler -> restPersonMedAndeler.ytelsePerioder.sortedBy { it.stønadFom } },
                )

        assertEquals(2, restVedtakBarnMap.size)

        // Barn 1
        val barn1Perioder = restVedtakBarnMap[barn1Fnr]!!.sortedBy { it.stønadFom }
        assertEquals(4, barn1Perioder.size)
        assertEquals(1354, barn1Perioder[0].beløp)
        assertEquals(januar2021, barn1Perioder[0].stønadFom)
        assertEquals(satsEndringDatoSeptember2021.minusMonths(1), barn1Perioder[0].stønadTom)
        assertEquals(1654, barn1Perioder[1].beløp)
        assertEquals(satsEndringDatoSeptember2021, barn1Perioder[1].stønadFom)
        assertEquals(1676, barn1Perioder[2].beløp)
        assertEquals(satsEndringDatoJanuar2022, barn1Perioder[2].stønadFom)
        assertEquals(1054, barn1Perioder[3].beløp)
        assertTrue(stønadTom >= barn1Perioder[3].stønadTom)

        // Barn 3
        val barn3perioder = restVedtakBarnMap[barn3Fnr]!!.sortedBy { it.stønadFom }
        assertEquals(4, barn3perioder.size)
        assertEquals(1354, barn3perioder[0].beløp)
        assertEquals(januar2021, barn3perioder[0].stønadFom)
        assertEquals(satsEndringDatoSeptember2021.minusMonths(1), barn3perioder[0].stønadTom)
        assertEquals(1654, barn3perioder[1].beløp)
        assertEquals(satsEndringDatoSeptember2021, barn3perioder[1].stønadFom)
        assertEquals(1676, barn3perioder[2].beløp)
        assertEquals(satsEndringDatoJanuar2022, barn3perioder[2].stønadFom)
        assertEquals(1054, barn3perioder[3].beløp)
        assertTrue(stønadTom >= barn3perioder[3].stønadTom)
    }

    @Test
    fun `Hent en persons bostedsadresse fra PDL og lagre den i database`() {
        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()

        val matrikkelId = 123456L
        val søkerHusnummer = "12"
        val søkerHusbokstav = "A"
        val søkerBruksenhetsnummer = "H012"
        val søkerAdressnavn = "Sannergate"
        val søkerKommunenummer = "1234"
        val søkerTilleggsnavn = "whatever"
        val søkerPostnummer = "2222"

        val barn1Bruksenhetsnummer = "H201"
        val barn1Tilleggsnavn = "whoknows"
        val barn1Postnummer = "3333"
        val barn1Kommunenummer = "3233"
        val barn2BostedKommune = "Oslo"

        every { mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(tilAktør(søkerFnr)) } returns PersonInfo(
            fødselsdato = LocalDate.of(1990, 1, 1),
            adressebeskyttelseGradering = null,
            navn = "Mor",
            kjønn = Kjønn.KVINNE,
            forelderBarnRelasjon = emptySet(),
            bostedsadresser = mutableListOf(
                Bostedsadresse(
                    vegadresse = Vegadresse(
                        matrikkelId,
                        søkerHusnummer,
                        søkerHusbokstav,
                        søkerBruksenhetsnummer,
                        søkerAdressnavn,
                        søkerKommunenummer,
                        søkerTilleggsnavn,
                        søkerPostnummer,
                    ),
                ),
            ),
            sivilstander = listOf(Sivilstand(type = SIVILSTAND.UOPPGITT)),
        )

        every { mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(tilAktør(barn1Fnr)) } returns PersonInfo(
            fødselsdato = LocalDate.of(2009, 1, 1),
            adressebeskyttelseGradering = null,
            navn = "Gutt",
            kjønn = Kjønn.MANN,
            forelderBarnRelasjon = emptySet(),
            bostedsadresser = mutableListOf(
                Bostedsadresse(
                    matrikkeladresse = Matrikkeladresse(
                        matrikkelId,
                        barn1Bruksenhetsnummer,
                        barn1Tilleggsnavn,
                        barn1Postnummer,
                        barn1Kommunenummer,
                    ),
                ),
            ),
            sivilstander = listOf(Sivilstand(type = SIVILSTAND.UOPPGITT)),
        )

        every { mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(tilAktør(barn2Fnr)) } returns PersonInfo(
            fødselsdato = LocalDate.of(2012, 1, 1),
            adressebeskyttelseGradering = null,
            navn = "Jente",
            kjønn = Kjønn.KVINNE,
            forelderBarnRelasjon = emptySet(),
            bostedsadresser = mutableListOf(Bostedsadresse(ukjentBosted = UkjentBosted(barn2BostedKommune))),
            sivilstander = listOf(Sivilstand(type = SIVILSTAND.UOPPGITT)),
        )

        every { mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(tilAktør(søkerFnr)) } returns PersonInfo(
            fødselsdato = LocalDate.of(1990, 1, 1),
            bostedsadresser = mutableListOf(
                Bostedsadresse(
                    vegadresse = Vegadresse(
                        matrikkelId,
                        søkerHusnummer,
                        søkerHusbokstav,
                        søkerBruksenhetsnummer,
                        søkerAdressnavn,
                        søkerKommunenummer,
                        søkerTilleggsnavn,
                        søkerPostnummer,
                    ),
                ),
            ),
        )
        every { mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(tilAktør(barn1Fnr)) } returns PersonInfo(
            fødselsdato = LocalDate.of(2009, 1, 1),
            bostedsadresser = mutableListOf(
                Bostedsadresse(
                    matrikkeladresse = Matrikkeladresse(
                        matrikkelId,
                        barn1Bruksenhetsnummer,
                        barn1Tilleggsnavn,
                        barn1Postnummer,
                        barn1Kommunenummer,
                    ),
                ),
            ),
        )
        every { mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(tilAktør(barn2Fnr)) } returns PersonInfo(
            fødselsdato = LocalDate.of(1990, 1, 1),
            bostedsadresser = mutableListOf(Bostedsadresse(ukjentBosted = UkjentBosted(barn2BostedKommune))),
        )

        val fagsak = fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerFnr))
        val behandling = behandlingService.opprettBehandling(
            nyOrdinærBehandling(
                søkersIdent = søkerFnr,
                fagsakId = fagsak.data!!.id,
            ),
        )

        val søkerAktør = personidentService.hentOgLagreAktør(søkerFnr, true)
        val barn1Aktør = personidentService.hentOgLagreAktør(barn1Fnr, true)
        val barn2Aktør = personidentService.hentOgLagreAktør(barn2Fnr, true)

        persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
            søkerAktør,
            listOf(barn1Aktør, barn2Aktør),
            behandling,
            Målform.NB,
        )

        val søker = personRepository.findByAktør(søkerAktør).first()
        val vegadresse = søker.bostedsadresser.sisteAdresse() as GrVegadresse
        assertEquals(søkerAdressnavn, vegadresse.adressenavn)
        assertEquals(matrikkelId, vegadresse.matrikkelId)
        assertEquals(søkerBruksenhetsnummer, vegadresse.bruksenhetsnummer)
        assertEquals(søkerHusbokstav, vegadresse.husbokstav)
        assertEquals(søkerHusnummer, vegadresse.husnummer)
        assertEquals(søkerKommunenummer, vegadresse.kommunenummer)
        assertEquals(søkerPostnummer, vegadresse.postnummer)
        assertEquals(søkerTilleggsnavn, vegadresse.tilleggsnavn)

        assertEquals(3, søker.personopplysningGrunnlag.personer.size)

        søker.personopplysningGrunnlag.barna.forEach {
            when (it.aktør.aktivFødselsnummer()) {
                barn1Fnr -> {
                    val matrikkeladresse = it.bostedsadresser.sisteAdresse() as GrMatrikkeladresse
                    assertEquals(barn1Bruksenhetsnummer, matrikkeladresse.bruksenhetsnummer)
                    assertEquals(barn1Kommunenummer, matrikkeladresse.kommunenummer)
                    assertEquals(barn1Postnummer, matrikkeladresse.postnummer)
                    assertEquals(barn1Tilleggsnavn, matrikkeladresse.tilleggsnavn)
                }

                barn2Fnr -> {
                    val ukjentBosted = it.bostedsadresser.sisteAdresse() as GrUkjentBosted
                    assertEquals(barn2BostedKommune, ukjentBosted.bostedskommune)
                }

                else -> {
                    throw RuntimeException("Ujent barn fnr")
                }
            }
        }
    }

    @Test
    fun `Skal lagre og sende korrekt sakstatistikk for behandlingsresultat`() {
        val fnr = "12345678910"
        val fagsak = fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr))
        val behandling =
            behandlingService.opprettBehandling(nyOrdinærBehandling(søkersIdent = fnr, fagsakId = fagsak.data!!.id))
        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling = behandling)
        val vedtak = vedtakService.hentAktivForBehandling(behandling.id)

        vedtakService.oppdater(vedtak!!)

        behandlingHentOgPersisterService.lagreEllerOppdater(
            behandling.also {
                it.resultat = Behandlingsresultat.AVSLÅTT
            },
        )

        val behandlingDvhMeldinger = saksstatistikkMellomlagringRepository.finnMeldingerKlarForSending()
            .filter { it.type == SaksstatistikkMellomlagringType.BEHANDLING }
            .map { it.jsonToBehandlingDVH() }

        assertEquals(2, behandlingDvhMeldinger.size)
        assertThat(behandlingDvhMeldinger.last().resultat).isEqualTo("AVSLÅTT")
    }

    private fun markerBehandlingSomAvsluttet(behandling: Behandling): Behandling {
        behandling.status = BehandlingStatus.AVSLUTTET
        behandling.leggTilBehandlingStegTilstand(StegType.BEHANDLING_AVSLUTTET)
        return behandlingHentOgPersisterService.lagreOgFlush(behandling)
    }
}
