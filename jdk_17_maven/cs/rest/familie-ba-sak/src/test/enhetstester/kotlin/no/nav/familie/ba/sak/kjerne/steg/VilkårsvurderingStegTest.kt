package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.tilPersonEnkel
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.TilpassKompetanserTilRegelverkService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class VilkårsvurderingStegTest {

    private val vilkårService: VilkårService = mockk()
    private val beregningService: BeregningService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk(relaxed = true)
    private val behandlingstemaService: BehandlingstemaService = mockk(relaxed = true)
    private val tilbakestillBehandlingService: TilbakestillBehandlingService = mockk()
    private val tilpassKompetanserTilRegelverkService: TilpassKompetanserTilRegelverkService = mockk()
    private val vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService = mockk()

    private val vilkårsvurderingSteg: VilkårsvurderingSteg = VilkårsvurderingSteg(
        behandlingHentOgPersisterService,
        behandlingstemaService,
        vilkårService,
        beregningService,
        persongrunnlagService,
        tilbakestillBehandlingService,
        tilpassKompetanserTilRegelverkService,
        vilkårsvurderingForNyBehandlingService,
    )

    val behandling = lagBehandling(
        behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
        årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
    )
    val søker = lagPerson(type = PersonType.SØKER, fødselsdato = LocalDate.of(1984, 1, 1))
    val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 1, 1))

    @BeforeEach
    fun setup() {
        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
            barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()),
        )
        every { tilbakestillBehandlingService.tilbakestillDataTilVilkårsvurderingssteg(behandling) } returns Unit
        every { beregningService.genererTilkjentYtelseFraVilkårsvurdering(any(), any()) } returns lagInitiellTilkjentYtelse(
            behandling,
        )

        every { tilpassKompetanserTilRegelverkService.tilpassKompetanserTilRegelverk(BehandlingId(behandling.id)) } just Runs
    }

    @Test
    fun `skal fortsette til neste steg når helmanuell migreringsbehandling har del bosted`() {
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            person = søker,
            resultat = Resultat.OPPFYLT,
            periodeFom = søker.fødselsdato,
            periodeTom = LocalDate.now(),
            lagFullstendigVilkårResultat = true,
            personType = PersonType.SØKER,
            erDeltBosted = false,
        )
        val barnPersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            person = barn,
            resultat = Resultat.OPPFYLT,
            periodeFom = barn.fødselsdato,
            periodeTom = LocalDate.now(),
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
            erDeltBosted = true,
        )
        vikårsvurdering.personResultater = setOf(søkerPersonResultat, barnPersonResultat)
        every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns vikårsvurdering

        assertDoesNotThrow { vilkårsvurderingSteg.utførStegOgAngiNeste(behandling, "") }
    }

    @Test
    fun `skal validere når regelverk er konsistent`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = LocalDate.now().minusMonths(2).withDayOfMonth(1))

        val behandling = lagBehandling()

        val vilkårsvurderingBygger = VilkårsvurderingBuilder<Måned>(behandling)
            .forPerson(søker, MånedTidspunkt.nå())
            .medVilkår("N>", Vilkår.BOSATT_I_RIKET, Vilkår.LOVLIG_OPPHOLD)
            .forPerson(barn1, barn1.fødselsdato.tilMånedTidspunkt())
            .medVilkår("+>", Vilkår.UNDER_18_ÅR, Vilkår.GIFT_PARTNERSKAP)
            .medVilkår("N>", Vilkår.BOSATT_I_RIKET, Vilkår.LOVLIG_OPPHOLD, Vilkår.BOR_MED_SØKER)
            .byggPerson()

        val vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering()
        val søkerOgBarnPåBehandling = listOf(søker.tilPersonEnkel(), barn1.tilPersonEnkel())

        every { vilkårService.hentVilkårsvurdering(behandling.id) } returns vilkårsvurdering
        every { persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(behandling.id) } returns søkerOgBarnPåBehandling

        assertDoesNotThrow { vilkårsvurderingSteg.preValiderSteg(behandling, null) }
    }

    @Test
    fun `validering skal feile når det er blanding av regelverk på vilkårene for barnet`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)

        val behandling = lagBehandling()

        val vilkårsvurderingBygger = VilkårsvurderingBuilder<Måned>(behandling)
            .forPerson(søker, MånedTidspunkt.nå())
            .medVilkår("EEEEEEEEEEEEE", Vilkår.BOSATT_I_RIKET, Vilkår.LOVLIG_OPPHOLD)
            .forPerson(barn1, MånedTidspunkt.nå())
            .medVilkår("+++++++++++++", Vilkår.UNDER_18_ÅR, Vilkår.GIFT_PARTNERSKAP)
            .medVilkår("   EEEENNNNEE", Vilkår.BOSATT_I_RIKET)
            .medVilkår("     EEENNEEE", Vilkår.LOVLIG_OPPHOLD)
            .medVilkår("NNNNNNNNNNEEE", Vilkår.BOR_MED_SØKER)
            .byggPerson()

        val vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering()
        val søkerOgBarnPåBehandling = listOf(søker.tilPersonEnkel(), barn1.tilPersonEnkel())

        every { vilkårService.hentVilkårsvurdering(behandling.id) } returns vilkårsvurdering
        every { persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(behandling.id) } returns søkerOgBarnPåBehandling

        val exception = assertThrows<FunksjonellFeil> { vilkårsvurderingSteg.preValiderSteg(behandling, null) }
        assertEquals(
            "Det er forskjellig regelverk for en eller flere perioder for søker eller barna",
            exception.message,
        )
    }
}
