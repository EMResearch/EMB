package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler

import io.mockk.CapturingSlot
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import no.nav.familie.ba.sak.datagenerator.vilkårsvurdering.lagVilkårsvurderingMedOverstyrendeResultater
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.VergeResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene.FødselshendelsefiltreringResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene.FødselshendelsefiltreringResultatRepository
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene.erOppfylt
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
class FiltreringsreglerServiceTest {

    @MockK
    private lateinit var personopplysningerService: PersonopplysningerService

    @MockK
    private lateinit var personidentService: PersonidentService

    @MockK
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @MockK
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @MockK
    private lateinit var localDateService: LocalDateService

    @MockK
    private lateinit var fødselshendelsefiltreringResultatRepository: FødselshendelsefiltreringResultatRepository

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var behandlingHentOgPersisterService: BehandlingHentOgPersisterService

    @MockK
    private lateinit var tilkjentYtelseValideringService: TilkjentYtelseValideringService

    @InjectMockKs
    private lateinit var filtreringsreglerService: FiltreringsreglerService

    @MockK
    private lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @Test
    fun `kjørFiltreringsregler - skal gi resultat ikke oppfylt når mors vilkår om utvidet barnetrygd er oppfylt i tidsrommet barnet er mellom 0 og 18`() {
        val mor = tilfeldigSøker(fødselsdato = LocalDate.of(1985, 1, 1))
        val barn = tilfeldigPerson(fødselsdato = LocalDate.of(2021, 1, 1))
        val nyBehandlingHendelse = NyBehandlingHendelse(mor.aktør.aktørId, listOf(barn.aktør.aktørId))
        val sisteVedtatteBehandling = lagBehandling()
        val behandling = lagBehandling()

        val fødselshendelsefiltreringResultatSlot =
            settOppMocksHvorAlleFiltreringsreglerBlirOppfylt(mor, listOf(barn), behandling, sisteVedtatteBehandling)

        clearMocks(vilkårsvurderingRepository)
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(sisteVedtatteBehandling.id) } returns lagVilkårsvurderingMedOverstyrendeResultater(
            mor,
            listOf(barn),
            behandling,
            overstyrendeVilkårResultater = mapOf(
                Pair(
                    mor.aktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            periodeFom = LocalDate.of(2020, 11, 1),
                            periodeTom = LocalDate.of(2021, 2, 1),
                        ),
                    ),
                ),
            ),
        )

        mockkObject(FiltreringsregelEvaluering)
        val filtreringsreglerFaktaSlot = slot<FiltreringsreglerFakta>()

        filtreringsreglerService.kjørFiltreringsregler(nyBehandlingHendelse, behandling)

        verify { FiltreringsregelEvaluering.evaluerFiltreringsregler(capture(filtreringsreglerFaktaSlot)) }

        val fødselshendelsefiltreringResultat = fødselshendelsefiltreringResultatSlot.captured
        val filtreringsreglerFakta = filtreringsreglerFaktaSlot.captured

        assertThat(filtreringsreglerFakta.morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato).isTrue

        assertThat(fødselshendelsefiltreringResultat.single { it.resultat == Resultat.IKKE_OPPFYLT }.filtreringsregel).isEqualTo(
            Filtreringsregel.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO,
        )
        assertThat(fødselshendelsefiltreringResultat.erOppfylt()).isFalse
    }

    @Test
    fun `kjørFiltreringsregler - skal gi resultat oppfylt når mors vilkår om utvidet barnetrygd er oppfylt utenfor tidsrommet barnet er mellom 0 og 18`() {
        val mor = tilfeldigSøker(fødselsdato = LocalDate.of(1985, 1, 1))
        val barn = tilfeldigPerson(fødselsdato = LocalDate.of(2021, 1, 1))
        val nyBehandlingHendelse = NyBehandlingHendelse(mor.aktør.aktørId, listOf(barn.aktør.aktørId))
        val behandling = lagBehandling()
        val sisteVedtatteBehandling = lagBehandling()

        val fødselshendelsefiltreringResultatSlot =
            settOppMocksHvorAlleFiltreringsreglerBlirOppfylt(mor, listOf(barn), behandling, sisteVedtatteBehandling)

        clearMocks(vilkårsvurderingRepository)
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(sisteVedtatteBehandling.id) } returns lagVilkårsvurderingMedOverstyrendeResultater(
            mor,
            listOf(barn),
            behandling,
            overstyrendeVilkårResultater = mapOf(
                Pair(
                    mor.aktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            periodeFom = LocalDate.of(2020, 11, 1),
                            periodeTom = LocalDate.of(2021, 1, 1),
                        ),
                    ),
                ),
            ),
        )

        mockkObject(FiltreringsregelEvaluering)
        val filtreringsreglerFaktaSlot = slot<FiltreringsreglerFakta>()

        filtreringsreglerService.kjørFiltreringsregler(nyBehandlingHendelse, behandling)

        verify { FiltreringsregelEvaluering.evaluerFiltreringsregler(capture(filtreringsreglerFaktaSlot)) }

        val fødselshendelsefiltreringResultat = fødselshendelsefiltreringResultatSlot.captured
        val filtreringsreglerFakta = filtreringsreglerFaktaSlot.captured

        assertThat(filtreringsreglerFakta.morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato).isFalse

        assertThat(fødselshendelsefiltreringResultat.single { it.filtreringsregel == Filtreringsregel.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO }.resultat).isEqualTo(
            Resultat.OPPFYLT,
        )
        assertThat(fødselshendelsefiltreringResultat.erOppfylt()).isTrue
    }

    @Test
    fun `kjørFiltreringsregler - skal gi resultat ikke oppfylt når en vilkårsperiode for utvidet barnetrygd er oppfylt i tidsrommet barnet er mellom 0 og 18`() {
        val mor = tilfeldigSøker(fødselsdato = LocalDate.of(1985, 1, 1))
        val barn = tilfeldigPerson(fødselsdato = LocalDate.of(2021, 1, 1))
        val nyBehandlingHendelse = NyBehandlingHendelse(mor.aktør.aktørId, listOf(barn.aktør.aktørId))
        val behandling = lagBehandling()
        val sisteVedtatteBehandling = lagBehandling()

        val fødselshendelsefiltreringResultatSlot =
            settOppMocksHvorAlleFiltreringsreglerBlirOppfylt(mor, listOf(barn), behandling, sisteVedtatteBehandling)

        clearMocks(vilkårsvurderingRepository)
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(sisteVedtatteBehandling.id) } returns lagVilkårsvurderingMedOverstyrendeResultater(
            mor,
            listOf(barn),
            behandling,
            overstyrendeVilkårResultater = mapOf(
                Pair(
                    mor.aktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            periodeFom = LocalDate.of(2020, 11, 1),
                            periodeTom = LocalDate.of(2021, 1, 1),
                        ),
                        lagVilkårResultat(
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            periodeFom = LocalDate.of(2022, 1, 1),
                            periodeTom = LocalDate.of(2023, 1, 1),
                        ),
                    ),
                ),
            ),
        )

        mockkObject(FiltreringsregelEvaluering)
        val filtreringsreglerFaktaSlot = slot<FiltreringsreglerFakta>()

        filtreringsreglerService.kjørFiltreringsregler(nyBehandlingHendelse, behandling)

        verify { FiltreringsregelEvaluering.evaluerFiltreringsregler(capture(filtreringsreglerFaktaSlot)) }

        val fødselshendelsefiltreringResultat = fødselshendelsefiltreringResultatSlot.captured
        val filtreringsreglerFakta = filtreringsreglerFaktaSlot.captured

        assertThat(filtreringsreglerFakta.morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato).isTrue

        assertThat(fødselshendelsefiltreringResultat.single { it.resultat == Resultat.IKKE_OPPFYLT }.filtreringsregel).isEqualTo(
            Filtreringsregel.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO,
        )
        assertThat(fødselshendelsefiltreringResultat.erOppfylt()).isFalse
    }

    @Test
    fun `kjørFiltreringsregler - skal gi resultat ikke oppfylt når tom-dato er null på vilkåret utvidet barnetrygd`() {
        val mor = tilfeldigSøker(fødselsdato = LocalDate.of(1985, 1, 1))
        val barn = tilfeldigPerson(fødselsdato = LocalDate.of(2021, 1, 1))
        val nyBehandlingHendelse = NyBehandlingHendelse(mor.aktør.aktørId, listOf(barn.aktør.aktørId))
        val behandling = lagBehandling()
        val sisteVedtatteBehandling = lagBehandling()

        val fødselshendelsefiltreringResultatSlot =
            settOppMocksHvorAlleFiltreringsreglerBlirOppfylt(mor, listOf(barn), behandling, sisteVedtatteBehandling)

        clearMocks(vilkårsvurderingRepository)
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(sisteVedtatteBehandling.id) } returns lagVilkårsvurderingMedOverstyrendeResultater(
            mor,
            listOf(barn),
            behandling,
            overstyrendeVilkårResultater = mapOf(
                Pair(
                    mor.aktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            periodeFom = LocalDate.of(2020, 11, 1),
                            periodeTom = LocalDate.of(2021, 1, 1),
                        ),
                        lagVilkårResultat(
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            periodeFom = LocalDate.of(2022, 1, 1),
                            periodeTom = null,
                        ),
                    ),
                ),
            ),
        )

        mockkObject(FiltreringsregelEvaluering)
        val filtreringsreglerFaktaSlot = slot<FiltreringsreglerFakta>()

        filtreringsreglerService.kjørFiltreringsregler(nyBehandlingHendelse, behandling)

        verify { FiltreringsregelEvaluering.evaluerFiltreringsregler(capture(filtreringsreglerFaktaSlot)) }

        val fødselshendelsefiltreringResultat = fødselshendelsefiltreringResultatSlot.captured
        val filtreringsreglerFakta = filtreringsreglerFaktaSlot.captured

        assertThat(filtreringsreglerFakta.morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato).isTrue

        assertThat(fødselshendelsefiltreringResultat.single { it.resultat == Resultat.IKKE_OPPFYLT }.filtreringsregel).isEqualTo(
            Filtreringsregel.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO,
        )
        assertThat(fødselshendelsefiltreringResultat.erOppfylt()).isFalse
    }

    @Test
    fun `kjørFiltreringsregler - skal gi resultat oppfylt når begge barnas fødselsdatoer er etter tom på vilkåret utvidet barnetrygd`() {
        val mor = tilfeldigSøker(fødselsdato = LocalDate.of(1985, 1, 1))
        val barn1 = tilfeldigPerson(fødselsdato = LocalDate.of(2021, 1, 1))
        val barn2 = tilfeldigPerson(fødselsdato = LocalDate.of(2020, 1, 1))

        val nyBehandlingHendelse =
            NyBehandlingHendelse(mor.aktør.aktørId, listOf(barn1.aktør.aktørId, barn2.aktør.aktørId))
        val behandling = lagBehandling()
        val sisteVedtatteBehandling = lagBehandling()

        val fødselshendelsefiltreringResultatSlot =
            settOppMocksHvorAlleFiltreringsreglerBlirOppfylt(
                mor,
                listOf(barn1, barn2),
                behandling,
                sisteVedtatteBehandling,
            )

        clearMocks(vilkårsvurderingRepository)
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(sisteVedtatteBehandling.id) } returns lagVilkårsvurderingMedOverstyrendeResultater(
            mor,
            listOf(barn1, barn2),
            behandling,
            overstyrendeVilkårResultater = mapOf(
                Pair(
                    mor.aktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            periodeFom = LocalDate.of(2019, 11, 1),
                            periodeTom = LocalDate.of(2020, 1, 1),
                        ),
                    ),
                ),
            ),
        )

        mockkObject(FiltreringsregelEvaluering)
        val filtreringsreglerFaktaSlot = slot<FiltreringsreglerFakta>()

        filtreringsreglerService.kjørFiltreringsregler(nyBehandlingHendelse, behandling)

        verify { FiltreringsregelEvaluering.evaluerFiltreringsregler(capture(filtreringsreglerFaktaSlot)) }

        val fødselshendelsefiltreringResultat = fødselshendelsefiltreringResultatSlot.captured
        val filtreringsreglerFakta = filtreringsreglerFaktaSlot.captured

        assertThat(filtreringsreglerFakta.morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato).isFalse

        assertThat(fødselshendelsefiltreringResultat.single { it.filtreringsregel == Filtreringsregel.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO }.resultat).isEqualTo(
            Resultat.OPPFYLT,
        )
        assertThat(fødselshendelsefiltreringResultat.erOppfylt()).isTrue
    }

    @Test
    fun `kjørFiltreringsregler - skal gi resultat ikke oppfylt når mors vilkår om utvidet barnetrygd er oppfylt i tidsrommet et av barna er mellom 0 og 18`() {
        val mor = tilfeldigSøker(fødselsdato = LocalDate.of(1985, 1, 1))
        val barn1 = tilfeldigPerson(fødselsdato = LocalDate.of(2021, 1, 1))
        val barn2 = tilfeldigPerson(fødselsdato = LocalDate.of(2020, 1, 1))

        val nyBehandlingHendelse =
            NyBehandlingHendelse(mor.aktør.aktørId, listOf(barn1.aktør.aktørId, barn2.aktør.aktørId))
        val behandling = lagBehandling()
        val sisteVedtatteBehandling = lagBehandling()

        val fødselshendelsefiltreringResultatSlot =
            settOppMocksHvorAlleFiltreringsreglerBlirOppfylt(
                mor,
                listOf(barn1, barn2),
                behandling,
                sisteVedtatteBehandling,
            )

        clearMocks(vilkårsvurderingRepository)
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(sisteVedtatteBehandling.id) } returns lagVilkårsvurderingMedOverstyrendeResultater(
            mor,
            listOf(barn1, barn2),
            behandling,
            overstyrendeVilkårResultater = mapOf(
                Pair(
                    mor.aktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            periodeFom = LocalDate.of(2019, 11, 1),
                            periodeTom = LocalDate.of(2021, 1, 1),
                        ),
                    ),
                ),
            ),
        )

        mockkObject(FiltreringsregelEvaluering)
        val filtreringsreglerFaktaSlot = slot<FiltreringsreglerFakta>()

        filtreringsreglerService.kjørFiltreringsregler(nyBehandlingHendelse, behandling)

        verify { FiltreringsregelEvaluering.evaluerFiltreringsregler(capture(filtreringsreglerFaktaSlot)) }

        val fødselshendelsefiltreringResultat = fødselshendelsefiltreringResultatSlot.captured
        val filtreringsreglerFakta = filtreringsreglerFaktaSlot.captured

        assertThat(filtreringsreglerFakta.morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato).isTrue

        assertThat(fødselshendelsefiltreringResultat.single { it.resultat == Resultat.IKKE_OPPFYLT }.filtreringsregel).isEqualTo(
            Filtreringsregel.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO,
        )
        assertThat(fødselshendelsefiltreringResultat.erOppfylt()).isFalse
    }

    @Test
    fun `kjørFiltreringsregler - skal gi resultat ikke oppfylt når mor har opphørt barnetrygd`() {
        val mor = tilfeldigSøker(fødselsdato = LocalDate.of(1985, 1, 1))
        val barn = tilfeldigPerson(fødselsdato = LocalDate.of(2021, 1, 1))
        val nyBehandlingHendelse = NyBehandlingHendelse(mor.aktør.aktørId, listOf(barn.aktør.aktørId))
        val sisteVedtatteBehandling = lagBehandling()
        val behandling = lagBehandling()

        val fødselshendelsefiltreringResultatSlot =
            settOppMocksHvorAlleFiltreringsreglerBlirOppfylt(mor, listOf(barn), behandling, sisteVedtatteBehandling)

        clearMocks(andelTilkjentYtelseRepository)
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteVedtatteBehandling.id) } returns listOf(
            MånedPeriode(YearMonth.of(2018, 1), YearMonth.now()),
        )
            .map {
                lagAndelTilkjentYtelse(it.fom, it.tom)
            }

        mockkObject(FiltreringsregelEvaluering)
        val filtreringsreglerFaktaSlot = slot<FiltreringsreglerFakta>()

        filtreringsreglerService.kjørFiltreringsregler(nyBehandlingHendelse, behandling)

        verify { FiltreringsregelEvaluering.evaluerFiltreringsregler(capture(filtreringsreglerFaktaSlot)) }

        val fødselshendelsefiltreringResultat = fødselshendelsefiltreringResultatSlot.captured
        val filtreringsreglerFakta = filtreringsreglerFaktaSlot.captured

        assertThat(filtreringsreglerFakta.morHarIkkeOpphørtBarnetrygd).isFalse

        assertThat(fødselshendelsefiltreringResultat.single { it.resultat == Resultat.IKKE_OPPFYLT }.filtreringsregel).isEqualTo(
            Filtreringsregel.MOR_HAR_IKKE_OPPHØRT_BARNETRYGD,
        )
        assertThat(fødselshendelsefiltreringResultat.erOppfylt()).isFalse
    }

    @Test
    fun `kjørFiltreringsregler - skal gi resultat oppfylt når vilkår er oppfylt og mor ikke har opphørt barnetrygd`() {
        val mor = tilfeldigSøker(fødselsdato = LocalDate.of(1985, 1, 1))
        val barn = tilfeldigPerson(fødselsdato = LocalDate.of(2021, 1, 1))
        val nyBehandlingHendelse = NyBehandlingHendelse(mor.aktør.aktørId, listOf(barn.aktør.aktørId))
        val sisteVedtatteBehandling = lagBehandling()
        val behandling = lagBehandling()

        val fødselshendelsefiltreringResultatSlot =
            settOppMocksHvorAlleFiltreringsreglerBlirOppfylt(mor, listOf(barn), behandling, sisteVedtatteBehandling)

        clearMocks(andelTilkjentYtelseRepository)
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteVedtatteBehandling.id) } returns emptyList()

        mockkObject(FiltreringsregelEvaluering)
        val filtreringsreglerFaktaSlot = slot<FiltreringsreglerFakta>()

        filtreringsreglerService.kjørFiltreringsregler(nyBehandlingHendelse, behandling)

        verify { FiltreringsregelEvaluering.evaluerFiltreringsregler(capture(filtreringsreglerFaktaSlot)) }

        val fødselshendelsefiltreringResultat = fødselshendelsefiltreringResultatSlot.captured
        val filtreringsreglerFakta = filtreringsreglerFaktaSlot.captured

        assertThat(filtreringsreglerFakta.morHarIkkeOpphørtBarnetrygd).isTrue

        assertThat(fødselshendelsefiltreringResultat.single { it.filtreringsregel == Filtreringsregel.MOR_HAR_IKKE_OPPHØRT_BARNETRYGD }.resultat).isEqualTo(
            Resultat.OPPFYLT,
        )
        assertThat(fødselshendelsefiltreringResultat.erOppfylt()).isTrue
    }

    private fun settOppMocksHvorAlleFiltreringsreglerBlirOppfylt(
        mor: Person,
        barna: List<Person>,
        behandling: Behandling,
        sisteVedtatteBehandling: Behandling,
    ): CapturingSlot<List<FødselshendelsefiltreringResultat>> {
        every { personidentService.hentAktør(mor.aktør.aktørId) } returns mor.aktør
        every { personidentService.hentAktørIder(barna.map { it.aktør.aktørId }) } returns barna.map { it.aktør }

        every { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns lagTestPersonopplysningGrunnlag(
            behandling.id,
            mor,
            *barna.toTypedArray(),
        )
        every { behandlingService.hentMigreringsdatoPåFagsak(behandling.fagsak.id) } returns null
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns sisteVedtatteBehandling
        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(sisteVedtatteBehandling.id) } returns lagVilkårsvurderingMedOverstyrendeResultater(
            mor,
            barna,
            behandling,
            overstyrendeVilkårResultater = mapOf(
                Pair(
                    mor.aktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                            periodeFom = barna.minOf { it.fødselsdato }.minusYears(1),
                            periodeTom = barna.minOf { it.fødselsdato }.minusYears(1).plusMonths(6),
                        ),
                    ),
                ),
            ),
        )

        every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(mor.aktør) } returns PersonInfo(
            forelderBarnRelasjon =
            barna.map {
                ForelderBarnRelasjon(
                    aktør = it.aktør,
                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                )
            }.toSet(),
            fødselsdato = mor.fødselsdato,
        )

        every { personopplysningerService.harVerge(mor.aktør) } returns VergeResponse(false)

        every { localDateService.now() } returns LocalDate.now()

        every {
            tilkjentYtelseValideringService.barnetrygdLøperForAnnenForelder(
                behandling,
                barna,
            )
        } returns false

        val andelTilkjentytelse = listOf(
            MånedPeriode(YearMonth.of(2018, 1), YearMonth.now().plusYears(1)),
        )
            .map {
                lagAndelTilkjentYtelse(it.fom, it.tom)
            }
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns andelTilkjentytelse

        val fødselshendelsefiltreringResultatSlot = slot<List<FødselshendelsefiltreringResultat>>()

        every {
            fødselshendelsefiltreringResultatRepository.saveAll<FødselshendelsefiltreringResultat>(
                capture(
                    fødselshendelsefiltreringResultatSlot,
                ),
            )
        } returns mockk()
        return fødselshendelsefiltreringResultatSlot
    }
}
