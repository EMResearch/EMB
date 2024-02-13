package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse

import io.mockk.every
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class VelgFagsystemIntegrasjonTest(
    @Autowired val stegService: StegService,
    @Autowired val mockPersonopplysningerService: PersonopplysningerService,
    @Autowired val persongrunnlagService: PersongrunnlagService,
    @Autowired val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    @Autowired val velgFagSystemService: VelgFagSystemService,
    @Autowired val fagSakService: FagsakService,
    @Autowired val infotrygdService: InfotrygdService,
    @Autowired val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
    @Autowired val databaseCleanupService: DatabaseCleanupService,
    @Autowired val featureToggleService: FeatureToggleService,
) : AbstractSpringIntegrationTest() {

    val søkerFnr = randomFnr()

    @BeforeEach
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `sjekk om mor har løpende utbetalinger i infotrygd`() {
        every { infotrygdService.harLøpendeSakIInfotrygd(any(), any()) } returns true andThen false

        assertEquals(true, velgFagSystemService.morEllerBarnHarLøpendeSakIInfotrygd(søkerFnr, emptyList()))
    }

    @Test
    fun `skal IKKE velge ba-sak når mor har stønadhistorikk i Infotrygd`() {
        val nyBehandling = NyBehandlingHendelse(søkerFnr, listOf(søkerFnr))
        val fagsystemUtfall = FagsystemUtfall.SAKER_I_INFOTRYGD_MEN_IKKE_LØPENDE_UTBETALINGER

        every { infotrygdBarnetrygdClient.hentStønader(any(), any(), any()) } returns InfotrygdSøkResponse(
            listOf(Stønad(opphørtFom = "012020")),
            emptyList(),
        ) andThen InfotrygdSøkResponse(emptyList(), emptyList())

        val (fagsystemRegelVurdering, faktiskFagsystemUtfall) = velgFagSystemService.velgFagsystem(nyBehandling)
        assertEquals(FagsystemRegelVurdering.SEND_TIL_INFOTRYGD, fagsystemRegelVurdering)
        assertEquals(fagsystemUtfall, faktiskFagsystemUtfall)
    }

    @Test
    fun `skal IKKE velge ba-sak når mor er EØS borger`() {
        val nyBehandling = NyBehandlingHendelse(søkerFnr, listOf(søkerFnr))

        every { mockPersonopplysningerService.hentGjeldendeStatsborgerskap(any()) } returns Statsborgerskap(
            land = "POL",
            gyldigFraOgMed = LocalDate.now().minusYears(2),
            gyldigTilOgMed = null,
            bekreftelsesdato = null,
        )
        val (fagsystemRegelVurdering, faktiskFagsystemUtfall) = velgFagSystemService.velgFagsystem(nyBehandling)
        assertEquals(FagsystemRegelVurdering.SEND_TIL_BA, fagsystemRegelVurdering)
        assertEquals(FagsystemUtfall.STØTTET_I_BA_SAK, faktiskFagsystemUtfall)
    }

    @Test
    fun `skal velge ba-sak når mor er tredjelandsborger`() {
        val nyBehandling = NyBehandlingHendelse(søkerFnr, listOf(søkerFnr))
        val fagsystemUtfall = FagsystemUtfall.STØTTET_I_BA_SAK

        every { mockPersonopplysningerService.hentGjeldendeStatsborgerskap(any()) } returns Statsborgerskap(
            land = "USA",
            gyldigFraOgMed = LocalDate.now().minusYears(2),
            gyldigTilOgMed = null,
            bekreftelsesdato = null,
        )
        val (fagsystemRegelVurdering, faktiskFagsystemUtfall) = velgFagSystemService.velgFagsystem(nyBehandling)
        assertEquals(FagsystemRegelVurdering.SEND_TIL_BA, fagsystemRegelVurdering)
        assertEquals(fagsystemUtfall, faktiskFagsystemUtfall)
    }
}
