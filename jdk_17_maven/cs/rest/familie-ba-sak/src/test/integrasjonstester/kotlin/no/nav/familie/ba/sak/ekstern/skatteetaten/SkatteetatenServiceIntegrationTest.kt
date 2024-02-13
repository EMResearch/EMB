package no.nav.familie.ba.sak.ekstern.skatteetaten

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.nesteBehandlingId
import no.nav.familie.ba.sak.common.nesteVedtakId
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.Personident
import no.nav.familie.ba.sak.kjerne.personident.PersonidentRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPeriode
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioder
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPersonerResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class SkatteetatenServiceIntegrationTest : AbstractSpringIntegrationTest() {

    @Autowired
    lateinit var databaseCleanupService: DatabaseCleanupService

    @Autowired
    lateinit var fagsakRepository: FagsakRepository

    @Autowired
    lateinit var personidentService: PersonidentService

    @Autowired
    lateinit var personidentRepository: PersonidentRepository

    @Autowired
    lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    lateinit var behandlingHentOgPersisterService: BehandlingHentOgPersisterService

    val infotrygdBarnetrygdClientMock = mockk<InfotrygdBarnetrygdClient>()

    lateinit var skatteetatenService: SkatteetatenService

    @BeforeEach
    fun cleanUp() {
        databaseCleanupService.truncate()
    }

    @BeforeAll
    fun init() {
        skatteetatenService =
            SkatteetatenService(
                infotrygdBarnetrygdClientMock,
                fagsakRepository,
                andelTilkjentYtelseRepository,
                behandlingHentOgPersisterService,
            )
    }

    data class PerioderTestData(
        val fnr: String,
        val aktør: Aktør,
        val endretDato: LocalDateTime,
        val perioder: List<Triple<LocalDateTime, LocalDateTime?, SkatteetatenPeriode.Delingsprosent>>,
    )

    @Test
    fun `finnPerioderMedUtvidetBarnetrygd() skal return riktig data`() {
        val duplicatedFnr = "00000000001"
        val excludedFnr = "10000000004"
        val duplicatedAktørId = tilAktør(duplicatedFnr)
        val excludedAktørId = tilAktør(excludedFnr)

        // Result from ba-sak
        val testDataBaSak = arrayOf(
            // Excluded because of the vedtak is older
            PerioderTestData(
                fnr = duplicatedFnr,
                aktør = duplicatedAktørId,
                endretDato = LocalDateTime.of(2020, 11, 5, 12, 0),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2020, 9, 1, 12, 0),
                        LocalDateTime.of(2020, 10, 8, 12, 0),
                        SkatteetatenPeriode.Delingsprosent._0,
                    ),
                ),
            ),
            // Included
            PerioderTestData(
                fnr = duplicatedFnr,
                aktør = duplicatedAktørId,
                endretDato = LocalDateTime.of(2020, 11, 6, 12, 0),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2019, 9, 1, 12, 0),
                        LocalDateTime.of(2020, 7, 31, 12, 0),
                        SkatteetatenPeriode.Delingsprosent._0,
                    ),
                    Triple(
                        LocalDateTime.of(2020, 8, 1, 12, 0),
                        LocalDateTime.of(2020, 12, 8, 12, 0),
                        SkatteetatenPeriode.Delingsprosent._50,
                    ),
                ),
            ),
            // Excluded because the stonad period is earlier than the specified year
            PerioderTestData(
                fnr = "00000000002",
                aktør = tilAktør("00000000002"),
                endretDato = LocalDateTime.of(2020, 8, 5, 12, 0),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2019, 3, 1, 12, 0),
                        LocalDateTime.of(2019, 12, 31, 23, 59),
                        SkatteetatenPeriode.Delingsprosent._0,
                    ),
                ),
            ),
            // Excluded because the stonad period is later than the specified year
            PerioderTestData(
                fnr = "00000000003",
                aktør = tilAktør("00000000003"),
                endretDato = LocalDateTime.of(2020, 8, 5, 12, 0),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2021, 1, 1, 1, 0),
                        LocalDateTime.of(2022, 12, 31, 23, 59),
                        SkatteetatenPeriode.Delingsprosent._0,
                    ),
                ),
            ),
            // Excluded because the person ident is not in the provided list
            PerioderTestData(
                fnr = excludedFnr,
                aktør = excludedAktørId,
                endretDato = LocalDateTime.of(2020, 8, 5, 12, 0),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2020, 1, 1, 1, 0),
                        LocalDateTime.of(2022, 12, 31, 23, 59),
                        SkatteetatenPeriode.Delingsprosent._0,
                    ),
                ),
            ),
        )

        // result from Infotrygd
        val testDataInfotrygd = arrayOf(
            // Excluded because the person ident can be found in ba-sak
            PerioderTestData(
                fnr = duplicatedFnr,
                aktør = duplicatedAktørId,
                endretDato = LocalDateTime.of(2020, 9, 5, 12, 0),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2020, 8, 1, 12, 0),
                        LocalDateTime.of(2020, 9, 8, 12, 0),
                        SkatteetatenPeriode.Delingsprosent._0,
                    ),
                ),
            ),
            // Included
            PerioderTestData(
                fnr = "00000000010",
                aktør = tilAktør("00000000010"),
                endretDato = LocalDateTime.of(2020, 8, 5, 12, 0),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2020, 3, 1, 12, 0),
                        LocalDateTime.of(2020, 4, 8, 12, 0),
                        SkatteetatenPeriode.Delingsprosent._0,
                    ),
                ),
            ),
        )

        testDataBaSak.forEach {
            lagerTilkjentYtelse(it)
        }

        val result = testDataInfotrygd.flatMap {
            listOf(
                SkatteetatenPerioder(
                    it.fnr,
                    it.endretDato,
                    it.perioder.map { p ->
                        SkatteetatenPeriode(
                            fraMaaned = p.first.tilMaaned(),
                            tomMaaned = p.second?.tilMaaned(),
                            delingsprosent = p.third,
                        )
                    },
                ),
            )
        }

        every {
            infotrygdBarnetrygdClientMock.hentPerioderMedUtvidetBarnetrygdForPersoner(
                eq(listOf("00000000001", "00000000002", "00000000003", "00000000010")),
                any(),
            )
        } returns result

        val samletResultat =
            skatteetatenService.finnPerioderMedUtvidetBarnetrygd(
                testDataBaSak.filter { it.fnr != excludedFnr }
                    .map { it.fnr } +
                    testDataInfotrygd.map { it.fnr },
                "2020",
            )

        assertThat(samletResultat.brukere).hasSize(2)
        assertThat(samletResultat.brukere.find { it.ident == duplicatedFnr }!!.perioder).hasSize(2)
        assertThat(
            samletResultat.brukere.find { it.ident == duplicatedFnr }!!.perioder.find {
                it.fraMaaned == "2020-08"
            }!!.delingsprosent,
        ).isEqualTo(
            SkatteetatenPeriode.Delingsprosent._50,
        )
        assertThat(
            samletResultat.brukere.find { it.ident == duplicatedFnr }!!.perioder.find {
                it.tomMaaned == "2020-09"
            }!!.delingsprosent,
        ).isEqualTo(
            SkatteetatenPeriode.Delingsprosent._0,
        )
        assertThat(samletResultat.brukere.find { it.ident == testDataInfotrygd[1].fnr }!!.perioder).hasSize(1)
    }

    @Test
    fun `finnPerioderMedUtvidetBarnetrygd() skal slå sammen data fra infotrygd og ba-sak når overlappende periode`() {
        val fnr = "00000000001"
        val aktør = tilAktør(fnr)

        // Result from ba-sak
        val testDataBaSak = arrayOf(
            // Included
            PerioderTestData(
                fnr = fnr,
                aktør = aktør,
                endretDato = LocalDate.of(2022, 2, 6).atStartOfDay(),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2022, 3, 1, 12, 0),
                        LocalDateTime.of(2027, 7, 31, 12, 0),
                        SkatteetatenPeriode.Delingsprosent._0,
                    ),
                ),
            ),
        )

        // result from Infotrygd
        val testDataInfotrygd = arrayOf(
            // Excluded because the person ident can be found in ba-sak
            PerioderTestData(
                fnr = fnr,
                aktør = aktør,
                endretDato = LocalDate.of(2020, 9, 5).atStartOfDay(),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2019, 9, 1, 12, 0),
                        LocalDateTime.of(2022, 2, 8, 12, 0),
                        SkatteetatenPeriode.Delingsprosent._0,
                    ),
                ),
            ),
        )

        testDataBaSak.forEach {
            lagerTilkjentYtelse(it)
        }

        every {
            infotrygdBarnetrygdClientMock.hentPerioderMedUtvidetBarnetrygdForPersoner(
                eq(listOf("00000000001")),
                any(),
            )
        } returns testDataInfotrygd.flatMap {
            listOf(
                SkatteetatenPerioder(
                    it.fnr,
                    it.endretDato,
                    it.perioder.map { p ->
                        SkatteetatenPeriode(
                            fraMaaned = p.first.tilMaaned(),
                            tomMaaned = p.second?.tilMaaned(),
                            delingsprosent = p.third,
                        )
                    },
                ),
            )
        }

        val resultat = skatteetatenService.finnPerioderMedUtvidetBarnetrygd(listOf((fnr)), "2022")

        assertThat(resultat.brukere).hasSize(1)
        assertThat(resultat.brukere.first().perioder).hasSize(1)
        assertThat(resultat.brukere.first().perioder.first().fraMaaned).isEqualTo("2019-09")
        assertThat(resultat.brukere.first().perioder.first().tomMaaned).isEqualTo("2027-07")
        assertThat(resultat.brukere.first().perioder.first().delingsprosent).isEqualTo(SkatteetatenPeriode.Delingsprosent._0)
        assertThat(resultat.brukere.first().ident).isEqualTo(fnr)
        assertThat(resultat.brukere.first().sisteVedtakPaaIdent).isEqualTo(LocalDate.of(2022, 2, 6).atStartOfDay())
    }

    @Test
    fun `finnPerioderMedUtvidetBarnetrygd() skal slå sammen data fra infotrygd og ba-sak når infotrygdperioden slutter med null fordi den ikke er ferdig opphørt`() {
        val fnr = "00000000001"
        val aktør = tilAktør(fnr)

        // Result from ba-sak
        val testDataBaSak = arrayOf(
            // Included
            PerioderTestData(
                fnr = fnr,
                aktør = aktør,
                endretDato = LocalDate.of(2022, 2, 6).atStartOfDay(),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2022, 3, 1, 12, 0),
                        LocalDateTime.of(2027, 7, 31, 12, 0),
                        SkatteetatenPeriode.Delingsprosent._0,
                    ),
                ),
            ),
        )

        // result from Infotrygd
        val testDataInfotrygd = arrayOf(
            // Excluded because the person ident can be found in ba-sak
            PerioderTestData(
                fnr = fnr,
                aktør = aktør,
                endretDato = LocalDate.of(2020, 9, 5).atStartOfDay(),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2019, 9, 1, 12, 0),
                        null,
                        SkatteetatenPeriode.Delingsprosent._0,
                    ),
                ),
            ),
        )

        testDataBaSak.forEach {
            lagerTilkjentYtelse(it)
        }

        every {
            infotrygdBarnetrygdClientMock.hentPerioderMedUtvidetBarnetrygdForPersoner(
                eq(listOf("00000000001")),
                any(),
            )
        } returns testDataInfotrygd.flatMap {
            listOf(
                SkatteetatenPerioder(
                    it.fnr,
                    it.endretDato,
                    it.perioder.map { p ->
                        SkatteetatenPeriode(
                            fraMaaned = p.first.tilMaaned(),
                            tomMaaned = p.second?.tilMaaned(),
                            delingsprosent = p.third,
                        )
                    },
                ),
            )
        }

        val resultat = skatteetatenService.finnPerioderMedUtvidetBarnetrygd(listOf((fnr)), "2022")

        assertThat(resultat.brukere).hasSize(1)
        assertThat(resultat.brukere.first().perioder).hasSize(1)
        assertThat(resultat.brukere.first().perioder.first().fraMaaned).isEqualTo("2019-09")
        assertThat(resultat.brukere.first().perioder.first().tomMaaned).isEqualTo("2027-07")
        assertThat(resultat.brukere.first().perioder.first().delingsprosent).isEqualTo(SkatteetatenPeriode.Delingsprosent._0)
        assertThat(resultat.brukere.first().ident).isEqualTo(fnr)
        assertThat(resultat.brukere.first().sisteVedtakPaaIdent).isEqualTo(LocalDate.of(2022, 2, 6).atStartOfDay())
    }

    @Test
    fun `finnPerioderMedUtvidetBarnetrygd() skal slå sammen data fra infotrygd og ba-sak når overlappende periode  mellom ba-sak og infotrygd, noe som typisk skjer ved endret migreringsdato`() {
        val fnr = "00000000001"
        val aktør = tilAktør(fnr)

        // Result from ba-sak
        val testDataBaSak = arrayOf(
            // Included
            PerioderTestData(
                fnr = fnr,
                aktør = aktør,
                endretDato = LocalDate.of(2022, 2, 6).atStartOfDay(),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2021, 9, 1, 12, 0),
                        LocalDateTime.of(2027, 7, 31, 12, 0),
                        SkatteetatenPeriode.Delingsprosent._0,
                    ),
                ),
            ),
        )

        // result from Infotrygd
        val testDataInfotrygd = arrayOf(
            // Excluded because the person ident can be found in ba-sak
            PerioderTestData(
                fnr = fnr,
                aktør = aktør,
                endretDato = LocalDate.of(2020, 9, 5).atStartOfDay(),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2019, 9, 1, 12, 0),
                        LocalDateTime.of(2022, 3, 1, 12, 0),
                        SkatteetatenPeriode.Delingsprosent._0,
                    ),
                ),
            ),
        )

        testDataBaSak.forEach {
            lagerTilkjentYtelse(it)
        }

        every {
            infotrygdBarnetrygdClientMock.hentPerioderMedUtvidetBarnetrygdForPersoner(
                eq(listOf("00000000001")),
                any(),
            )
        } returns testDataInfotrygd.flatMap {
            listOf(
                SkatteetatenPerioder(
                    it.fnr,
                    it.endretDato,
                    it.perioder.map { p ->
                        SkatteetatenPeriode(
                            fraMaaned = p.first.tilMaaned(),
                            tomMaaned = p.second?.tilMaaned(),
                            delingsprosent = p.third,
                        )
                    },
                ),
            )
        }

        val resultat = skatteetatenService.finnPerioderMedUtvidetBarnetrygd(listOf((fnr)), "2022")

        assertThat(resultat.brukere).hasSize(1)
        assertThat(resultat.brukere.first().perioder).hasSize(1)
        assertThat(resultat.brukere.first().perioder.first().fraMaaned).isEqualTo("2019-09")
        assertThat(resultat.brukere.first().perioder.first().tomMaaned).isEqualTo("2027-07")
        assertThat(resultat.brukere.first().perioder.first().delingsprosent).isEqualTo(SkatteetatenPeriode.Delingsprosent._0)
        assertThat(resultat.brukere.first().ident).isEqualTo(fnr)
        assertThat(resultat.brukere.first().sisteVedtakPaaIdent).isEqualTo(LocalDate.of(2022, 2, 6).atStartOfDay())
    }

    @Test
    fun `finnPerioderMedUtvidetBarnetrygd() skal slå sammen perioder basert på prosent`() {
        val fnr = "00000000001"
        val aktørId = tilAktør("00000000001")
        val excludedFnr = "10000000004"

        // Result from ba-sak
        val testDataBaSak = arrayOf(
            PerioderTestData(
                fnr = fnr,
                aktør = aktørId,
                endretDato = LocalDateTime.of(2020, 11, 6, 12, 0),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2019, 9, 1, 12, 0),
                        LocalDateTime.of(2020, 2, 11, 12, 0),
                        SkatteetatenPeriode.Delingsprosent._0,
                    ),
                    Triple(
                        LocalDateTime.of(2020, 3, 1, 12, 0),
                        LocalDateTime.of(2020, 4, 8, 12, 0),
                        SkatteetatenPeriode.Delingsprosent._0,
                    ),
                    Triple(
                        LocalDateTime.of(2020, 5, 1, 12, 0),
                        LocalDateTime.of(2020, 6, 8, 12, 0),
                        SkatteetatenPeriode.Delingsprosent._0,
                    ),
                    Triple(
                        LocalDateTime.of(2020, 7, 1, 12, 0),
                        LocalDateTime.of(2020, 8, 8, 12, 0),
                        SkatteetatenPeriode.Delingsprosent._50,
                    ),
                    Triple(
                        LocalDateTime.of(2020, 9, 1, 12, 0),
                        LocalDateTime.of(2020, 11, 8, 12, 0),
                        SkatteetatenPeriode.Delingsprosent._0,
                    ),
                ),
            ),
        )

        testDataBaSak.forEach {
            every {
                infotrygdBarnetrygdClientMock.hentPerioderMedUtvidetBarnetrygdForPersoner(
                    eq(listOf(it.fnr)),
                    any(),
                )
            } returns emptyList()

            lagerTilkjentYtelse(it)
        }

        val samletResultat =
            skatteetatenService.finnPerioderMedUtvidetBarnetrygd(
                testDataBaSak.filter { it.fnr != excludedFnr }
                    .map { it.fnr },
                "2020",
            )

        assertThat(samletResultat.brukere).hasSize(1)
        assertThat(samletResultat.brukere.find { it.ident == fnr }!!.perioder).hasSize(3)
        val sortertePerioder = samletResultat.brukere.find { it.ident == fnr }!!.perioder.sortedBy { it.fraMaaned }
        assertThat(sortertePerioder[0].delingsprosent).isEqualTo(
            SkatteetatenPeriode.Delingsprosent._0,
        )
        assertThat(sortertePerioder[0].fraMaaned).isEqualTo(
            "2019-09",
        )
        assertThat(sortertePerioder[0].tomMaaned).isEqualTo(
            "2020-06",
        )

        assertThat(sortertePerioder[1].delingsprosent).isEqualTo(
            SkatteetatenPeriode.Delingsprosent._50,
        )
        assertThat(sortertePerioder[1].fraMaaned).isEqualTo("2020-07")
        assertThat(sortertePerioder[1].tomMaaned).isEqualTo(
            "2020-08",
        )

        assertThat(sortertePerioder[2].delingsprosent).isEqualTo(
            SkatteetatenPeriode.Delingsprosent._0,
        )
        assertThat(sortertePerioder[2].fraMaaned).isEqualTo(
            "2020-09",
        )
        assertThat(sortertePerioder[2].tomMaaned).isEqualTo(
            "2020-11",
        )
    }

    @Test
    fun `finnPerioderMedUtvidetBarnetrygd() skal IKKE finne perioder for år 2021 etter en revurdering med ny stønadTom 2020`() {
        val fnr = "00000000001"

        // Result from ba-sak
        val testDataBaSak =
            PerioderTestData(
                fnr = fnr,
                aktør = tilAktør(fnr),
                endretDato = LocalDateTime.of(2020, 11, 6, 12, 0),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2019, 9, 1, 12, 0),
                        LocalDateTime.of(2029, 7, 31, 12, 0),
                        SkatteetatenPeriode.Delingsprosent._0,
                    ),
                ),
            )

        lagerTilkjentYtelse(testDataBaSak)

        every {
            infotrygdBarnetrygdClientMock.hentPerioderMedUtvidetBarnetrygdForPersoner(
                eq(listOf(fnr)),
                any(),
            )
        } returns emptyList()

        var resultat =
            skatteetatenService.finnPerioderMedUtvidetBarnetrygd(
                listOf(testDataBaSak.fnr),
                "2021",
            )

        assertThat(resultat.brukere).hasSize(1)

        lagRevurderingMedNyStonadTom(testDataBaSak, YearMonth.of(2020, 12))

        resultat =
            skatteetatenService.finnPerioderMedUtvidetBarnetrygd(
                listOf(testDataBaSak.fnr),
                "2021",
            )

        assertThat(resultat.brukere).hasSize(0)
    }

    @Test
    fun `finnPersonerMedUtvidetBarnetrygd() skal IKKE ta med historisk ident som en ekstra person`() {
        val fnr = "00000000002"
        val historiskIdent = "00000000001"

        // Result from ba-sak
        val testDataBaSak =
            PerioderTestData(
                fnr = fnr,
                aktør = tilAktør(fnr).also { it.personidenter.add(Personident(historiskIdent, aktiv = false, aktør = it)) },
                endretDato = LocalDateTime.of(2020, 11, 6, 12, 0),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2019, 9, 1, 12, 0),
                        LocalDateTime.of(2029, 7, 31, 12, 0),
                        SkatteetatenPeriode.Delingsprosent._0,
                    ),
                ),
            )

        lagerTilkjentYtelse(testDataBaSak)

        every {
            infotrygdBarnetrygdClientMock.hentPersonerMedUtvidetBarnetrygd(
                any(),
            )
        } returns SkatteetatenPersonerResponse()

        val resultat =
            skatteetatenService.finnPersonerMedUtvidetBarnetrygd(
                "2021",
            )

        assertThat(resultat.brukere).hasSize(1)
        assertThat(resultat.brukere.first().ident == fnr)
    }

    fun lagerTilkjentYtelse(perioderTestData: PerioderTestData) {
        val fødselsnummer = perioderTestData.aktør.aktivFødselsnummer()
        val aktør = perioderTestData.aktør
        personidentService.hentOgLagreAktør(fødselsnummer, true).also {
            personidentRepository.saveAll(aktør.personidenter.filter { !it.aktiv })
        }

        val fagsak = fagsakRepository.finnFagsakForAktør(aktør) ?: fagsakRepository.saveAndFlush(Fagsak(aktør = aktør))

        val behandling = Behandling(
            fagsak = fagsak,
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            opprettetÅrsak = BehandlingÅrsak.MIGRERING,
            kategori = BehandlingKategori.NASJONAL,
            underkategori = BehandlingUnderkategori.UTVIDET,
            status = BehandlingStatus.AVSLUTTET,
            aktiv = false,
        )
        behandlingHentOgPersisterService.lagreOgFlush(behandling)

        val ty = TilkjentYtelse(
            behandling = behandling,
            opprettetDato = perioderTestData.endretDato.toLocalDate(),
            endretDato = perioderTestData.endretDato.toLocalDate(),
            utbetalingsoppdrag = "utbetalt",
        ).also {
            it.andelerTilkjentYtelse.addAll(
                perioderTestData.perioder.map { p ->
                    AndelTilkjentYtelse(
                        behandlingId = it.behandling.id,
                        tilkjentYtelse = it,
                        aktør = perioderTestData.aktør,
                        kalkulertUtbetalingsbeløp = 1000,
                        nasjonaltPeriodebeløp = 1000,
                        stønadFom = YearMonth.of(p.first.year, p.first.month),
                        stønadTom = YearMonth.of(p.second!!.year, p.second!!.month),
                        type = YtelseType.UTVIDET_BARNETRYGD,
                        sats = 1,
                        prosent = p.third.tilBigDecimal(),
                    )
                }.toMutableSet(),
            )
        }
        tilkjentYtelseRepository.saveAndFlush(ty)
    }

    fun lagRevurderingMedNyStonadTom(perioderTestData: PerioderTestData, stønadTom: YearMonth) {
        val fødselsnummer = perioderTestData.aktør.aktivFødselsnummer()
        val aktør = personidentService.hentOgLagreAktør(fødselsnummer, false)

        val fagsak = fagsakRepository.finnFagsakForAktør(aktør)!!

        val behandling = behandlingHentOgPersisterService.lagreOgFlush(
            Behandling(
                id = nesteBehandlingId(),
                fagsak = fagsak,
                type = BehandlingType.REVURDERING,
                opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.UTVIDET,
            ),
        )

        val ty = TilkjentYtelse(
            id = nesteVedtakId(),
            behandling = behandling,
            opprettetDato = perioderTestData.endretDato.toLocalDate(),
            endretDato = perioderTestData.endretDato.toLocalDate(),
            utbetalingsoppdrag = "utbetalt",
        ).also {
            it.andelerTilkjentYtelse.addAll(
                perioderTestData.perioder.map { p ->
                    AndelTilkjentYtelse(
                        behandlingId = it.behandling.id,
                        tilkjentYtelse = it,
                        aktør = perioderTestData.aktør,
                        kalkulertUtbetalingsbeløp = 1000,
                        nasjonaltPeriodebeløp = 1000,
                        stønadFom = YearMonth.of(p.first.year, p.first.month),
                        stønadTom = stønadTom,
                        type = YtelseType.UTVIDET_BARNETRYGD,
                        sats = 1,
                        prosent = p.third.tilBigDecimal(),
                    )
                }.toMutableSet(),
            )
        }
        tilkjentYtelseRepository.saveAndFlush(ty)
    }
}

fun LocalDateTime.tilMaaned(): String = this.format(DateTimeFormatter.ofPattern("yyyy-MM"))
