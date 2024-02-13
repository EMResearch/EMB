package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.SettPåMaskinellVentÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FerdigstillBehandlingTaskTest : AbstractSpringIntegrationTest() {

    @Autowired
    private lateinit var vedtakService: VedtakService

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var persongrunnlagService: PersongrunnlagService

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var stegService: StegService

    @Autowired
    lateinit var vilkårsvurderingService: VilkårsvurderingService

    @Autowired
    lateinit var databaseCleanupService: DatabaseCleanupService

    @Autowired
    lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    lateinit var saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository

    @Autowired
    lateinit var vedtaksperiodeService: VedtaksperiodeService

    @Autowired
    lateinit var personidentService: PersonidentService

    @Autowired
    lateinit var brevmalService: BrevmalService

    @Autowired
    lateinit var snikeIKøenService: SnikeIKøenService

    private val fnr = randomFnr()

    @BeforeEach
    fun init() {
        databaseCleanupService.truncate()
    }

    private fun kjørSteg(resultat: Resultat): Behandling {
        val aktørId = personidentService.hentAktør(fnr)
        val fnrBarn = ClientMocks.barnFnr[0]

        val behandling = kjørStegprosessForFGB(
            tilSteg = if (resultat == Resultat.OPPFYLT) StegType.DISTRIBUER_VEDTAKSBREV else StegType.REGISTRERE_SØKNAD,
            søkerFnr = fnr,
            barnasIdenter = listOf(fnrBarn),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
        )

        return if (resultat == Resultat.IKKE_OPPFYLT) {
            val vilkårsvurdering = lagVilkårsvurdering(aktørId, behandling, resultat)

            vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
            val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(behandling)

            behandlingService.oppdaterStatusPåBehandling(
                behandlingEtterVilkårsvurdering.id,
                BehandlingStatus.IVERKSETTER_VEDTAK,
            )
            behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
                behandlingId = behandlingEtterVilkårsvurdering.id,
                steg = StegType.FERDIGSTILLE_BEHANDLING,
            )
        } else {
            behandling
        }
    }

    @Test
    fun `Skal ferdigstille behandling og fagsak blir til løpende`() {
        val behandling = kjørSteg(Resultat.OPPFYLT)

        val ferdigstiltBehandling = stegService.håndterFerdigstillBehandling(behandling)

        assertEquals(BehandlingStatus.AVSLUTTET, ferdigstiltBehandling.status)
        assertEquals(
            FagsakStatus.AVSLUTTET.name,
            saksstatistikkMellomlagringRepository.findByTypeAndTypeId(
                SaksstatistikkMellomlagringType.BEHANDLING,
                ferdigstiltBehandling.id,
            )
                .last().jsonToBehandlingDVH().behandlingStatus,
        )

        val ferdigstiltFagsak = ferdigstiltBehandling.fagsak
        assertEquals(FagsakStatus.LØPENDE, ferdigstiltFagsak.status)

        assertEquals(
            FagsakStatus.LØPENDE.name,
            saksstatistikkMellomlagringRepository.findByTypeAndTypeId(
                SaksstatistikkMellomlagringType.SAK,
                ferdigstiltFagsak.id,
            )
                .last().jsonToSakDVH().sakStatus,
        )
    }

    @Test
    fun `Skal ferdigstille behandling og sette fagsak til stanset`() {
        val behandling = kjørSteg(Resultat.IKKE_OPPFYLT)

        val ferdigstiltBehandling = stegService.håndterFerdigstillBehandling(behandling)
        assertEquals(BehandlingStatus.AVSLUTTET, ferdigstiltBehandling.status)

        val ferdigstiltFagsak = ferdigstiltBehandling.fagsak
        assertEquals(FagsakStatus.AVSLUTTET, ferdigstiltFagsak.status)
        assertEquals(
            FagsakStatus.AVSLUTTET.name,
            saksstatistikkMellomlagringRepository.findByTypeAndTypeId(
                SaksstatistikkMellomlagringType.SAK,
                ferdigstiltFagsak.id,
            )
                .last().jsonToSakDVH().sakStatus,
        )
    }

    @Nested
    inner class BehandlingPåMaskinellVent {

        @Test
        fun `skal reaktivere en behandling som er på maskinell vent`() {
            val behandling1 = opprettBehandling(status = BehandlingStatus.UTREDES)
            settPåMaskinellVent(behandling1)

            val behandling2 = kjørSteg(Resultat.OPPFYLT)
            stegService.håndterFerdigstillBehandling(behandling2)

            assertThat(behandlingRepository.finnBehandling(behandling2.id).aktiv).isFalse()
            assertThat(behandlingRepository.finnBehandling(behandling1.id).aktiv).isTrue()
        }

        @Test
        fun `skal reaktivere en behandling etter ferdigstilling av henlagt behandling`() {
            val behandling1 = opprettBehandling(status = BehandlingStatus.UTREDES)
            settPåMaskinellVent(behandling1)

            val behandling2 = kjørSteg(Resultat.OPPFYLT)
            behandling2.resultat = Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET
            stegService.håndterFerdigstillBehandling(behandling2)

            assertThat(behandlingRepository.finnBehandling(behandling2.id).aktiv).isFalse()
            assertThat(behandlingRepository.finnBehandling(behandling1.id).aktiv).isTrue()
        }

        @Test
        fun `skal reaktivere en behandling etter ferdigstilling av henlagt behandling som har en tidligere iverksatt behandling`() {
            val behandling = kjørSteg(Resultat.OPPFYLT)
            stegService.håndterFerdigstillBehandling(behandling)
            assertThat(behandlingRepository.finnBehandling(behandling.id).aktiv).isTrue()

            val behandling2 = opprettBehandling(status = BehandlingStatus.UTREDES)
            settPåMaskinellVent(behandling2)

            val behandling3 = opprettBehandling(
                status = BehandlingStatus.FATTER_VEDTAK,
                resultat = Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET,
            )
            stegService.håndterFerdigstillBehandling(behandling3)

            assertThat(behandlingRepository.finnBehandling(behandling2.id).aktiv).isTrue()
        }

        private fun opprettBehandling(
            status: BehandlingStatus = BehandlingStatus.IVERKSETTER_VEDTAK,
            resultat: Behandlingsresultat = Behandlingsresultat.INNVILGET,
        ): Behandling {
            val behandling = Behandling(
                fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr),
                opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                type = BehandlingType.REVURDERING,
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                status = status,
                resultat = resultat,
            ).initBehandlingStegTilstand()
            val ferdigstillSteg = BehandlingStegTilstand(
                behandling = behandling,
                behandlingSteg = StegType.FERDIGSTILLE_BEHANDLING,
            )
            behandling.behandlingStegTilstand.add(ferdigstillSteg)
            return behandlingService.lagreNyOgDeaktiverGammelBehandling(behandling)
        }

        private fun settPåMaskinellVent(behandling: Behandling) {
            snikeIKøenService.settAktivBehandlingTilPåMaskinellVent(
                behandling.id,
                SettPåMaskinellVentÅrsak.SATSENDRING,
            )
        }
    }
}
