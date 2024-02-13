package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.dato
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.common.årMnd
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.ekstern.restDomene.InstitusjonInfo
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningTestUtil.sisteAndelPerIdent
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth

class UtbetalingsoppdragIntegrasjonTest(
    @Autowired
    private val beregningService: BeregningService,

    @Autowired
    private val personidentService: PersonidentService,

    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,

    @Autowired
    private val utbetalingsoppdragGeneratorService: UtbetalingsoppdragGeneratorService,

    @Autowired
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) : AbstractSpringIntegrationTest() {

    lateinit var utbetalingsoppdragGenerator: UtbetalingsoppdragGenerator

    @BeforeEach
    fun setUp() {
        databaseCleanupService.truncate()
        utbetalingsoppdragGenerator = UtbetalingsoppdragGenerator(beregningService)
    }

    @Test
    fun `skal opprette et nytt utbetalingsoppdrag med felles løpende periodeId og separat kjeding på to personer`() {
        val personMedFlerePerioder = tilfeldigPerson()
        val tilfeldigPerson = tilfeldigPerson()
        val fagsak =
            fagsakService.hentEllerOpprettFagsakForPersonIdent(personMedFlerePerioder.aktør.aktivFødselsnummer())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val vedtak = lagVedtak(behandling = behandling)
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val andelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2019-04"),
                årMnd("2023-03"),
                YtelseType.SMÅBARNSTILLEGG,
                660,
                behandling,
                person = personMedFlerePerioder,
                aktør = personidentService.hentOgLagreAktør(personMedFlerePerioder.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = tilkjentYtelse,
            ),
            lagAndelTilkjentYtelse(
                årMnd("2026-05"),
                årMnd("2027-06"),
                YtelseType.SMÅBARNSTILLEGG,
                660,
                behandling,
                person = personMedFlerePerioder,
                aktør = personidentService.hentOgLagreAktør(personMedFlerePerioder.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = tilkjentYtelse,
            ),
            lagAndelTilkjentYtelse(
                årMnd("2019-03"),
                årMnd("2037-02"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                tilkjentYtelse = tilkjentYtelse,
                person = tilfeldigPerson,
                aktør = personidentService.hentOgLagreAktør(tilfeldigPerson.aktør.aktivFødselsnummer(), true),
            ),
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelse)

        val utbetalingsoppdrag =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                "saksbehandler",
                vedtak,
                true,
                oppdaterteKjeder = ØkonomiUtils.grupperAndeler(
                    andelerTilkjentYtelse.forIverksetting(),
                ),
            )

        assertEquals(Utbetalingsoppdrag.KodeEndring.NY, utbetalingsoppdrag.kodeEndring)
        assertEquals(3, utbetalingsoppdrag.utbetalingsperiode.size)

        val utbetalingsperioderPerKlasse = utbetalingsoppdrag.utbetalingsperiode.groupBy { it.klassifisering }
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATR")[0],
            2,
            null,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2019-03-01",
            "2037-02-28",
        )
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATRSMA")[0],
            0,
            null,
            fagsak.aktør.aktivFødselsnummer(),
            660,
            "2019-04-01",
            "2023-03-31",
        )
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATRSMA")[1],
            1,
            0,
            fagsak.aktør.aktivFødselsnummer(),
            660,
            "2026-05-01",
            "2027-06-30",
        )
    }

    @Test
    fun `skal opprette et fullstendig opphør for to personer, hvor opphørsdatoer blir første dato i hver kjede`() {
        val personMedFlerePerioder = tilfeldigPerson()
        val fagsak =
            fagsakService.hentEllerOpprettFagsakForPersonIdent(personMedFlerePerioder.aktør.aktivFødselsnummer())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val førsteDatoKjede1 = årMnd("2019-04")
        val førsteDatoKjede2 = årMnd("2019-03")
        val andelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                førsteDatoKjede1,
                årMnd("2023-03"),
                YtelseType.SMÅBARNSTILLEGG,
                660,
                behandling,
                person = personMedFlerePerioder,
                aktør = personidentService.hentOgLagreAktør(personMedFlerePerioder.aktør.aktivFødselsnummer(), true),
                periodeIdOffset = 0,
            ),
            lagAndelTilkjentYtelse(
                årMnd("2026-05"),
                årMnd("2027-06"),
                YtelseType.SMÅBARNSTILLEGG,
                660,
                behandling,
                person = personMedFlerePerioder,
                aktør = personidentService.hentOgLagreAktør(personMedFlerePerioder.aktør.aktivFødselsnummer(), true),
                periodeIdOffset = 1,
            ),
            lagAndelTilkjentYtelse(
                førsteDatoKjede2,
                årMnd("2037-02"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 2,
            ),
        )

        val vedtak = lagVedtak(behandling = behandling)

        val utbetalingsoppdrag =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                saksbehandlerId = "saksbehandler",
                vedtak = vedtak,
                erFørsteBehandlingPåFagsak = false,
                forrigeKjeder = ØkonomiUtils.grupperAndeler(
                    andelerTilkjentYtelse.forIverksetting(),
                ),
                sisteAndelPerIdent = sisteAndelPerIdent(andelerTilkjentYtelse),
            )

        assertEquals(Utbetalingsoppdrag.KodeEndring.ENDR, utbetalingsoppdrag.kodeEndring)
        assertEquals(2, utbetalingsoppdrag.utbetalingsperiode.size)

        val utbetalingsperioderPerKlasse = utbetalingsoppdrag.utbetalingsperiode.groupBy { it.klassifisering }
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATRSMA")[0],
            1,
            null,
            fagsak.aktør.aktivFødselsnummer(),
            660,
            "2026-05-01",
            "2027-06-30",
            førsteDatoKjede1.førsteDagIInneværendeMåned(),
        )
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATR")[0],
            2,
            null,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2019-03-01",
            "2037-02-28",
            førsteDatoKjede2.førsteDagIInneværendeMåned(),
        )
    }

    @Test
    fun `skal opprette revurdering med endring på eksisterende periode`() {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak,
                førsteSteg = StegType.BEHANDLING_AVSLUTTET,
            ),
        )

        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val person = tilfeldigPerson()
        val vedtak = lagVedtak(behandling)
        val fomDatoSomEndres = "2033-01-01"
        val andelerFørstegangsbehandling = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2020-01"),
                årMnd("2029-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 0,
                person = person,
                aktør = personidentService.hentOgLagreAktør(person.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = tilkjentYtelse,
            ),
            lagAndelTilkjentYtelse(
                dato(fomDatoSomEndres).toYearMonth(),
                årMnd("2034-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 1,
                person = person,
                aktør = personidentService.hentOgLagreAktør(person.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = tilkjentYtelse,
            ),
            lagAndelTilkjentYtelse(
                årMnd("2037-01"),
                årMnd("2039-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 2,
                person = person,
                aktør = personidentService.hentOgLagreAktør(person.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = tilkjentYtelse,
            ),
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerFørstegangsbehandling)
        tilkjentYtelse.utbetalingsoppdrag = "Oppdrag"

        utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
            "saksbehandler",
            vedtak,
            true,
            oppdaterteKjeder = ØkonomiUtils.grupperAndeler(
                andelerFørstegangsbehandling.forIverksetting(),
            ),
        )

        avsluttOgLagreBehandling(behandling)
        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val tilkjentYtelse2 = lagInitiellTilkjentYtelse(behandling2)
        val vedtak2 = lagVedtak(behandling2)
        val andelerRevurdering = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2020-01"),
                årMnd("2029-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 0,
                person = person,
                aktør = personidentService.hentOgLagreAktør(person.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = tilkjentYtelse,
            ),
            lagAndelTilkjentYtelse(
                årMnd("2034-01"),
                årMnd("2034-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 3,
                person = person,
                aktør = personidentService.hentOgLagreAktør(person.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = tilkjentYtelse2,
            ),
            lagAndelTilkjentYtelse(
                årMnd("2037-01"),
                årMnd("2039-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 4,
                person = person,
                aktør = personidentService.hentOgLagreAktør(person.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = tilkjentYtelse2,
            ),
        )
        tilkjentYtelse2.andelerTilkjentYtelse.addAll(andelerRevurdering)
        val sisteAndelPerIdent = beregningService.hentSisteAndelPerIdent(behandling.fagsak.id)

        val utbetalingsoppdrag =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                "saksbehandler",
                vedtak2,
                false,
                forrigeKjeder = ØkonomiUtils.grupperAndeler(
                    andelerFørstegangsbehandling.forIverksetting(),
                ),
                sisteAndelPerIdent = sisteAndelPerIdent,
                oppdaterteKjeder = ØkonomiUtils.grupperAndeler(
                    andelerRevurdering.forIverksetting(),
                ),
            )
        avsluttOgLagreBehandling(behandling2)

        assertEquals(Utbetalingsoppdrag.KodeEndring.ENDR, utbetalingsoppdrag.kodeEndring)
        assertEquals(3, utbetalingsoppdrag.utbetalingsperiode.size)

        val opphørsperiode = utbetalingsoppdrag.utbetalingsperiode.find { it.opphør != null }
        assertNotNull(opphørsperiode)
        val nyeUtbetalingsPerioderSortert =
            utbetalingsoppdrag.utbetalingsperiode.filter { it.opphør == null }.sortedBy { it.vedtakdatoFom }
        assertEquals(2, nyeUtbetalingsPerioderSortert.size)

        assertUtbetalingsperiode(
            opphørsperiode!!,
            2,
            1,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2037-01-01",
            "2039-12-31",
            dato(fomDatoSomEndres),
        )
        assertUtbetalingsperiode(
            nyeUtbetalingsPerioderSortert.first(),
            3,
            2,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2034-01-01",
            "2034-12-31",
        )
        assertUtbetalingsperiode(
            nyeUtbetalingsPerioderSortert.last(),
            4,
            3,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2037-01-01",
            "2039-12-31",
        )
    }

    @Test
    fun `Skal opprette revurdering med nytt barn`() {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak,
                førsteSteg = StegType.BEHANDLING_AVSLUTTET,
            ),
        )
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val aktør = personidentService.hentOgLagreAktør(randomFnr(), true)
        val person = tilfeldigPerson(aktør = aktør)
        val vedtak = lagVedtak(behandling)
        val andelerFørstegangsbehandling = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2020-01"),
                årMnd("2029-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 0,
                person = person,
                aktør = aktør,
                tilkjentYtelse = tilkjentYtelse,
            ),
            lagAndelTilkjentYtelse(
                årMnd("2033-01"),
                årMnd("2034-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 1,
                person = person,
                aktør = aktør,
                tilkjentYtelse = tilkjentYtelse,
            ),
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerFørstegangsbehandling)
        tilkjentYtelse.utbetalingsoppdrag = "Oppdrag"

        utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
            "saksbehandler",
            vedtak,
            true,
            oppdaterteKjeder = ØkonomiUtils.grupperAndeler(
                andelerFørstegangsbehandling.forIverksetting(),
            ),
        )

        avsluttOgLagreBehandling(behandling)
        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val tilkjentYtelse2 = lagInitiellTilkjentYtelse(behandling2)
        val nyAktør = personidentService.hentOgLagreAktør(randomFnr(), true)
        val nyPerson = tilfeldigPerson(aktør = nyAktør)
        val vedtak2 = lagVedtak(behandling2)
        val andelerRevurdering = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2022-01"),
                årMnd("2034-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 2,
                person = nyPerson,
                aktør = nyAktør,
                tilkjentYtelse = tilkjentYtelse2,
            ),
            lagAndelTilkjentYtelse(
                årMnd("2037-01"),
                årMnd("2039-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 3,
                person = nyPerson,
                aktør = personidentService.hentOgLagreAktør(nyPerson.aktør.aktørId, true),
                tilkjentYtelse = tilkjentYtelse2,
            ),
        )
        tilkjentYtelse2.andelerTilkjentYtelse.addAll(andelerRevurdering)
        val sisteAndelPerIdent = beregningService.hentSisteAndelPerIdent(behandling2.fagsak.id)

        val utbetalingsoppdrag =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                "saksbehandler",
                vedtak2,
                false,
                forrigeKjeder = ØkonomiUtils.grupperAndeler(
                    andelerFørstegangsbehandling.forIverksetting(),
                ),
                sisteAndelPerIdent = sisteAndelPerIdent,
                oppdaterteKjeder = ØkonomiUtils.grupperAndeler(
                    andelerRevurdering.forIverksetting(),
                ),
            )

        assertEquals(Utbetalingsoppdrag.KodeEndring.ENDR, utbetalingsoppdrag.kodeEndring)
        assertEquals(3, utbetalingsoppdrag.utbetalingsperiode.size)
        val sorterteUtbetalingsperioder = utbetalingsoppdrag.utbetalingsperiode.sortedBy { it.periodeId }
        assertUtbetalingsperiode(
            sorterteUtbetalingsperioder[0],
            1,
            0,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2033-01-01",
            "2034-12-31",
        )
        assertUtbetalingsperiode(
            sorterteUtbetalingsperioder[1],
            2,
            null,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2022-01-01",
            "2034-12-31",
        )
        assertUtbetalingsperiode(
            sorterteUtbetalingsperioder[2],
            3,
            2,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2037-01-01",
            "2039-12-31",
        )
    }

    @Test
    fun `skal opprette et nytt utbetalingsoppdrag med to andeler på samme person og separat kjeding for småbarnstillegg`() {
        val personMedFlerePerioder = tilfeldigPerson()
        val fagsak =
            fagsakService.hentEllerOpprettFagsakForPersonIdent(personMedFlerePerioder.aktør.aktivFødselsnummer())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val vedtak = lagVedtak(behandling = behandling)
        val andelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2019-04"),
                årMnd("2023-03"),
                YtelseType.SMÅBARNSTILLEGG,
                660,
                behandling,
                person = personMedFlerePerioder,
                aktør = personidentService.hentOgLagreAktør(personMedFlerePerioder.aktør.aktørId, true),
            ),
            lagAndelTilkjentYtelse(
                årMnd("2026-05"),
                årMnd("2027-06"),
                YtelseType.SMÅBARNSTILLEGG,
                660,
                behandling,
                person = personMedFlerePerioder,
                aktør = personidentService.hentOgLagreAktør(personMedFlerePerioder.aktør.aktørId, true),
            ),
            lagAndelTilkjentYtelse(
                årMnd("2019-03"),
                årMnd("2037-02"),
                YtelseType.UTVIDET_BARNETRYGD,
                1054,
                behandling,
                person = personMedFlerePerioder,
                aktør = personidentService.hentOgLagreAktør(personMedFlerePerioder.aktør.aktørId, true),
            ),
        )

        val utbetalingsoppdrag = utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
            "saksbehandler",
            vedtak,
            true,
            oppdaterteKjeder = ØkonomiUtils.grupperAndeler(
                andelerTilkjentYtelse.forIverksetting(),
            ),
        )

        assertEquals(Utbetalingsoppdrag.KodeEndring.NY, utbetalingsoppdrag.kodeEndring)
        assertEquals(3, utbetalingsoppdrag.utbetalingsperiode.size)

        val utbetalingsperioderPerKlasse = utbetalingsoppdrag.utbetalingsperiode.groupBy { it.klassifisering }
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATR")[0],
            2,
            null,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2019-03-01",
            "2037-02-28",
        )
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATRSMA")[0],
            0,
            null,
            fagsak.aktør.aktivFødselsnummer(),
            660,
            "2019-04-01",
            "2023-03-31",
        )
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATRSMA")[1],
            1,
            0,
            fagsak.aktør.aktivFødselsnummer(),
            660,
            "2026-05-01",
            "2027-06-30",
        )
    }

    @Test
    fun `opprettelse av utbetalingsoppdrag hvor flere har småbarnstillegg kaster feil`() {
        val behandling = lagBehandling()
        val vedtak = lagVedtak(behandling = behandling)
        val andelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(årMnd("2019-04"), årMnd("2023-03"), YtelseType.SMÅBARNSTILLEGG, 660, behandling),
            lagAndelTilkjentYtelse(årMnd("2026-05"), årMnd("2027-06"), YtelseType.SMÅBARNSTILLEGG, 660, behandling),
        )

        assertThrows<java.lang.IllegalArgumentException> {
            utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                "saksbehandler",
                vedtak,
                true,
                oppdaterteKjeder = ØkonomiUtils.grupperAndeler(
                    andelerTilkjentYtelse.forIverksetting(),
                ),
            )
        }
    }

    @Test
    fun `Ved full betalingsoppdrag skal komplett utbetalinsoppdrag genereres også når ingen endring blitt gjort`() {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak,
                førsteSteg = StegType.BEHANDLING_AVSLUTTET,
            ),
        )

        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val person = tilfeldigPerson()
        val vedtak = lagVedtak(behandling)
        val andelerFørstegangsbehandling = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2020-01"),
                årMnd("2029-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 0,
                person = person,
                aktør = personidentService.hentOgLagreAktør(person.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = tilkjentYtelse,
            ),
            lagAndelTilkjentYtelse(
                årMnd("2030-01"),
                årMnd("2034-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 1,
                person = person,
                aktør = personidentService.hentOgLagreAktør(person.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = tilkjentYtelse,
            ),
            lagAndelTilkjentYtelse(
                årMnd("2035-01"),
                årMnd("2039-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 2,
                person = person,
                aktør = personidentService.hentOgLagreAktør(person.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = tilkjentYtelse,
            ),
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerFørstegangsbehandling)

        tilkjentYtelse.utbetalingsoppdrag = "Oppdrag"

        utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
            "saksbehandler",
            vedtak,
            true,
            oppdaterteKjeder = ØkonomiUtils.grupperAndeler(
                andelerFørstegangsbehandling.forIverksetting(),
            ),
        )

        avsluttOgLagreBehandling(behandling)
        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val tilkjentYtelse2 = lagInitiellTilkjentYtelse(behandling2)
        val vedtak2 = lagVedtak(behandling2)
        val andelerRevurdering = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2020-01"),
                årMnd("2029-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 0,
                person = person,
                aktør = personidentService.hentOgLagreAktør(person.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = tilkjentYtelse,
            ),
            lagAndelTilkjentYtelse(
                årMnd("2030-01"),
                årMnd("2034-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 3,
                person = person,
                aktør = personidentService.hentOgLagreAktør(person.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = tilkjentYtelse2,
            ),
            lagAndelTilkjentYtelse(
                årMnd("2035-01"),
                årMnd("2039-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 4,
                person = person,
                aktør = personidentService.hentOgLagreAktør(person.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = tilkjentYtelse2,
            ),
        )
        tilkjentYtelse2.andelerTilkjentYtelse.addAll(andelerRevurdering)
        val sisteAndelPerIdent = beregningService.hentSisteAndelPerIdent(behandling2.fagsak.id)

        val utbetalingsoppdrag =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                "saksbehandler",
                vedtak2,
                false,
                forrigeKjeder = ØkonomiUtils.grupperAndeler(
                    andelerFørstegangsbehandling.forIverksetting(),
                ),
                sisteAndelPerIdent = sisteAndelPerIdent,
                oppdaterteKjeder = ØkonomiUtils.grupperAndeler(
                    andelerRevurdering.forIverksetting(),
                ),
                erSimulering = true,
            )

        assertEquals(Utbetalingsoppdrag.KodeEndring.ENDR, utbetalingsoppdrag.kodeEndring)
        assertEquals(4, utbetalingsoppdrag.utbetalingsperiode.size)

        val opphørsperiode = utbetalingsoppdrag.utbetalingsperiode.find { it.opphør != null }
        assertNotNull(opphørsperiode)
        val nyeUtbetalingsPerioderSortert =
            utbetalingsoppdrag.utbetalingsperiode.filter { it.opphør == null }.sortedBy { it.vedtakdatoFom }
        assertEquals(3, nyeUtbetalingsPerioderSortert.size)

        assertUtbetalingsperiode(
            opphørsperiode!!,
            2,
            1,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2035-01-01",
            "2039-12-31",
            dato("2020-01-01"),
        )
        assertUtbetalingsperiode(
            nyeUtbetalingsPerioderSortert.first(),
            3,
            2,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2020-01-01",
            "2029-12-31",
        )
        assertUtbetalingsperiode(
            nyeUtbetalingsPerioderSortert[1],
            4,
            3,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2030-01-01",
            "2034-12-31",
        )
        assertUtbetalingsperiode(
            nyeUtbetalingsPerioderSortert.last(),
            5,
            4,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2035-01-01",
            "2039-12-31",
        )
    }

    @Test
    fun `Ved full betalingsoppdrag skal komplett utbetalinsoppdrag genereres også når bare siste periode blitt endrett`() {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak,
                førsteSteg = StegType.BEHANDLING_AVSLUTTET,
            ),
        )

        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val person = tilfeldigPerson()
        val vedtak = lagVedtak(behandling)
        val andelerFørstegangsbehandling = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2020-01"),
                årMnd("2029-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 0,
                person = person,
                aktør = personidentService.hentOgLagreAktør(person.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = tilkjentYtelse,
            ),
            lagAndelTilkjentYtelse(
                årMnd("2030-01"),
                årMnd("2034-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 1,
                person = person,
                aktør = personidentService.hentOgLagreAktør(person.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = tilkjentYtelse,
            ),
            lagAndelTilkjentYtelse(
                årMnd("2035-01"),
                årMnd("2039-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 2,
                person = person,
                aktør = personidentService.hentOgLagreAktør(person.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = tilkjentYtelse,
            ),
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerFørstegangsbehandling)
        tilkjentYtelse.utbetalingsoppdrag = "Oppdrag"

        utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
            "saksbehandler",
            vedtak,
            true,
            oppdaterteKjeder = ØkonomiUtils.grupperAndeler(
                andelerFørstegangsbehandling.forIverksetting(),
            ),
        )
        avsluttOgLagreBehandling(behandling)
        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val tilkjentYtelse2 = lagInitiellTilkjentYtelse(behandling2)
        val vedtak2 = lagVedtak(behandling2)
        val andelerRevurdering = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2020-01"),
                årMnd("2029-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 0,
                person = person,
                aktør = personidentService.hentOgLagreAktør(person.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = tilkjentYtelse,
            ),
            lagAndelTilkjentYtelse(
                årMnd("2030-01"),
                årMnd("2034-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 3,
                person = person,
                aktør = personidentService.hentOgLagreAktør(person.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = tilkjentYtelse2,
            ),
            lagAndelTilkjentYtelse(
                årMnd("2035-01"),
                årMnd("2038-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling2,
                periodeIdOffset = 4,
                person = person,
                aktør = personidentService.hentOgLagreAktør(person.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = tilkjentYtelse2,
            ),
        )
        tilkjentYtelse2.andelerTilkjentYtelse.addAll(andelerRevurdering)

        val sisteAndelPerIdent = beregningService.hentSisteAndelPerIdent(behandling2.fagsak.id)

        val utbetalingsoppdrag =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                "saksbehandler",
                vedtak2,
                false,
                forrigeKjeder = ØkonomiUtils.grupperAndeler(
                    andelerFørstegangsbehandling.forIverksetting(),
                ),
                sisteAndelPerIdent = sisteAndelPerIdent,
                oppdaterteKjeder = ØkonomiUtils.grupperAndeler(
                    andelerRevurdering.forIverksetting(),
                ),
                erSimulering = true,
            )

        assertEquals(Utbetalingsoppdrag.KodeEndring.ENDR, utbetalingsoppdrag.kodeEndring)
        assertEquals(4, utbetalingsoppdrag.utbetalingsperiode.size)

        val opphørsperiode = utbetalingsoppdrag.utbetalingsperiode.find { it.opphør != null }
        assertNotNull(opphørsperiode)
        val nyeUtbetalingsPerioderSortert =
            utbetalingsoppdrag.utbetalingsperiode.filter { it.opphør == null }.sortedBy { it.vedtakdatoFom }
        assertEquals(3, nyeUtbetalingsPerioderSortert.size)

        assertUtbetalingsperiode(
            opphørsperiode!!,
            2,
            1,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2035-01-01",
            "2039-12-31",
            dato("2020-01-01"),
        )
        assertUtbetalingsperiode(
            nyeUtbetalingsPerioderSortert.first(),
            3,
            2,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2020-01-01",
            "2029-12-31",
        )
        assertUtbetalingsperiode(
            nyeUtbetalingsPerioderSortert[1],
            4,
            3,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2030-01-01",
            "2034-12-31",
        )
        assertUtbetalingsperiode(
            nyeUtbetalingsPerioderSortert.last(),
            5,
            4,
            fagsak.aktør.aktivFødselsnummer(),
            1054,
            "2035-01-01",
            "2038-12-31",
        )
    }

    @Test
    fun `Skal teste uthenting av offset på revurderinger`() {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak,
                førsteSteg = StegType.BEHANDLING_AVSLUTTET,
            ),
        )

        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val person = tilfeldigPerson()
        val vedtak = lagVedtak(behandling)
        val andelerFørstegangsbehandling = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2020-01"),
                årMnd("2029-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                periodeIdOffset = 0,
                person = person,
                aktør = personidentService.hentOgLagreAktør(person.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = tilkjentYtelse,
            ),
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerFørstegangsbehandling)
        tilkjentYtelse.utbetalingsoppdrag = "Oppdrag"

        utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
            "saksbehandler",
            vedtak,
            true,
            oppdaterteKjeder = ØkonomiUtils.grupperAndeler(
                andelerFørstegangsbehandling.forIverksetting(),
            ),
        )

        avsluttOgLagreBehandling(behandling)
        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            (
                lagBehandling(
                    fagsak,
                    førsteSteg = StegType.BEHANDLING_AVSLUTTET,
                )
                ),
        )
        val tilkjentYtelse2 = lagInitiellTilkjentYtelse(behandling2)
        val andelerRevurdering = emptyList<AndelTilkjentYtelse>()
        tilkjentYtelse2.andelerTilkjentYtelse.addAll(andelerRevurdering)
        tilkjentYtelse2.utbetalingsoppdrag = "Oppdrag"

        utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
            "saksbehandler",
            vedtak,
            false,
            oppdaterteKjeder = ØkonomiUtils.grupperAndeler(
                andelerRevurdering.forIverksetting(),
            ),
        )

        avsluttOgLagreBehandling(behandling2)
        val behandling3 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val tilkjentYtelse3 = lagInitiellTilkjentYtelse(behandling3)
        val andelerRevurdering2 = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2020-01"),
                årMnd("2029-12"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling3,
                periodeIdOffset = 0,
                person = person,
                aktør = personidentService.hentOgLagreAktør(person.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = tilkjentYtelse,
            ),
        )
        tilkjentYtelse3.andelerTilkjentYtelse.addAll(andelerRevurdering2)

        assertEquals(
            0,
            beregningService.hentSisteAndelPerIdent(behandling3.fagsak.id).maxOf { it.value.periodeOffset!! },
        )
    }

    @Test
    fun `Skal opphøre tideligere utbetaling hvis barnet ikke har utbetaling i den nye behandlingen`() {
        val søker = tilfeldigPerson()
        val førsteBarnet = tilfeldigPerson()
        val andreBarnet = tilfeldigPerson()

        val fagsak =
            fagsakService.hentEllerOpprettFagsakForPersonIdent(søker.aktør.aktivFødselsnummer())
        val førsteBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val førsteVedtak = lagVedtak(behandling = førsteBehandling)

        val førsteTilkjentYtelse = lagInitiellTilkjentYtelse(førsteBehandling)
        val førsteAndelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2019-04"),
                årMnd("2023-03"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1345,
                førsteBehandling,
                person = førsteBarnet,
                aktør = personidentService.hentOgLagreAktør(førsteBarnet.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = førsteTilkjentYtelse,
            ),
            lagAndelTilkjentYtelse(
                årMnd("2023-04"),
                årMnd("2027-06"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                førsteBehandling,
                person = førsteBarnet,
                aktør = personidentService.hentOgLagreAktør(førsteBarnet.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = førsteTilkjentYtelse,
            ),
        )
        førsteTilkjentYtelse.andelerTilkjentYtelse.addAll(førsteAndelerTilkjentYtelse)
        førsteTilkjentYtelse.utbetalingsoppdrag = "utbetalingsoppdrg"
        tilkjentYtelseRepository.saveAndFlush(førsteTilkjentYtelse)

        utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
            førsteVedtak,
            "Z123",
            AndelTilkjentYtelseForIverksettingFactory(),
        )
        avsluttOgLagreBehandling(førsteBehandling)

        val andreBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak,
                behandlingType = BehandlingType.REVURDERING,
            ),
        )
        val andreVedtak = lagVedtak(behandling = andreBehandling)

        val andreTilkjentYtelse = lagInitiellTilkjentYtelse(andreBehandling)
        val andreAndelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2019-04"),
                årMnd("2023-03"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1345,
                andreBehandling,
                person = andreBarnet,
                aktør = personidentService.hentOgLagreAktør(andreBarnet.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = andreTilkjentYtelse,
            ),
            lagAndelTilkjentYtelse(
                årMnd("2023-04"),
                årMnd("2027-06"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                andreBehandling,
                person = andreBarnet,
                aktør = personidentService.hentOgLagreAktør(andreBarnet.aktør.aktivFødselsnummer(), true),
                tilkjentYtelse = andreTilkjentYtelse,
            ),
        )
        andreTilkjentYtelse.andelerTilkjentYtelse.addAll(andreAndelerTilkjentYtelse)
        tilkjentYtelseRepository.saveAndFlush(andreTilkjentYtelse)

        val utbetalingsoppdrag = utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
            andreVedtak,
            "Z123",
            AndelTilkjentYtelseForIverksettingFactory(),
        )
        assertEquals(Utbetalingsoppdrag.KodeEndring.ENDR, utbetalingsoppdrag.kodeEndring)
        assertEquals(3, utbetalingsoppdrag.utbetalingsperiode.size)
        assertEquals(true, utbetalingsoppdrag.utbetalingsperiode.first().erEndringPåEksisterendePeriode)
        assertEquals(Opphør(YearMonth.of(2019, 4).toLocalDate()), utbetalingsoppdrag.utbetalingsperiode.first().opphør)
        assertEquals(0, utbetalingsoppdrag.utbetalingsperiode.first().forrigePeriodeId)
        assertEquals(false, utbetalingsoppdrag.utbetalingsperiode[1].erEndringPåEksisterendePeriode)
        assertNull(utbetalingsoppdrag.utbetalingsperiode[1].opphør)
        assertNull(utbetalingsoppdrag.utbetalingsperiode[1].forrigePeriodeId)
    }

    @Test
    fun `skal opprette et nytt utbetalingsoppdrag for institusjon`() {
        val tilfeldigPerson = tilfeldigPerson()
        val fagsak =
            fagsakService.hentEllerOpprettFagsakForPersonIdent(
                tilfeldigPerson.aktør.aktivFødselsnummer(),
                fagsakType = FagsakType.INSTITUSJON,
                institusjon = InstitusjonInfo(ORGNUMMER, TSS_ID_INSTITUSJON),
            )
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val vedtak = lagVedtak(behandling = behandling)
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling)
        val andelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                årMnd("2019-03"),
                årMnd("2037-02"),
                YtelseType.ORDINÆR_BARNETRYGD,
                1054,
                behandling,
                tilkjentYtelse = tilkjentYtelse,
                person = tilfeldigPerson,
                aktør = personidentService.hentOgLagreAktør(tilfeldigPerson.aktør.aktivFødselsnummer(), true),
            ),
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelse)

        val utbetalingsoppdrag =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                "saksbehandler",
                vedtak,
                true,
                oppdaterteKjeder = ØkonomiUtils.grupperAndeler(
                    andelerTilkjentYtelse.forIverksetting(),
                ),
            )

        assertEquals(Utbetalingsoppdrag.KodeEndring.NY, utbetalingsoppdrag.kodeEndring)
        assertEquals(1, utbetalingsoppdrag.utbetalingsperiode.size)

        val utbetalingsperioderPerKlasse = utbetalingsoppdrag.utbetalingsperiode.groupBy { it.klassifisering }
        assertUtbetalingsperiode(
            utbetalingsperioderPerKlasse.getValue("BATR")[0],
            0,
            null,
            TSS_ID_INSTITUSJON,
            1054,
            "2019-03-01",
            "2037-02-28",
        )
    }

    @Nested
    inner class SisteAndelIKjeden {

        val søker = tilfeldigPerson()

        lateinit var fagsak: Fagsak
        lateinit var førsteBehandling: Behandling
        lateinit var førsteVedtak: Vedtak
        lateinit var aktørSøker: Aktør

        val fom = årMnd("2019-04")
        val tom = årMnd("2019-05")
        val fom2 = årMnd("2019-06")
        val tom2 = årMnd("2020-05")

        @BeforeEach
        fun setUp() {
            fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søker.aktør.aktivFødselsnummer())
            førsteBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
            førsteVedtak = lagVedtak(behandling = førsteBehandling)
            aktørSøker = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)
        }

        @Test
        fun `skal hente siste andelene per ident og ytelsestype`() {
            with(lagInitiellTilkjentYtelse(førsteBehandling, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                val andeler = listOf(
                    lagAndel(this, fom, tom),
                    lagAndel(this, fom, tom, YtelseType.UTVIDET_BARNETRYGD, 1054),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }

            genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(førsteVedtak)
            avsluttOgLagreBehandling(førsteBehandling)

            val andreBehandling = opprettRevurdering()
            val andreVedtak = lagVedtak(behandling = andreBehandling)

            with(lagInitiellTilkjentYtelse(andreBehandling, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                val andeler = listOf(
                    lagAndel(this, fom, tom),
                    lagAndel(this, fom2, tom2),
                    lagAndel(this, fom, tom, YtelseType.UTVIDET_BARNETRYGD, 1054),
                    lagAndel(this, fom2, tom2, YtelseType.UTVIDET_BARNETRYGD, 1054),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }

            val utbetalingsoppdrag = genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(andreVedtak)
            assertThat(utbetalingsoppdrag.kodeEndring).isEqualTo(Utbetalingsoppdrag.KodeEndring.ENDR)
            assertThat(utbetalingsoppdrag.utbetalingsperiode).hasSize(2)
            assertThat(utbetalingsoppdrag.utbetalingsperiode.map { it.erEndringPåEksisterendePeriode })
                .containsOnly(false)
            with(utbetalingsoppdrag.utbetalingsperiode[0]) {
                assertThat(periodeId).isEqualTo(2)
                assertThat(forrigePeriodeId).isEqualTo(0)
                assertThat(sats.toInt()).isEqualTo(1345)
            }
            with(utbetalingsoppdrag.utbetalingsperiode[1]) {
                assertThat(periodeId).isEqualTo(3)
                assertThat(forrigePeriodeId).isEqualTo(1)
                assertThat(sats.toInt()).isEqualTo(1054)
            }
        }

        @Test
        fun `flere ytelestyper per person`() {
            val barn = tilfeldigPerson()
            val aktørBarn = personidentService.hentOgLagreAktør(barn.aktør.aktivFødselsnummer(), true)

            with(lagInitiellTilkjentYtelse(førsteBehandling, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                val andeler = listOf(
                    lagAndelTilkjentYtelse(
                        fom,
                        tom,
                        YtelseType.SMÅBARNSTILLEGG,
                        1,
                        behandling,
                        søker,
                        aktørSøker,
                        tilkjentYtelse = this,
                    ),
                    lagAndelTilkjentYtelse(
                        fom,
                        tom,
                        YtelseType.UTVIDET_BARNETRYGD,
                        2,
                        behandling,
                        søker,
                        aktørSøker,
                        tilkjentYtelse = this,
                    ),
                    lagAndelTilkjentYtelse(
                        fom,
                        tom,
                        YtelseType.ORDINÆR_BARNETRYGD,
                        3,
                        behandling,
                        barn,
                        aktørBarn,
                        tilkjentYtelse = this,
                    ),
                    lagAndelTilkjentYtelse(
                        fom,
                        tom,
                        YtelseType.UTVIDET_BARNETRYGD,
                        4,
                        behandling,
                        barn,
                        aktørBarn,
                        tilkjentYtelse = this,
                    ),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            val utbetalingsoppdrag = genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(førsteVedtak)
            avsluttOgLagreBehandling(førsteBehandling)
            assertThat(utbetalingsoppdrag.utbetalingsperiode).hasSize(4)
            assertThat(utbetalingsoppdrag.utbetalingsperiode.map { it.forrigePeriodeId })
                .`as`("Alle utbetalingsperioder skal peke mot null i forrigePeriodeId")
                .containsOnly(null)

            val revurdering = opprettRevurdering()

            with(lagInitiellTilkjentYtelse(revurdering, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                val andeler = listOf(
                    lagAndelTilkjentYtelse(
                        fom,
                        tom,
                        YtelseType.SMÅBARNSTILLEGG,
                        2,
                        revurdering,
                        søker,
                        aktørSøker,
                        tilkjentYtelse = this,
                    ),
                    lagAndelTilkjentYtelse(
                        fom,
                        tom,
                        YtelseType.UTVIDET_BARNETRYGD,
                        3,
                        revurdering,
                        søker,
                        aktørSøker,
                        tilkjentYtelse = this,
                    ),
                    lagAndelTilkjentYtelse(
                        fom,
                        tom,
                        YtelseType.ORDINÆR_BARNETRYGD,
                        4,
                        revurdering,
                        barn,
                        aktørBarn,
                        tilkjentYtelse = this,
                    ),
                    lagAndelTilkjentYtelse(
                        fom,
                        tom,
                        YtelseType.UTVIDET_BARNETRYGD,
                        5,
                        revurdering,
                        barn,
                        aktørBarn,
                        tilkjentYtelse = this,
                    ),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            val utbetalingsoppdrag2 = genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(lagVedtak(revurdering))
            val opphørsperioder = utbetalingsoppdrag2.utbetalingsperiode.filter { it.erEndringPåEksisterendePeriode }
            val nyePerioder = utbetalingsoppdrag2.utbetalingsperiode.filterNot { it.erEndringPåEksisterendePeriode }
            assertThat(opphørsperioder).hasSize(4)
            assertThat(nyePerioder).hasSize(4)
            assertUtbetalingsperiode(nyePerioder[0], 4, 0, aktørSøker, 2, fom, tom)
            assertUtbetalingsperiode(nyePerioder[1], 5, 1, aktørSøker, 3, fom, tom)
            assertUtbetalingsperiode(nyePerioder[2], 6, 2, aktørSøker, 4, fom, tom)
            assertUtbetalingsperiode(nyePerioder[3], 7, 3, aktørSøker, 5, fom, tom)
        }

        @Test
        fun `skal alltid peke til siste andelen i kjeden ved opphør, selv opphør etter opphør`() {
            fun assertHarKunOpphør(utbetalingsoppdrag: Utbetalingsoppdrag, opphørFom: YearMonth) {
                assertThat(utbetalingsoppdrag.kodeEndring).isEqualTo(Utbetalingsoppdrag.KodeEndring.ENDR)
                assertThat(utbetalingsoppdrag.utbetalingsperiode).hasSize(1)
                with(utbetalingsoppdrag.utbetalingsperiode[0]) {
                    assertThat(erEndringPåEksisterendePeriode).isTrue()
                    assertThat(opphør!!.opphørDatoFom).isEqualTo(opphørFom.atDay(1))
                    assertThat(periodeId).isEqualTo(1L)
                    assertThat(forrigePeriodeId).isEqualTo(0L)
                    assertThat(vedtakdatoFom).isEqualTo(fom2.atDay(1))
                    assertThat(vedtakdatoTom).isEqualTo(tom2.atEndOfMonth())
                    assertThat(sats.toInt()).isEqualTo(1345)
                }
            }

            with(lagInitiellTilkjentYtelse(førsteBehandling, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                val andeler = listOf(
                    lagAndel(this, fom = fom, tom = tom),
                    lagAndel(this, fom = fom2, tom = tom2),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }

            genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(lagVedtak(behandling = førsteBehandling))
            avsluttOgLagreBehandling(førsteBehandling)

            val andreBehandling = opprettRevurdering()

            with(lagInitiellTilkjentYtelse(andreBehandling, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                andelerTilkjentYtelse.add(lagAndel(this, fom = fom, tom = tom))
                tilkjentYtelseRepository.saveAndFlush(this)
            }

            val utbetalingsoppdrag = genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(lagVedtak(andreBehandling))
            assertHarKunOpphør(utbetalingsoppdrag, fom2)

            avsluttOgLagreBehandling(andreBehandling)
            val tredjeBehandling = opprettRevurdering()
            with(lagInitiellTilkjentYtelse(tredjeBehandling, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            with(genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(lagVedtak(behandling = tredjeBehandling))) {
                assertHarKunOpphør(this, fom)
            }
        }

        @Test
        fun `ny andel etter opphør skal peke til siste andelen`() {
            with(lagInitiellTilkjentYtelse(førsteBehandling, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                val andeler = listOf(
                    lagAndel(this, fom = fom, tom = tom),
                    lagAndel(this, fom = fom2, tom = tom2),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }

            genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(lagVedtak(behandling = førsteBehandling))
            avsluttOgLagreBehandling(førsteBehandling)

            val andreBehandling = opprettRevurdering()

            with(lagInitiellTilkjentYtelse(andreBehandling, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                andelerTilkjentYtelse.add(lagAndel(this, fom = fom, tom = tom))
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(lagVedtak(behandling = andreBehandling))

            avsluttOgLagreBehandling(andreBehandling)
            val tredjeBehandling = opprettRevurdering()
            with(lagInitiellTilkjentYtelse(tredjeBehandling, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                val andeler = listOf(
                    lagAndel(this, fom = fom, tom = tom),
                    lagAndel(this, fom = fom2, tom = tom2),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            with(genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(lagVedtak(behandling = tredjeBehandling))) {
                assertThat(kodeEndring).isEqualTo(Utbetalingsoppdrag.KodeEndring.ENDR)
                assertThat(utbetalingsperiode).hasSize(1)
                with(utbetalingsperiode[0]) {
                    assertThat(erEndringPåEksisterendePeriode).isFalse()
                    assertThat(opphør).isNull()
                    assertThat(periodeId).isEqualTo(2L)
                    assertThat(forrigePeriodeId).isEqualTo(1L)
                    assertThat(vedtakdatoFom).isEqualTo(fom2.atDay(1))
                    assertThat(vedtakdatoTom).isEqualTo(tom2.atEndOfMonth())
                    assertThat(sats.toInt()).isEqualTo(1345)
                }
            }
        }

        fun lagAndel(
            tilkjentYtelse: TilkjentYtelse,
            fom: YearMonth,
            tom: YearMonth,
            type: YtelseType = YtelseType.SMÅBARNSTILLEGG,
            beløp: Int = 1345,
            aktør: Aktør? = null,
            person: Person? = null,
        ): AndelTilkjentYtelse =
            lagAndelTilkjentYtelse(
                fom = fom,
                tom = tom,
                ytelseType = type,
                beløp = beløp,
                behandling = tilkjentYtelse.behandling,
                person = person ?: søker,
                aktør = aktør ?: aktørSøker,
                tilkjentYtelse = tilkjentYtelse,
            )

        private fun opprettRevurdering() = opprettRevurdering(fagsak)
    }

    private fun opprettRevurdering(fagsak: Fagsak) =
        behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(fagsak, behandlingType = BehandlingType.REVURDERING),
        )

    private fun genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(vedtak: Vedtak): Utbetalingsoppdrag {
        return utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
            vedtak,
            "Z123",
            AndelTilkjentYtelseForIverksettingFactory(),
        )
    }

    private fun avsluttOgLagreBehandling(behandling: Behandling) {
        behandling.status = BehandlingStatus.AVSLUTTET
        behandling.leggTilBehandlingStegTilstand(StegType.BEHANDLING_AVSLUTTET)
        behandlingHentOgPersisterService.lagreEllerOppdater(behandling, false)
    }

    private fun assertUtbetalingsperiode(
        utbetalingsperiode: Utbetalingsperiode,
        periodeId: Long,
        forrigePeriodeId: Long?,
        utbetalesTil: Aktør,
        sats: Int,
        fom: YearMonth,
        tom: YearMonth,
        opphørFom: LocalDate? = null,
    ) = assertUtbetalingsperiode(
        utbetalingsperiode,
        periodeId,
        forrigePeriodeId,
        utbetalesTil.aktivFødselsnummer(),
        sats,
        fom.atDay(1).toString(),
        tom.atEndOfMonth().toString(),
        opphørFom,
    )

    private fun assertUtbetalingsperiode(
        utbetalingsperiode: Utbetalingsperiode,
        periodeId: Long,
        forrigePeriodeId: Long?,
        utbetalesTil: String,
        sats: Int,
        fom: String,
        tom: String,
        opphørFom: LocalDate? = null,
    ) {
        assertEquals(periodeId, utbetalingsperiode.periodeId)
        assertEquals(forrigePeriodeId, utbetalingsperiode.forrigePeriodeId)
        assertEquals(sats, utbetalingsperiode.sats.toInt())
        assertEquals(dato(fom), utbetalingsperiode.vedtakdatoFom)
        assertEquals(dato(tom), utbetalingsperiode.vedtakdatoTom)
        if (opphørFom != null) {
            assertEquals(opphørFom, utbetalingsperiode.opphør?.opphørDatoFom)
        }
        assertEquals(utbetalesTil, utbetalingsperiode.utbetalesTil)
    }

    companion object {
        private const val TSS_ID_INSTITUSJON = "80000"
        private const val ORGNUMMER = "987654321"
    }
}
