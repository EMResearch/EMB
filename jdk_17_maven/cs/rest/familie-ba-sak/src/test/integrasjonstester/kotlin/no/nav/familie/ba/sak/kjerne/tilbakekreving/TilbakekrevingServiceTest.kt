package no.nav.familie.ba.sak.kjerne.tilbakekreving

import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.opprettRestTilbakekreving
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.ekstern.restDomene.InstitusjonInfo
import no.nav.familie.ba.sak.ekstern.restDomene.VergeInfo
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.brev.mottaker.Brevmottaker
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerRepository
import no.nav.familie.ba.sak.kjerne.brev.mottaker.MottakerType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.springframework.beans.factory.annotation.Autowired
import java.util.Properties
import java.util.stream.Stream
import no.nav.familie.kontrakter.felles.tilbakekreving.Brevmottaker as TilbakekrevingBrevmottaker

class TilbakekrevingServiceTest(
    @Autowired private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val persongrunnlagService: PersongrunnlagService,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val stegService: StegService,
    @Autowired private val tilbakekrevingService: TilbakekrevingService,
    @Autowired private val tilbakekrevingRepository: TilbakekrevingRepository,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired private val databaseCleanupService: DatabaseCleanupService,
    @Autowired private val brevmalService: BrevmalService,
    @Autowired private val brevmottakerRepository: BrevmottakerRepository,
) : AbstractSpringIntegrationTest() {

    @BeforeEach
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    @Tag("integration")
    fun `tilbakekreving skal bli OPPRETT_TILBAKEKREVING_MED_VARSEL når man oppretter tilbakekreving med varsel`() {
        val behandling = kjørStegprosessForFGB(
            tilSteg = StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
            søkerFnr = randomFnr(),
            barnasIdenter = listOf(ClientMocks.barnFnr[0]),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
        )

        val restTilbakekreving = opprettRestTilbakekreving()
        tilbakekrevingService.validerRestTilbakekreving(restTilbakekreving, behandling.id)
        tilbakekrevingService.lagreTilbakekreving(restTilbakekreving, behandling.id)

        stegService.håndterIverksettMotFamilieTilbake(behandling, Properties())

        val tilbakekreving = tilbakekrevingRepository.findByBehandlingId(behandling.id)

        assertEquals(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL, tilbakekreving?.valg)
        assertEquals("id1", tilbakekreving?.tilbakekrevingsbehandlingId)
        assertEquals("Varsel", tilbakekreving?.varsel)
    }

    @Test
    @Tag("integration")
    fun `tilbakekreving skal bli OPPRETT_TILBAKEKREVING_MED_VARSEL når man oppretter tilbakekreving med varsel for institusjon`() {
        val behandling = kjørStegprosessForFGB(
            tilSteg = StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
            søkerFnr = "09121079074",
            barnasIdenter = listOf("09121079074"),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            institusjon = InstitusjonInfo(orgNummer = "998765432", tssEksternId = "8000000"),
            brevmalService = brevmalService,
        )

        val restTilbakekreving = opprettRestTilbakekreving()
        tilbakekrevingService.validerRestTilbakekreving(restTilbakekreving, behandling.id)
        tilbakekrevingService.lagreTilbakekreving(restTilbakekreving, behandling.id)

        stegService.håndterIverksettMotFamilieTilbake(behandling, Properties())

        val tilbakekreving = tilbakekrevingRepository.findByBehandlingId(behandling.id)

        assertEquals(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL, tilbakekreving?.valg)
        assertEquals("id1", tilbakekreving?.tilbakekrevingsbehandlingId)
        assertEquals("Varsel", tilbakekreving?.varsel)
    }

    @Test
    @Tag("integration")
    fun `tilbakekreving skal bli OPPRETT_TILBAKEKREVING_MED_VARSEL når man oppretter tilbakekreving med varsel for mindreårig med verge`() {
        val behandling = kjørStegprosessForFGB(
            tilSteg = StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
            søkerFnr = "10031000033",
            barnasIdenter = listOf("10031000033"),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            verge = VergeInfo("04068203010"),
            brevmalService = brevmalService,
        )

        val restTilbakekreving = opprettRestTilbakekreving()
        tilbakekrevingService.validerRestTilbakekreving(restTilbakekreving, behandling.id)
        tilbakekrevingService.lagreTilbakekreving(restTilbakekreving, behandling.id)

        stegService.håndterIverksettMotFamilieTilbake(behandling, Properties())

        val tilbakekreving = tilbakekrevingRepository.findByBehandlingId(behandling.id)

        assertEquals(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL, tilbakekreving?.valg)
        assertEquals("id1", tilbakekreving?.tilbakekrevingsbehandlingId)
        assertEquals("Varsel", tilbakekreving?.varsel)
    }

    @Tag("integration")
    @ParameterizedTest
    @ArgumentsSource(TestProvider::class)
    @Suppress("SENSELESS_COMPARISON")
    fun `lagOpprettTilbakekrevingRequest sender brevmottakere i kall mot familie-tilbake`(arguments: Triple<VergeInfo, MottakerType, Vergetype>) {
        val behandling = kjørStegprosessForFGB(
            tilSteg = StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
            søkerFnr = if (arguments.first != null) ClientMocks.barnFnr[0] else randomFnr(),
            barnasIdenter = listOf(ClientMocks.barnFnr[0]),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,
            verge = arguments.first,
        )

        val brevmottaker = Brevmottaker(
            behandlingId = behandling.id,
            type = arguments.second,
            navn = "Donald Duck",
            adresselinje1 = "Andebyveien 1",
            postnummer = "0000",
            poststed = "OSLO",
            landkode = "NO",
        )
        brevmottakerRepository.saveAndFlush(brevmottaker)

        val opprettTilbakekrevingRequest = tilbakekrevingService.lagOpprettTilbakekrevingRequest(behandling)
        assertEquals(1, opprettTilbakekrevingRequest.manuelleBrevmottakere.size)
        val actualBrevmottaker = opprettTilbakekrevingRequest.manuelleBrevmottakere.first()

        assertBrevmottakerEquals(brevmottaker, actualBrevmottaker)
        assertEquals(arguments.third, actualBrevmottaker.vergetype)
    }

    private fun assertBrevmottakerEquals(expected: Brevmottaker, actual: TilbakekrevingBrevmottaker) {
        assertEquals(expected.navn, actual.navn)
        assertEquals(expected.type.name, actual.type.name)
        assertEquals(expected.adresselinje1, actual.manuellAdresseInfo?.adresselinje1)
        assertEquals(expected.postnummer, actual.manuellAdresseInfo?.postnummer)
        assertEquals(expected.poststed, actual.manuellAdresseInfo?.poststed)
        assertEquals(expected.landkode, actual.manuellAdresseInfo?.landkode)
    }

    private class TestProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return Stream.of(
                Arguments.of(Triple(null, MottakerType.FULLMEKTIG, Vergetype.ANNEN_FULLMEKTIG)),
                Arguments.of(Triple(null, MottakerType.VERGE, Vergetype.VERGE_FOR_VOKSEN)),
                Arguments.of(Triple(VergeInfo("12345678910"), MottakerType.VERGE, Vergetype.VERGE_FOR_BARN)),
                Arguments.of(Triple(null, MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE, null)),
            )
        }
    }
}
