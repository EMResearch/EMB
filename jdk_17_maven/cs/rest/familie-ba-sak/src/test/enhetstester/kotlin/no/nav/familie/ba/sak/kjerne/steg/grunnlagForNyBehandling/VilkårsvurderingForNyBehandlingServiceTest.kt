package no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.vilkårsvurdering.lagVilkårsvurderingMedOverstyrendeResultater
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingMetrics
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.reflect.full.declaredMemberProperties

class VilkårsvurderingForNyBehandlingServiceTest {

    private val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    private val behandlingService = mockk<BehandlingService>()
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val behandlingstemaService = mockk<BehandlingstemaService>()
    private val endretUtbetalingAndelService = mockk<EndretUtbetalingAndelService>()
    private val vilkårsvurderingMetrics = mockk<VilkårsvurderingMetrics>()
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()

    private lateinit var vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService

    @BeforeEach
    fun setUp() {
        vilkårsvurderingForNyBehandlingService = VilkårsvurderingForNyBehandlingService(
            vilkårsvurderingService = vilkårsvurderingService,
            behandlingService = behandlingService,
            persongrunnlagService = persongrunnlagService,
            behandlingstemaService = behandlingstemaService,
            endretUtbetalingAndelService = endretUtbetalingAndelService,
            vilkårsvurderingMetrics = vilkårsvurderingMetrics,
            andelerTilkjentYtelseRepository = andelTilkjentYtelseRepository,
        )
    }

