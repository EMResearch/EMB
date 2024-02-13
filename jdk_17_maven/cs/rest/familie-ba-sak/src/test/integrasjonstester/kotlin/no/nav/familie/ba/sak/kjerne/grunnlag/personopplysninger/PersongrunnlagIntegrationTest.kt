package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import io.mockk.every
import io.mockk.verify
import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.DødsfallData
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlKontaktinformasjonForDødsbo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlKontaktinformasjonForDødsboAdresse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRequest
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.defaultBostedsadresseHistorikk
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class PersongrunnlagIntegrationTest(
    @Autowired
    private val mockIntegrasjonClient: IntegrasjonClient,

    @Autowired
    private val persongrunnlagService: PersongrunnlagService,

    @Autowired
    private val personidentService: PersonidentService,

    @Autowired
    private val mockPersonopplysningerService: PersonopplysningerService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val behandlingService: BehandlingService,
) : AbstractSpringIntegrationTest() {

    @Test
    fun `Skal lagre dødsfall på person når person er død`() {
        val søkerAktør = personidentService.hentOgLagreAktør(randomFnr(), true)
        val barn1Aktør = personidentService.hentOgLagreAktør(randomFnr(), true)

        val dødsdato = "2020-04-04"
        val adresselinje1 = "Gatenavn 1"
        val poststedsnavn = "Oslo"
        val postnummer = "1234"

        every { mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(søkerAktør) } returns PersonInfo(
            fødselsdato = LocalDate.of(1990, 1, 1),
            adressebeskyttelseGradering = null,
            navn = "Mor",
            kjønn = Kjønn.KVINNE,
            forelderBarnRelasjon = emptySet(),
            bostedsadresser = mutableListOf(Bostedsadresse()) + defaultBostedsadresseHistorikk,
            sivilstander = listOf(Sivilstand(type = SIVILSTAND.UOPPGITT)),
            dødsfall = DødsfallData(erDød = true, dødsdato = dødsdato),
            kontaktinformasjonForDoedsbo = PdlKontaktinformasjonForDødsbo(
                adresse = PdlKontaktinformasjonForDødsboAdresse(
                    adresselinje1 = adresselinje1,
                    poststedsnavn = poststedsnavn,
                    postnummer = postnummer,
                ),
            ),
        )

        every { mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(barn1Aktør) } returns PersonInfo(
            fødselsdato = LocalDate.of(2009, 1, 1),
            adressebeskyttelseGradering = null,
            navn = "Gutt",
            kjønn = Kjønn.MANN,
            forelderBarnRelasjon = emptySet(),
            bostedsadresser = mutableListOf(Bostedsadresse()) + defaultBostedsadresseHistorikk,
            sivilstander = listOf(Sivilstand(type = SIVILSTAND.UOPPGITT)),
            dødsfall = null,
            kontaktinformasjonForDoedsbo = null,
        )

        val fagsak = fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerAktør.aktivFødselsnummer()))
        val behandling = behandlingService.opprettBehandling(
            nyOrdinærBehandling(
                søkersIdent = søkerAktør.aktivFødselsnummer(),
                fagsakId = fagsak.data!!.id,
            ),
        )

        val personopplysningGrunnlag = persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
            aktør = søkerAktør,
            barnFraInneværendeBehandling = listOf(barn1Aktør),
            behandling = behandling,
            målform = Målform.NB,
        )

        Assertions.assertTrue(personopplysningGrunnlag.søker.erDød())
        assertEquals(LocalDate.parse(dødsdato), personopplysningGrunnlag.søker.dødsfall?.dødsfallDato)
        assertEquals(adresselinje1, personopplysningGrunnlag.søker.dødsfall?.dødsfallAdresse)
        assertEquals(postnummer, personopplysningGrunnlag.søker.dødsfall?.dødsfallPostnummer)
        assertEquals(poststedsnavn, personopplysningGrunnlag.søker.dødsfall?.dødsfallPoststed)

        Assertions.assertFalse(personopplysningGrunnlag.barna.single().erDød())
        assertEquals(null, personopplysningGrunnlag.barna.single().dødsfall)
    }

    @Test
    fun `Skal hente arbeidsforhold for mor når hun er EØS-borger og det er en automatisk behandling`() {
        val morAktør = personidentService.hentOgLagreAktør(randomFnr(), true)
        val barn1Aktør = personidentService.hentOgLagreAktør(randomFnr(), true)

        every { mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(morAktør) } returns PersonInfo(
            fødselsdato = LocalDate.of(1990, 1, 1),
            adressebeskyttelseGradering = null,
            navn = "Mor",
            kjønn = Kjønn.KVINNE,
            forelderBarnRelasjon = emptySet(),
            bostedsadresser = mutableListOf(Bostedsadresse()) + defaultBostedsadresseHistorikk,
            sivilstander = listOf(Sivilstand(type = SIVILSTAND.UOPPGITT)),
            statsborgerskap = listOf(
                Statsborgerskap(
                    land = "POL",
                    gyldigFraOgMed = null,
                    gyldigTilOgMed = null,
                    bekreftelsesdato = null,
                ),
            ),
            dødsfall = null,
            kontaktinformasjonForDoedsbo = null,
        )

        every { mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(barn1Aktør) } returns PersonInfo(
            fødselsdato = LocalDate.of(2009, 1, 1),
            adressebeskyttelseGradering = null,
            navn = "Gutt",
            kjønn = Kjønn.MANN,
            forelderBarnRelasjon = emptySet(),
            bostedsadresser = mutableListOf(Bostedsadresse()) + defaultBostedsadresseHistorikk,
            sivilstander = listOf(Sivilstand(type = SIVILSTAND.UOPPGITT)),
            dødsfall = null,
            kontaktinformasjonForDoedsbo = null,
        )
        val fagsak = fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = morAktør.aktivFødselsnummer()))
        val behandling = behandlingService.opprettBehandling(
            NyBehandling(
                skalBehandlesAutomatisk = true,
                søkersIdent = morAktør.aktivFødselsnummer(),
                behandlingÅrsak = BehandlingÅrsak.FØDSELSHENDELSE,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                kategori = BehandlingKategori.NASJONAL, // alltid NASJONAL for fødselshendelse
                underkategori = BehandlingUnderkategori.ORDINÆR,
                fagsakId = fagsak.data!!.id,
            ),
        )

        persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
            aktør = morAktør,
            barnFraInneværendeBehandling = listOf(barn1Aktør),
            behandling = behandling,
            målform = Målform.NB,
        )

        verify(exactly = 1) { mockIntegrasjonClient.hentArbeidsforhold(any(), any()) }
    }

    @Test
    fun `Skal ikke hente arbeidsforhold for mor når det er en automatisk behandling, men hun er norsk statsborger`() {
        val morAktør = personidentService.hentOgLagreAktør(randomFnr(), true)
        val barn1Aktør = personidentService.hentOgLagreAktør(randomFnr(), true)

        every { mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(morAktør) } returns PersonInfo(
            fødselsdato = LocalDate.of(1990, 1, 1),
            adressebeskyttelseGradering = null,
            navn = "Mor",
            kjønn = Kjønn.KVINNE,
            forelderBarnRelasjon = emptySet(),
            bostedsadresser = mutableListOf(Bostedsadresse()) + defaultBostedsadresseHistorikk,
            sivilstander = listOf(Sivilstand(type = SIVILSTAND.UOPPGITT)),
            statsborgerskap = listOf(
                Statsborgerskap(
                    land = "NOR",
                    gyldigFraOgMed = null,
                    gyldigTilOgMed = null,
                    bekreftelsesdato = null,
                ),
            ),
            dødsfall = null,
            kontaktinformasjonForDoedsbo = null,
        )

        every { mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(barn1Aktør) } returns PersonInfo(
            fødselsdato = LocalDate.of(2009, 1, 1),
            adressebeskyttelseGradering = null,
            navn = "Gutt",
            kjønn = Kjønn.MANN,
            forelderBarnRelasjon = emptySet(),
            bostedsadresser = mutableListOf(Bostedsadresse()) + defaultBostedsadresseHistorikk,
            sivilstander = listOf(Sivilstand(type = SIVILSTAND.UOPPGITT)),
            dødsfall = null,
            kontaktinformasjonForDoedsbo = null,
        )
        val fagsak = fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = morAktør.aktivFødselsnummer()))
        val behandling = behandlingService.opprettBehandling(
            NyBehandling(
                skalBehandlesAutomatisk = true,
                søkersIdent = morAktør.aktivFødselsnummer(),
                behandlingÅrsak = BehandlingÅrsak.FØDSELSHENDELSE,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                fagsakId = fagsak.data!!.id,
            ),
        )

        persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
            aktør = morAktør,
            barnFraInneværendeBehandling = listOf(barn1Aktør),
            behandling = behandling,
            målform = Målform.NB,
        )

        verify(exactly = 0) { mockIntegrasjonClient.hentArbeidsforhold(any(), any()) }
    }

    @Test
    fun `Skal filtrere ut bostedsadresse uten verdier når de mappes inn`() {
        val søkerAktør = personidentService.hentOgLagreAktør(randomFnr(), true)
        val barn1Aktør = personidentService.hentOgLagreAktør(randomFnr(), true)

        every { mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(søkerAktør) } returns PersonInfo(
            fødselsdato = LocalDate.of(1990, 1, 1),
            adressebeskyttelseGradering = null,
            navn = "Mor",
            kjønn = Kjønn.KVINNE,
            forelderBarnRelasjon = emptySet(),
            bostedsadresser = mutableListOf(Bostedsadresse()) + defaultBostedsadresseHistorikk,
            sivilstander = listOf(Sivilstand(type = SIVILSTAND.UOPPGITT)),
        )

        every { mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(barn1Aktør) } returns PersonInfo(
            fødselsdato = LocalDate.of(2009, 1, 1),
            adressebeskyttelseGradering = null,
            navn = "Gutt",
            kjønn = Kjønn.MANN,
            forelderBarnRelasjon = emptySet(),
            bostedsadresser = mutableListOf(Bostedsadresse()) + defaultBostedsadresseHistorikk,
            sivilstander = listOf(Sivilstand(type = SIVILSTAND.UOPPGITT)),
        )

        val fagsak = fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerAktør.aktivFødselsnummer()))
        val behandling = behandlingService.opprettBehandling(
            nyOrdinærBehandling(
                søkersIdent = søkerAktør.aktivFødselsnummer(),
                fagsakId = fagsak.data!!.id,
            ),
        )

        val personopplysningGrunnlag = persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
            søkerAktør,
            listOf(barn1Aktør),
            behandling,
            Målform.NB,
        )

        personopplysningGrunnlag.personer.forEach {
            assertEquals(defaultBostedsadresseHistorikk.size, it.bostedsadresser.size)
        }
    }
}
