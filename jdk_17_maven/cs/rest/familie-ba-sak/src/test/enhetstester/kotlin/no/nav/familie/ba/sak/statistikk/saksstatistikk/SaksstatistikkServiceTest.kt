package no.nav.familie.ba.sak.statistikk.saksstatistikk

import com.fasterxml.jackson.core.JsonFactory
import com.worldturner.medeia.api.UrlSchemaSource
import com.worldturner.medeia.api.jackson.MedeiaJacksonApi
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.unmockkAll
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.datagenerator.settpåvent.lagSettPåVent
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_NAVN
import no.nav.familie.eksterne.kontrakter.saksstatistikk.AktørDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.ResultatBegrunnelseDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SettPåVent
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.random.Random.Default.nextBoolean
import kotlin.random.Random.Default.nextInt

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SaksstatistikkServiceTest(
    @MockK(relaxed = true)
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,

    @MockK(relaxed = true)
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService,

    @MockK
    private val arbeidsfordelingService: ArbeidsfordelingService,

    @MockK
    private val totrinnskontrollService: TotrinnskontrollService,

    @MockK
    private val fagsakService: FagsakService,

    @MockK
    private val personopplysningerService: PersonopplysningerService,

    @MockK
    private val personidentService: PersonidentService,

    @MockK
    private val persongrunnlagService: PersongrunnlagService,

    @MockK
    private val vedtakService: VedtakService,

    @MockK
    private val vedtaksperiodeService: VedtaksperiodeService,

    @MockK
    private val settPåVentService: SettPåVentService,

) {

    private val sakstatistikkService = SaksstatistikkService(
        behandlingHentOgPersisterService,
        behandlingSøknadsinfoService,
        arbeidsfordelingService,
        totrinnskontrollService,
        vedtakService,
        fagsakService,
        personopplysningerService,
        persongrunnlagService,
        vedtaksperiodeService,
        settPåVentService,
    )

    @BeforeAll
    fun init() {
        MockKAnnotations.init()

        every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(any()) } returns ArbeidsfordelingPåBehandling(
            behandlendeEnhetId = "4820",
            behandlendeEnhetNavn = "Nav",
            behandlingId = 1,
        )
        every { arbeidsfordelingService.hentArbeidsfordelingsenhet(any()) } returns Arbeidsfordelingsenhet(
            "4821",
            "NAV",
        )

        every { settPåVentService.finnAktivSettPåVentPåBehandling(any()) } returns lagSettPåVent()
    }

    @AfterAll
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Skal mappe henleggelsesårsak til behandlingDVH for henlagt behandling`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE).also {
            it.resultat = Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET
        }

        every { behandlingHentOgPersisterService.hent(any()) } returns behandling
        every { totrinnskontrollService.hentAktivForBehandling(any()) } returns null
        every { vedtakService.hentAktivForBehandling(any()) } returns null

        val behandlingDvh = sakstatistikkService.mapTilBehandlingDVH(2)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDvh))

        assertThat(behandlingDvh?.resultat).isEqualTo("HENLAGT_FEILAKTIG_OPPRETTET")
        assertThat(behandlingDvh?.resultatBegrunnelser).hasSize(0)
    }

    @Test
    fun `Skal mappe til behandlingDVH for Automatisk rute`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE, skalBehandlesAutomatisk = true).also {
            it.resultat = Behandlingsresultat.INNVILGET
        }

        val vedtak = lagVedtak(behandling)
        val vedtaksperiodeMedBegrunnelser =
            lagVedtaksperiodeMedBegrunnelser()

        every { behandlingHentOgPersisterService.hent(any()) } returns behandling
        every { vedtakService.hentAktivForBehandling(any()) } returns vedtak
        every { vedtaksperiodeService.hentPersisterteVedtaksperioder(any()) } returns listOf(
            vedtaksperiodeMedBegrunnelser,
        )
        every { totrinnskontrollService.hentAktivForBehandling(any()) } returns Totrinnskontroll(
            saksbehandler = SYSTEM_NAVN,
            saksbehandlerId = SYSTEM_FORKORTELSE,
            beslutter = SYSTEM_NAVN,
            beslutterId = SYSTEM_FORKORTELSE,
            godkjent = true,
            behandling = behandling,
        )

        val behandlingDvh = sakstatistikkService.mapTilBehandlingDVH(2)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDvh))

        assertThat(behandlingDvh?.funksjonellTid).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(behandlingDvh?.tekniskTid).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(behandlingDvh?.mottattDato).isEqualTo(
            ZonedDateTime.of(
                behandling.opprettetTidspunkt,
                SaksstatistikkService.TIMEZONE,
            ),
        )
        assertThat(behandlingDvh?.registrertDato).isEqualTo(
            ZonedDateTime.of(
                behandling.opprettetTidspunkt,
                SaksstatistikkService.TIMEZONE,
            ),
        )
        assertThat(behandlingDvh?.vedtaksDato).isEqualTo(vedtak.vedtaksdato?.toLocalDate())
        assertThat(behandlingDvh?.behandlingId).isEqualTo(behandling.id.toString())
        assertThat(behandlingDvh?.relatertBehandlingId).isNull()
        assertThat(behandlingDvh?.sakId).isEqualTo(behandling.fagsak.id.toString())
        assertThat(behandlingDvh?.vedtakId).isEqualTo(vedtak.id.toString())
        assertThat(behandlingDvh?.behandlingType).isEqualTo(behandling.type.name)
        assertThat(behandlingDvh?.utenlandstilsnitt).isEqualTo(behandling.kategori.name)
        assertThat(behandlingDvh?.behandlingKategori).isEqualTo(behandling.underkategori.name)
        assertThat(behandlingDvh?.behandlingUnderkategori).isNull()
        assertThat(behandlingDvh?.behandlingStatus).isEqualTo(behandling.status.name)
        assertThat(behandlingDvh?.totrinnsbehandling).isFalse
        assertThat(behandlingDvh?.saksbehandler).isEqualTo(SYSTEM_FORKORTELSE)
        assertThat(behandlingDvh?.beslutter).isEqualTo(SYSTEM_FORKORTELSE)
        assertThat(behandlingDvh?.avsender).isEqualTo("familie-ba-sak")
        assertThat(behandlingDvh?.versjon).isNotEmpty
        assertThat(behandlingDvh?.resultat).isEqualTo(behandling.resultat.name)
    }

    @Test
    fun `Skal mappe til behandlingDVH for manuell rute`() {
        val behandling =
            lagBehandling(årsak = BehandlingÅrsak.SØKNAD).also { it.resultat = Behandlingsresultat.AVSLÅTT }

        every { totrinnskontrollService.hentAktivForBehandling(any()) } returns Totrinnskontroll(
            saksbehandler = "Saksbehandler",
            saksbehandlerId = "saksbehandlerId",
            beslutter = "Beslutter",
            beslutterId = "beslutterId",
            godkjent = true,
            behandling = behandling,
        )

        val vedtak = lagVedtak(behandling)

        val vedtaksperiodeFom = LocalDate.of(2021, 3, 11)
        val vedtaksperiodeTom = LocalDate.of(21, 4, 11)
        val vedtaksperiodeMedBegrunnelser =
            lagVedtaksperiodeMedBegrunnelser(vedtak = vedtak, fom = vedtaksperiodeFom, tom = vedtaksperiodeTom)

        every { behandlingHentOgPersisterService.hent(any()) } returns behandling
        every { persongrunnlagService.hentSøker(any()) } returns tilfeldigSøker()
        every { persongrunnlagService.hentBarna(any<Behandling>()) } returns listOf(
            tilfeldigPerson()
                .copy(aktør = randomAktør("01010000001")),
        )

        every { vedtakService.hentAktivForBehandling(any()) } returns vedtak
        every { vedtaksperiodeService.hentPersisterteVedtaksperioder(any()) } returns listOf(
            vedtaksperiodeMedBegrunnelser,
        )

        val mottattDato = LocalDateTime.now()
        every { behandlingSøknadsinfoService.hentSøknadMottattDato(any()) } returns mottattDato

        val tidSattPåVent = now()
        val frist = now().plusWeeks(3)
        every { settPåVentService.finnAktivSettPåVentPåBehandling(any()) } returns lagSettPåVent(
            tidSattPåVent = tidSattPåVent,
            årsak = SettPåVentÅrsak.AVVENTER_DOKUMENTASJON,
            frist = frist,
        )

        val behandlingDvh = sakstatistikkService.mapTilBehandlingDVH(2)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(behandlingDvh))

        assertThat(behandlingDvh?.funksjonellTid).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(behandlingDvh?.tekniskTid).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.MINUTES))
        assertThat(behandlingDvh?.mottattDato).isEqualTo(
            mottattDato.atZone(SaksstatistikkService.TIMEZONE),
        )
        assertThat(behandlingDvh?.registrertDato).isEqualTo(
            behandling.opprettetTidspunkt.atZone(SaksstatistikkService.TIMEZONE),
        )
        assertThat(behandlingDvh?.vedtaksDato).isEqualTo(vedtak.vedtaksdato?.toLocalDate())
        assertThat(behandlingDvh?.behandlingId).isEqualTo(behandling.id.toString())
        assertThat(behandlingDvh?.relatertBehandlingId).isNull()
        assertThat(behandlingDvh?.sakId).isEqualTo(behandling.fagsak.id.toString())
        assertThat(behandlingDvh?.vedtakId).isEqualTo(vedtak.id.toString())
        assertThat(behandlingDvh?.behandlingType).isEqualTo(behandling.type.name)
        assertThat(behandlingDvh?.behandlingStatus).isEqualTo(behandling.status.name)
        assertThat(behandlingDvh?.totrinnsbehandling).isTrue
        assertThat(behandlingDvh?.saksbehandler).isEqualTo("saksbehandlerId")
        assertThat(behandlingDvh?.beslutter).isEqualTo("beslutterId")
        assertThat(behandlingDvh?.avsender).isEqualTo("familie-ba-sak")
        assertThat(behandlingDvh?.versjon).isNotEmpty
        assertThat(behandlingDvh?.settPaaVent?.tidSattPaaVent)
            .isEqualTo(tidSattPåVent.atStartOfDay(SaksstatistikkService.TIMEZONE))
        assertThat(behandlingDvh?.settPaaVent?.aarsak).isEqualTo(SettPåVentÅrsak.AVVENTER_DOKUMENTASJON.name)
        assertThat(behandlingDvh?.settPaaVent?.frist)
            .isEqualTo(frist.atStartOfDay(SaksstatistikkService.TIMEZONE))
    }

    @Test
    fun `skal levere dvh-kodene for sakstype (institusjon, enslig_mindreårig) i behandlingUnderkategori`() {
        every { totrinnskontrollService.hentAktivForBehandling(any()) } returns null
        every { vedtakService.hentAktivForBehandling(any()) } returns null

        listOf(FagsakType.INSTITUSJON, FagsakType.BARN_ENSLIG_MINDREÅRIG, null)
            .forEach { optionalSakstype ->
                val fagsak = defaultFagsak().copy(type = optionalSakstype ?: FagsakType.NORMAL)
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD, fagsak = fagsak)

                every { behandlingHentOgPersisterService.hent(any()) } returns behandling

                val behandlingDvh = sakstatistikkService.mapTilBehandlingDVH(2)
                assertThat("${behandlingDvh?.behandlingUnderkategori}").isSubstringOf("${optionalSakstype?.name}")
            }
    }

    @Test
    fun `Skal mappe til sakDVH, ingen aktiv behandling, så kun aktør SØKER, bostedsadresse i Norge`() {
        every { fagsakService.hentPåFagsakId(any()) } answers {
            Fagsak(status = FagsakStatus.OPPRETTET, aktør = tilAktør("12345678910"))
        }

        every { personidentService.hentAktør("12345678910") } returns Aktør("1234567891000")
        every { personidentService.hentAktør("12345678911") } returns Aktør("1234567891100")
        every { personopplysningerService.hentPersoninfoEnkel(tilAktør("12345678910")) } returns PersonInfo(
            fødselsdato = LocalDate.of(
                2017,
                3,
                1,
            ),
            bostedsadresser = mutableListOf(
                Bostedsadresse(
                    vegadresse = Vegadresse(
                        matrikkelId = 1111,
                        husnummer = null,
                        husbokstav = null,
                        bruksenhetsnummer = null,
                        adressenavn = null,
                        kommunenummer = null,
                        tilleggsnavn = null,
                        postnummer = "2222",
                    ),
                ),
            ),
        )

        every { behandlingHentOgPersisterService.finnAktivForFagsak(any()) } returns null

        val sakDvh = sakstatistikkService.mapTilSakDvh(1)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sakDvh))

        assertThat(sakDvh?.aktorId).isEqualTo(1234567891000)
        assertThat(sakDvh?.aktorer).hasSize(1).extracting("rolle").contains("SØKER")
        assertThat(sakDvh?.sakStatus).isEqualTo(FagsakStatus.OPPRETTET.name)
        assertThat(sakDvh?.avsender).isEqualTo("familie-ba-sak")
        assertThat(sakDvh?.bostedsland).isEqualTo("NO")
    }

    @Test
    fun `Skal mappe til sakDVH, ingen aktiv behandling, så kun aktør SØKER, bostedsadresse i Utland`() {
        every { fagsakService.hentPåFagsakId(any()) } answers {
            Fagsak(status = FagsakStatus.OPPRETTET, aktør = tilAktør("12345678910"))
        }

        every { personidentService.hentAktør("12345678910") } returns Aktør("1234567891000")
        every { personidentService.hentAktør("12345678911") } returns Aktør("1234567891100")

        every { personopplysningerService.hentPersoninfoEnkel(tilAktør("12345678910")) } returns PersonInfo(
            fødselsdato = LocalDate.of(
                2017,
                3,
                1,
            ),
        )
        every { personopplysningerService.hentLandkodeAlpha2UtenlandskBostedsadresse(tilAktør("12345678910")) } returns "SE"

        every { behandlingHentOgPersisterService.finnAktivForFagsak(any()) } returns null

        val sakDvh = sakstatistikkService.mapTilSakDvh(1)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sakDvh))

        assertThat(sakDvh?.aktorId).isEqualTo(1234567891000)
        assertThat(sakDvh?.aktorer).hasSize(1).extracting("rolle").contains("SØKER")
        assertThat(sakDvh?.sakStatus).isEqualTo(FagsakStatus.OPPRETTET.name)
        assertThat(sakDvh?.avsender).isEqualTo("familie-ba-sak")
        assertThat(sakDvh?.bostedsland).isEqualTo("SE")
    }

    @Test
    fun `Skal mappe til sakDVH, aktører har SØKER og BARN`() {
        val randomAktørId = randomAktør()
        val fagsak = Fagsak(status = FagsakStatus.OPPRETTET, aktør = randomAktørId)
        every { fagsakService.hentPåFagsakId(any()) } answers {
            fagsak
        }
        every { personidentService.hentAktør(any()) } returns randomAktørId
        every { personopplysningerService.hentLandkodeAlpha2UtenlandskBostedsadresse(any()) } returns "SE"

        every { persongrunnlagService.hentAktiv(any()) } returns lagTestPersonopplysningGrunnlag(
            1,
            tilfeldigPerson(personType = PersonType.BARN),
            tilfeldigPerson(personType = PersonType.SØKER),
        )

        every { behandlingHentOgPersisterService.finnAktivForFagsak(any()) } returns lagBehandling(fagsak)

        val sakDvh = sakstatistikkService.mapTilSakDvh(1)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sakDvh))

        assertThat(sakDvh?.aktorId).isEqualTo(randomAktørId.aktørId.toLong())
        assertThat(sakDvh?.aktorer).hasSize(2).extracting("rolle").containsOnly("SØKER", "BARN")
        assertThat(sakDvh?.sakStatus).isEqualTo(FagsakStatus.OPPRETTET.name)
        assertThat(sakDvh?.avsender).isEqualTo("familie-ba-sak")
        assertThat(sakDvh?.bostedsland).isEqualTo("SE")
    }

    @Test
    fun `Enum-verdier brukt i behandlingDVH skal validere mot json schema`() {
        val enumVerdier = listOf(
            BehandlingType.values(),
            BehandlingStatus.values(),
            BehandlingUnderkategori.values(),
            BehandlingÅrsak.values(),
            BehandlingKategori.values(),
            Behandlingsresultat.values(),
            SettPåVentÅrsak.values(),
        )

        val alleMuligeResultatBegrunnelser = Standardbegrunnelse.values().map {
            ResultatBegrunnelseDVH(now(), now(), it.vedtakBegrunnelseType.name, it.name)
        }

        for (i in 0..enumVerdier.maxOf { it.size }) {
            val enumI = enumVerdier.map { it.getOrElse(i) { _ -> it.first() } }
            val behandlingType = enumI[0] as BehandlingType
            val behandlingStatus = enumI[1].name
            val behandlingUnderkategori = enumI[2].name
            val behandlingAarsak = enumI[3].name
            val behandlingKategori = enumI[4].name
            val behandlingsresultat = enumI[5].name
            val settPåVentÅrsak = enumI[6].name

            val behandlingDVH = BehandlingDVH(
                funksjonellTid = ZonedDateTime.now(),
                tekniskTid = ZonedDateTime.now(),
                mottattDato = LocalDateTime.now().atZone(SaksstatistikkService.TIMEZONE),
                registrertDato = LocalDateTime.now().atZone(SaksstatistikkService.TIMEZONE),
                behandlingId = nextInt(100000000, 999999999).toString(),
                funksjonellId = UUID.randomUUID().toString(),
                sakId = nextInt(100000000, 999999999).toString(),
                behandlingType = behandlingType.name,
                behandlingStatus = behandlingStatus,
                behandlingKategori = behandlingUnderkategori,
                behandlingAarsak = behandlingAarsak,
                automatiskBehandlet = nextBoolean(),
                utenlandstilsnitt = behandlingKategori,
                ansvarligEnhetKode = "EnhetKodeA",
                behandlendeEnhetKode = "EnhetKodeB",
                ansvarligEnhetType = "NORG",
                behandlendeEnhetType = "NORG",
                totrinnsbehandling = nextBoolean(),
                avsender = "familie-ba-sak",
                versjon = Utils.hentPropertyFraMaven("familie.kontrakter.saksstatistikk") ?: "2",
                // Ikke påkrevde felt
                vedtaksDato = now(),
                relatertBehandlingId = nextInt(100000000, 999999999).toString(),
                vedtakId = nextInt(100000000, 999999999).toString(),
                resultat = behandlingsresultat,
                behandlingTypeBeskrivelse = behandlingType.visningsnavn,
                resultatBegrunnelser = alleMuligeResultatBegrunnelser,
                behandlingOpprettetAv = "behandling.opprettetAv",
                behandlingOpprettetType = "saksbehandlerId",
                behandlingOpprettetTypeBeskrivelse = "saksbehandlerId. VL ved automatisk behandling",
                beslutter = "beslutterId",
                saksbehandler = "saksbehandlerId",
                settPaaVent = SettPåVent(
                    frist = now().atStartOfDay(SaksstatistikkService.TIMEZONE),
                    tidSattPaaVent = now().atStartOfDay(SaksstatistikkService.TIMEZONE),
                    aarsak = settPåVentÅrsak,
                ),
            )
            try {
                validerJsonMotSchema(
                    sakstatistikkObjectMapper.writeValueAsString(behandlingDVH),
                    "/schema/behandling-schema.json",
                )
            } catch (e: Exception) {
                throw IllegalStateException(
                    "Skjema til saksstatistikk validerer ikke etter endringer blant enum-verdier. Sjekk feilmelding og oppdater enten enum til å passe skjema eller skjema til å passe enum.",
                    e,
                )
            }
        }
    }

    @Test
    fun `Enum-verdier brukt i sakDvh skal validere mot json schema`() {
        val deltagere =
            PersonType.values().map { personType -> AktørDVH(randomAktør().aktørId.toLong(), personType.name) }

        FagsakStatus.values().forEach {
            val sakDvh = SakDVH(
                funksjonellTid = ZonedDateTime.now(),
                tekniskTid = ZonedDateTime.now(),
                opprettetDato = now(),
                funksjonellId = UUID.randomUUID().toString(),
                sakId = nextInt(100000000, 999999999).toString(),
                aktorId = deltagere.first().aktorId,
                aktorer = deltagere,
                sakStatus = it.name,
                avsender = "familie-ba-sak",
                versjon = Utils.hentPropertyFraMaven("familie.kontrakter.saksstatistikk") ?: "2",
                bostedsland = "NO",
            )
            try {
                validerJsonMotSchema(
                    sakstatistikkObjectMapper.writeValueAsString(sakDvh),
                    "/schema/sak-schema.json",
                )
            } catch (e: Exception) {
                throw IllegalStateException(
                    "Skjema til saksstatistikk validerer ikke etter endringer blant enum-verdier. Sjekk feilmelding og oppdater enten enum til å passe skjema eller skjema til å passe enum.",
                    e,
                )
            }
        }
    }

    fun validerJsonMotSchema(json: String, schemaPath: String) {
        val api = MedeiaJacksonApi()
        val behandlingSchemaValidator = api.loadSchema(
            UrlSchemaSource(
                object {}::class.java.getResource(schemaPath)!!,
            ),
        )
        val validatedParser = api.decorateJsonParser(
            behandlingSchemaValidator,
            JsonFactory().createParser(json.toByteArray(Charset.defaultCharset())),
        )
        api.parseAll(validatedParser)
    }
}
