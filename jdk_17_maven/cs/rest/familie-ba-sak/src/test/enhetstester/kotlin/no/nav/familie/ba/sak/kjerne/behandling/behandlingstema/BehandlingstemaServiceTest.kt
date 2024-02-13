package no.nav.familie.ba.sak.kjerne.behandling.behandlingstema

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPersonResultaterForSøkerOgToBarn
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilPersonEnkelSøkerOgBarn
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjeService
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjer
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BehandlingstemaServiceTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val loggService = mockk<LoggService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val tidslinjeService = mockk<VilkårsvurderingTidslinjeService>()
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()

    val behandlingstemaService = BehandlingstemaService(
        behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
        loggService = loggService,
        oppgaveService = oppgaveService,
        vilkårsvurderingTidslinjeService = tidslinjeService,
        vilkårsvurderingRepository = vilkårsvurderingRepository,
    )
    val defaultFagsak = defaultFagsak()
    val defaultBehandling = lagBehandling(defaultFagsak)

    @BeforeAll
    fun init() {
        every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(defaultFagsak.id) } returns defaultBehandling
    }

    @Test
    fun `Skal utlede EØS dersom minst ett vilkår i har blitt behandlet i inneværende behandling`() {
        val barn = randomAktør()
        val vilkårsvurdering = Vilkårsvurdering(
            behandling = defaultBehandling,
        )
        val personResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = barn,
            vilkårResultater = mutableSetOf(
                lagVilkårResultat(
                    vilkår = Vilkår.BOSATT_I_RIKET,
                    vilkårRegelverk = Regelverk.NASJONALE_REGLER,
                    behandlingId = defaultBehandling.id,
                ),
                lagVilkårResultat(
                    vilkår = Vilkår.BOSATT_I_RIKET,
                    vilkårRegelverk = Regelverk.EØS_FORORDNINGEN,
                    behandlingId = defaultBehandling.id,
                ),
            ),
        )
        vilkårsvurdering.personResultater = setOf(personResultat)
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(defaultBehandling.id) } returns vilkårsvurdering

        val kategori = behandlingstemaService.hentKategoriFraInneværendeBehandling(defaultFagsak.id)

        assertEquals(BehandlingKategori.EØS, kategori)
    }

    @Test
    fun `Skal utlede NASJONAL dersom EØS vilkåret ble behandlet i annen behandling`() {
        val barn = randomAktør()
        val vilkårsvurdering = Vilkårsvurdering(
            behandling = defaultBehandling,
        )
        val personResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = barn,
            vilkårResultater = mutableSetOf(
                lagVilkårResultat(
                    vilkår = Vilkår.BOSATT_I_RIKET,
                    vilkårRegelverk = Regelverk.NASJONALE_REGLER,
                    behandlingId = defaultBehandling.id,
                ),
                lagVilkårResultat(
                    vilkår = Vilkår.BOSATT_I_RIKET,
                    vilkårRegelverk = Regelverk.EØS_FORORDNINGEN,
                    behandlingId = 0L,
                ),
            ),
        )
        vilkårsvurdering.personResultater = setOf(personResultat)
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(defaultBehandling.id) } returns vilkårsvurdering

        val kategori = behandlingstemaService.hentKategoriFraInneværendeBehandling(defaultFagsak.id)

        assertEquals(BehandlingKategori.NASJONAL, kategori)
    }

    @Test
    fun `Skal utlede UTVIDET dersom minst ett vilkår i har blitt behandlet i inneværende behandling`() {
        val vilkårsvurdering = Vilkårsvurdering(
            behandling = defaultBehandling,
        )
        val personResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = defaultFagsak.aktør,
            vilkårResultater = mutableSetOf(
                lagVilkårResultat(
                    vilkår = Vilkår.UTVIDET_BARNETRYGD,
                    behandlingId = defaultBehandling.id,
                ),
                lagVilkårResultat(
                    vilkår = Vilkår.UTVIDET_BARNETRYGD,
                    behandlingId = defaultBehandling.id,
                ),
            ),
        )
        vilkårsvurdering.personResultater = setOf(personResultat)
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(defaultBehandling.id) } returns vilkårsvurdering

        val underkategori = behandlingstemaService.hentUnderkategoriFraInneværendeBehandling(defaultFagsak.id)

        assertEquals(BehandlingUnderkategori.UTVIDET, underkategori)
    }

    @Test
    fun `Skal utlede ORDINÆR dersom UTVIDET vilkåret ble behandlet i annen behandling`() {
        val barn = randomAktør()
        val vilkårsvurdering = Vilkårsvurdering(
            behandling = defaultBehandling,
        )
        val personResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = barn,
            vilkårResultater = mutableSetOf(
                lagVilkårResultat(
                    vilkår = Vilkår.UTVIDET_BARNETRYGD,
                    vilkårRegelverk = Regelverk.NASJONALE_REGLER,
                    behandlingId = 0L,
                ),
            ),
        )
        vilkårsvurdering.personResultater = setOf(personResultat)
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(defaultBehandling.id) } returns vilkårsvurdering

        val underkategori = behandlingstemaService.hentUnderkategoriFraInneværendeBehandling(defaultFagsak.id)

        assertEquals(BehandlingUnderkategori.ORDINÆR, underkategori)
    }

    @Test
    fun `skal hente løpende kategori til NASJONAL når siste behandling er NASJONAL og har løpende utbetaling`() {
        val søkerFnr = randomFnr()
        val barnFnr = listOf(randomFnr())
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(defaultFagsak.id) } returns defaultBehandling
        every { tidslinjeService.hentTidslinjer(BehandlingId(defaultBehandling.id)) } returns
            VilkårsvurderingTidslinjer(
                vilkårsvurdering = lagVilkårsvurdering(tilAktør(søkerFnr), defaultBehandling, Resultat.OPPFYLT),
                søkerOgBarn = lagTestPersonopplysningGrunnlag(defaultBehandling.id, randomFnr(), barnFnr)
                    .tilPersonEnkelSøkerOgBarn(),
            )
        assertEquals(BehandlingKategori.NASJONAL, behandlingstemaService.hentLøpendeKategori(defaultFagsak.id))
    }

    @Test
    fun `skal hente løpende kategori til EØS når siste behandling er EØS og har løpende utbetaling`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        val barn2Fnr = randomFnr()
        val barnaFnr = listOf(barnFnr, barn2Fnr)
        val behandlingMedEøsRegelverk = defaultBehandling.copy(kategori = BehandlingKategori.EØS)
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(defaultFagsak.id) } returns
            behandlingMedEøsRegelverk
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandlingMedEøsRegelverk)
        vilkårsvurdering.personResultater = lagPersonResultaterForSøkerOgToBarn(
            vilkårsvurdering,
            tilAktør(søkerFnr),
            tilAktør(barnFnr),
            tilAktør(barn2Fnr),
            LocalDate.now().minusMonths(1),
            LocalDate.now().plusYears(2),
        )
        every { tidslinjeService.hentTidslinjer(BehandlingId(defaultBehandling.id)) } returns
            VilkårsvurderingTidslinjer(
                vilkårsvurdering = vilkårsvurdering,
                søkerOgBarn = lagTestPersonopplysningGrunnlag(defaultBehandling.id, søkerFnr, barnaFnr)
                    .tilPersonEnkelSøkerOgBarn(),
            )
        assertEquals(BehandlingKategori.EØS, behandlingstemaService.hentLøpendeKategori(defaultFagsak.id))
    }

    @Test
    fun `skal hente løpende kategori til NASJONAL når siste behandling er EØS opphørt`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        val barn2Fnr = randomFnr()
        val barnaFnr = listOf(barnFnr, barn2Fnr)
        val behandlingMedEøsRegelverk = defaultBehandling.copy(kategori = BehandlingKategori.EØS)
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(defaultFagsak.id) } returns
            behandlingMedEøsRegelverk
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandlingMedEøsRegelverk)
        vilkårsvurdering.personResultater = lagPersonResultaterForSøkerOgToBarn(
            vilkårsvurdering,
            tilAktør(søkerFnr),
            tilAktør(barnFnr),
            tilAktør(barn2Fnr),
            LocalDate.now().minusMonths(3),
            LocalDate.now().minusMonths(2),
        )
        every { tidslinjeService.hentTidslinjer(BehandlingId(defaultBehandling.id)) } returns
            VilkårsvurderingTidslinjer(
                vilkårsvurdering = vilkårsvurdering,
                søkerOgBarn = lagTestPersonopplysningGrunnlag(defaultBehandling.id, søkerFnr, barnaFnr)
                    .tilPersonEnkelSøkerOgBarn(),
            )
        assertEquals(BehandlingKategori.NASJONAL, behandlingstemaService.hentLøpendeKategori(defaultFagsak.id))
    }
}
