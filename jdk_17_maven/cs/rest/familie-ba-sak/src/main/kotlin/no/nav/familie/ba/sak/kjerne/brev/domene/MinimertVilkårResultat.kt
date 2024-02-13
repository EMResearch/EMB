package no.nav.familie.ba.sak.kjerne.brev.domene

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.lagOgValiderPeriodeFraVilkår
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelseDeserializer
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import java.time.LocalDate

data class MinimertVilkårResultat(
    val vilkårType: Vilkår,
    val periodeFom: LocalDate?,
    val periodeTom: LocalDate?,
    val resultat: Resultat,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering>,
    val erEksplisittAvslagPåSøknad: Boolean?,
    @JsonDeserialize(using = IVedtakBegrunnelseDeserializer::class)
    val standardbegrunnelser: List<IVedtakBegrunnelse>,
) {

    fun toPeriode(): Periode = lagOgValiderPeriodeFraVilkår(
        this.periodeFom,
        this.periodeTom,
        this.erEksplisittAvslagPåSøknad,
    )
}

fun VilkårResultat.tilMinimertVilkårResultat() =
    MinimertVilkårResultat(
        vilkårType = this.vilkårType,
        periodeFom = this.periodeFom,
        periodeTom = this.periodeTom,
        resultat = this.resultat,
        utdypendeVilkårsvurderinger = this.utdypendeVilkårsvurderinger,
        erEksplisittAvslagPåSøknad = this.erEksplisittAvslagPåSøknad,
        standardbegrunnelser = this.standardbegrunnelser,
    )
