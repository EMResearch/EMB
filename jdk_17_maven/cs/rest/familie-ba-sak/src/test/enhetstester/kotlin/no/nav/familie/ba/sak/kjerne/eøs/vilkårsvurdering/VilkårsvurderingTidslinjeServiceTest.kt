import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.vilkårsvurdering.lagVilkårsvurderingMedOverstyrendeResultater
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjeService
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.simulering.lagBehandling
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.erTom
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilAnnenForelderOmfattetAvNorskLovgivningTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class VilkårsvurderingTidslinjeServiceTest {
    val persongrunnlagService = mockk<PersongrunnlagService>()
    val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()

    private lateinit var vilkårsvurderingTidslinjeService: VilkårsvurderingTidslinjeService

    @BeforeEach
    fun setUp() {
        vilkårsvurderingTidslinjeService = VilkårsvurderingTidslinjeService(
            vilkårsvurderingRepository = vilkårsvurderingRepository,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
        )
    }

    @Test
    fun `skal forskyve fom med 1 mnd for periode med erAnnenForelderOmfattetAvNorskLovgivning`() {
        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN)
        val fagsak = Fagsak(aktør = søker.aktør)
        val behandling = lagBehandling(fagsak = fagsak, behandlingKategori = BehandlingKategori.EØS)
        val vilkårsvurdering = lagVilkårsvurderingMedOverstyrendeResultater(
            søker = søker,
            barna = listOf(barn),
            overstyrendeVilkårResultater = mapOf(
                Pair(
                    søker.aktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.BOSATT_I_RIKET,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.of(2023, 1, 2),
                            periodeTom = LocalDate.of(2023, 3, 4),
                            behandlingId = behandling.id,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING),
                        ),
                    ),
                ),
            ),
        )
        every { persongrunnlagService.hentAktivThrows(behandlingId = behandling.id) } returns lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
            barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()),
        )
        every { vilkårsvurderingService.hentAktivForBehandlingThrows(behandlingId = behandling.id) } returns vilkårsvurdering

        val faktiskTidslinje = vilkårsvurderingTidslinjeService.hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(
            behandlingId = BehandlingId(behandling.id),
        )
        val forventetTidslinje = "++".tilAnnenForelderOmfattetAvNorskLovgivningTidslinje(feb(2023))
        assertThat(faktiskTidslinje).isEqualTo(forventetTidslinje)
    }

    @Test
    fun `skal ikke gi noen oppfylte perioder hvis vilkår kun oppfylt innenfor én måned`() {
        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN)
        val fagsak = Fagsak(aktør = søker.aktør)
        val behandling = lagBehandling(fagsak = fagsak, behandlingKategori = BehandlingKategori.EØS)
        val vilkårsvurdering = lagVilkårsvurderingMedOverstyrendeResultater(
            søker = søker,
            barna = listOf(barn),
            overstyrendeVilkårResultater = mapOf(
                Pair(
                    søker.aktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.BOSATT_I_RIKET,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.of(2021, 12, 1),
                            periodeTom = LocalDate.of(2021, 12, 31),
                            behandlingId = behandling.id,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING),
                        ),
                    ),
                ),
            ),
        )
        every { persongrunnlagService.hentAktivThrows(behandlingId = behandling.id) } returns lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
            barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()),
        )
        every { vilkårsvurderingService.hentAktivForBehandlingThrows(behandlingId = behandling.id) } returns vilkårsvurdering

        val faktiskTidslinje = vilkårsvurderingTidslinjeService.hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(
            behandlingId = BehandlingId(behandling.id),
        )

        assertThat(faktiskTidslinje.erTom()).isTrue
    }

    @Test
    fun `skal forskyve fom med 1 mnd for flere perioder med erAnnenForelderOmfattetAvNorskLovgivning`() {
        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN)
        val fagsak = Fagsak(aktør = søker.aktør)
        val behandling = lagBehandling(fagsak = fagsak, behandlingKategori = BehandlingKategori.EØS)
        val vilkårsvurdering = lagVilkårsvurderingMedOverstyrendeResultater(
            søker = søker,
            barna = listOf(barn),
            overstyrendeVilkårResultater = mapOf(
                Pair(
                    søker.aktør.aktørId,
                    listOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.BOSATT_I_RIKET,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.of(2023, 1, 2),
                            periodeTom = LocalDate.of(2023, 3, 4),
                            behandlingId = behandling.id,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING),
                        ),
                        lagVilkårResultat(
                            vilkårType = Vilkår.BOSATT_I_RIKET,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.of(2023, 4, 30),
                            periodeTom = LocalDate.of(2023, 7, 1),
                            behandlingId = behandling.id,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING),
                        ),
                    ),
                ),
            ),
        )
        every { persongrunnlagService.hentAktivThrows(behandlingId = behandling.id) } returns lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
            barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()),
        )
        every { vilkårsvurderingService.hentAktivForBehandlingThrows(behandlingId = behandling.id) } returns vilkårsvurdering

        val faktiskTidslinje = vilkårsvurderingTidslinjeService.hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(
            behandlingId = BehandlingId(behandling.id),
        )
        val forventetTidslinje = "++ +++".tilAnnenForelderOmfattetAvNorskLovgivningTidslinje(feb(2023)).filtrerIkkeNull()
        assertThat(faktiskTidslinje).isEqualTo(forventetTidslinje)
    }
}