    @Test
    fun `skal kopiere vilkårsvurdering fra forrige behandling ved satsendring - alle vilkår for alle personer er oppfylt`() {
        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN)
        val fagsak = Fagsak(aktør = søker.aktør)
        val behandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.SATSENDRING)
        val forrigeBehandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.SØKNAD)

        val forrigeVilkårsvurdering = lagVilkårsvurderingMedOverstyrendeResultater(
            søker = søker,
            barna = listOf(barn),
            behandling = forrigeBehandling,
            overstyrendeVilkårResultater = emptyMap(),
            id = 1,
        )
        val forventetNåværendeVilkårsvurdering = lagVilkårsvurderingMedOverstyrendeResultater(
            søker = søker,
            barna = listOf(barn),
            behandling = behandling,
            overstyrendeVilkårResultater = emptyMap(),
        )

        every { vilkårsvurderingService.hentAktivForBehandling(behandlingId = forrigeBehandling.id) } returns forrigeVilkårsvurdering

        val slot = slot<Vilkårsvurdering>()

        every { vilkårsvurderingService.lagreNyOgDeaktiverGammel(capture(slot)) } returnsArgument 0

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns lagTestPersonopplysningGrunnlag(
            forrigeBehandling.id,
            søker,
            barn,
        )

        every {
            endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(
                behandling,
                forrigeBehandling,
            )
        } just runs

        vilkårsvurderingForNyBehandlingService.opprettVilkårsvurderingUtenomHovedflyt(
            behandling = behandling,
            forrigeBehandlingSomErVedtatt = forrigeBehandling,
        )

        verify(exactly = 1) { vilkårsvurderingService.lagreNyOgDeaktiverGammel(any()) }

        validerKopiertVilkårsvurdering(slot.captured, forrigeVilkårsvurdering, forventetNåværendeVilkårsvurdering)
    }

    @Test
    fun `skal kopiere vilkårsvurdering fra forrige behandling ved satsendring - alle VilkårResultater er ikke oppfylt`() {
        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN)
        val fagsak = Fagsak(aktør = søker.aktør)
        val behandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.SATSENDRING)
        val forrigeBehandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.SØKNAD)

        val forrigeVilkårsvurdering = lagVilkårsvurderingMedOverstyrendeResultater(
            søker = søker,
            barna = listOf(barn),
            behandling = forrigeBehandling,
            overstyrendeVilkårResultater = mapOf(
                Pair(
                    barn.aktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.BOSATT_I_RIKET,
                            resultat = Resultat.OPPFYLT,
                            periodeTom = LocalDate.now().minusMonths(4),
                            behandlingId = forrigeBehandling.id,
                        ),
                        lagVilkårResultat(
                            vilkårType = Vilkår.BOSATT_I_RIKET,
                            resultat = Resultat.IKKE_OPPFYLT,
                            periodeFom = null,
                            periodeTom = null,
                            behandlingId = forrigeBehandling.id,
                        ),
                    ),
                ),
            ),
            id = 1,
        )
        val forventetNåværendeVilkårsvurdering = lagVilkårsvurderingMedOverstyrendeResultater(
            søker = søker,
            barna = listOf(barn),
            behandling = behandling,
            overstyrendeVilkårResultater = mapOf(
                Pair(
                    barn.aktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.BOSATT_I_RIKET,
                            resultat = Resultat.OPPFYLT,
                            periodeTom = LocalDate.now().minusMonths(4),
                            behandlingId = behandling.id,
                        ),
                    ),
                ),
            ),
        )

        every { vilkårsvurderingService.hentAktivForBehandling(behandlingId = forrigeBehandling.id) } returns forrigeVilkårsvurdering

        val slot = slot<Vilkårsvurdering>()

        every { vilkårsvurderingService.lagreNyOgDeaktiverGammel(capture(slot)) } returnsArgument 0

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns lagTestPersonopplysningGrunnlag(
            forrigeBehandling.id,
            søker,
            barn,
        )

        every {
            endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(
                behandling,
                forrigeBehandling,
            )
        } just runs

        vilkårsvurderingForNyBehandlingService.opprettVilkårsvurderingUtenomHovedflyt(
            behandling = behandling,
            forrigeBehandlingSomErVedtatt = forrigeBehandling,
        )

        verify(exactly = 1) { vilkårsvurderingService.lagreNyOgDeaktiverGammel(any()) }

        validerKopiertVilkårsvurdering(slot.captured, forrigeVilkårsvurdering, forventetNåværendeVilkårsvurdering)
    }

    @Test
    fun `skal kopiere vilkårsvurdering fra forrige behandling ved satsendring - ett barn har ikke oppfylt alle vilkår og har ingen tilkjent ytelse fra forrige behandling`() {
        val søker = lagPerson(type = PersonType.SØKER)
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)
        val barna = listOf(barn1, barn2)
        val fagsak = Fagsak(aktør = søker.aktør)
        val behandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.SATSENDRING)
        val forrigeBehandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.SØKNAD)

        val forrigeVilkårsvurdering = lagVilkårsvurderingMedOverstyrendeResultater(
            søker = søker,
            barna = barna,
            behandling = forrigeBehandling,
            overstyrendeVilkårResultater = mapOf(
                Pair(
                    barn1.aktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.BOSATT_I_RIKET,
                            resultat = Resultat.IKKE_OPPFYLT,
                            periodeFom = null,
                            periodeTom = null,
                            behandlingId = forrigeBehandling.id,
                        ),
                    ),
                ),
            ),
            id = 1,
        )
        val forventetNåværendeVilkårsvurdering = lagVilkårsvurderingMedOverstyrendeResultater(
            søker = søker,
            barna = listOf(barn2),
            behandling = behandling,
            overstyrendeVilkårResultater = emptyMap(),
        )

        every { vilkårsvurderingService.hentAktivForBehandling(behandlingId = forrigeBehandling.id) } returns forrigeVilkårsvurdering

        val slot = slot<Vilkårsvurdering>()

        every { vilkårsvurderingService.lagreNyOgDeaktiverGammel(capture(slot)) } returnsArgument 0

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns lagTestPersonopplysningGrunnlag(
            forrigeBehandling.id,
            søker,
            barn2,
        )

        every {
            endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(
                behandling,
                forrigeBehandling,
            )
        } just runs

        vilkårsvurderingForNyBehandlingService.opprettVilkårsvurderingUtenomHovedflyt(
            behandling = behandling,
            forrigeBehandlingSomErVedtatt = forrigeBehandling,
        )

        verify(exactly = 1) { vilkårsvurderingService.lagreNyOgDeaktiverGammel(any()) }

        validerKopiertVilkårsvurdering(slot.captured, forrigeVilkårsvurdering, forventetNåværendeVilkårsvurdering)
    }

    companion object {
        fun validerKopiertVilkårsvurdering(
            kopiertVilkårsvurdering: Vilkårsvurdering,
            forrigeVilkårsvurdering: Vilkårsvurdering,
            forventetNåværendeVilkårsvurdering: Vilkårsvurdering,
        ) {
            assertThat(kopiertVilkårsvurdering.id).isNotEqualTo(forrigeVilkårsvurdering.id)
            assertThat(kopiertVilkårsvurdering.behandling.id).isNotEqualTo(forrigeVilkårsvurdering.behandling.id)

            kopiertVilkårsvurdering.personResultater.forEach {
                assertThat(it.aktør).isEqualTo(forventetNåværendeVilkårsvurdering.personResultater.first { personResultat -> personResultat.aktør.aktivFødselsnummer() == it.aktør.aktivFødselsnummer() }.aktør)
            }

            assertThat(kopiertVilkårsvurdering.personResultater.flatMap { it.vilkårResultater }.size).isEqualTo(
                forventetNåværendeVilkårsvurdering.personResultater.flatMap { it.vilkårResultater }.size,
            )

            val kopierteOgForrigeVilkårResultaterGruppertEtterVilkårType =
                kopiertVilkårsvurdering.personResultater.fold(mutableListOf<Pair<List<VilkårResultat>, List<VilkårResultat>>>()) { acc, personResultat ->
                    val vilkårResultaterForrigeBehandlingForPerson =
                        forventetNåværendeVilkårsvurdering.personResultater.filter { it.aktør.aktivFødselsnummer() == personResultat.aktør.aktivFødselsnummer() }
                            .flatMap { it.vilkårResultater }
                    acc.addAll(
                        personResultat.vilkårResultater.groupBy { it.vilkårType }
                            .map { (vilkårType, vilkårResultater) ->
                                Pair(
                                    vilkårResultater,
                                    vilkårResultaterForrigeBehandlingForPerson.filter { forrigeVilkårResultat -> forrigeVilkårResultat.vilkårType == vilkårType },
                                )
                            },
                    )
                    acc
                }

            val baseEntitetFelter =
                BaseEntitet::class.declaredMemberProperties.map { it.name }.toTypedArray()
            kopierteOgForrigeVilkårResultaterGruppertEtterVilkårType.forEach {
                assertThat(it.first).usingRecursiveFieldByFieldElementComparatorIgnoringFields(
                    "id",
                    "personResultat",
                    *baseEntitetFelter,
                )
                    .isEqualTo(it.second)
            }
        }
    }
}
