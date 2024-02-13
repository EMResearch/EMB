package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPersonResultaterForSøkerOgToBarn
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilPersonEnkelSøkerOgBarn
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class VilkårsvurderingTidslinjerTest {

    @Test
    fun `et vilkår kan ha overlappende vilkårsresultater hvis bare ett er oppfylt`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        val barn2Fnr = randomFnr()
        val barnaFnr = listOf(barnFnr, barn2Fnr)

        val defaultBehandling = lagBehandling(defaultFagsak())
        val vilkårsvurdering = Vilkårsvurdering(
            behandling = defaultBehandling,
        ).also {
            it.personResultater = lagPersonResultaterForSøkerOgToBarn(
                it,
                tilAktør(søkerFnr),
                tilAktør(barnFnr),
                tilAktør(barn2Fnr),
                LocalDate.now().minusMonths(3),
                LocalDate.now().minusMonths(2),
            )
        }

        // Legg på et overlappende vilkårsresultat som IKKE er oppfylt
        vilkårsvurdering.personResultater.filter { it.aktør.aktivFødselsnummer() == barnFnr }.forEach {
            it.vilkårResultater.add(
                lagVilkårResultat(
                    id = 1000,
                    personResultat = it,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    behandlingId = defaultBehandling.id,
                    periodeFom = null, // uendelig lenge siden
                    periodeTom = null, // uendelig lenge til
                    resultat = Resultat.IKKE_OPPFYLT,
                ),
            )
        }

        assertDoesNotThrow {
            VilkårsvurderingTidslinjer(
                vilkårsvurdering = vilkårsvurdering,
                søkerOgBarn = lagTestPersonopplysningGrunnlag(defaultBehandling.id, søkerFnr, barnaFnr)
                    .tilPersonEnkelSøkerOgBarn(),
            )
        }
    }

    @Test
    fun `kan ikke ha to overlappende vilkårsresultater hvis begge er oppfylt`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        val barn2Fnr = randomFnr()
        val barnaFnr = listOf(barnFnr, barn2Fnr)

        val defaultBehandling = lagBehandling(defaultFagsak())
        val vilkårsvurdering = Vilkårsvurdering(
            behandling = defaultBehandling,
        ).also {
            it.personResultater = lagPersonResultaterForSøkerOgToBarn(
                it,
                tilAktør(søkerFnr),
                tilAktør(barnFnr),
                tilAktør(barn2Fnr),
                LocalDate.now().minusMonths(3),
                LocalDate.now().minusMonths(2),
            )
        }

        // Legg på et overlappende vilkårsresultat som ER oppfylt
        vilkårsvurdering.personResultater.filter { it.aktør.aktivFødselsnummer() == barnFnr }.forEach {
            it.vilkårResultater.add(
                lagVilkårResultat(
                    id = 500,
                    personResultat = it,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    behandlingId = defaultBehandling.id,
                    periodeTom = null,
                    resultat = Resultat.OPPFYLT,
                ),
            )
        }

        assertThrows<Tidslinje.Companion.TidslinjeFeilException> {
            VilkårsvurderingTidslinjer(
                vilkårsvurdering = vilkårsvurdering,
                søkerOgBarn = lagTestPersonopplysningGrunnlag(defaultBehandling.id, søkerFnr, barnaFnr)
                    .tilPersonEnkelSøkerOgBarn(),
            )
        }
    }

    private fun lagVilkårResultat(
        id: Long = 0,
        personResultat: PersonResultat? = null,
        vilkårType: Vilkår = Vilkår.BOSATT_I_RIKET,
        resultat: Resultat = Resultat.OPPFYLT,
        periodeFom: LocalDate? = LocalDate.of(2009, 12, 24),
        periodeTom: LocalDate? = LocalDate.of(2010, 1, 31),
        begrunnelse: String = "",
        behandlingId: Long = lagBehandling().id,
        utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList(),
    ) = VilkårResultat(
        id = id,
        personResultat = personResultat,
        vilkårType = vilkårType,
        resultat = resultat,
        periodeFom = periodeFom,
        periodeTom = periodeTom,
        begrunnelse = begrunnelse,
        sistEndretIBehandlingId = behandlingId,
        utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger,
    )
}
