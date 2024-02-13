package no.nav.familie.tilbake.iverksettvedtak

import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.tilbake.api.dto.AktsomhetDto
import no.nav.familie.tilbake.api.dto.SærligGrunnDto
import no.nav.familie.tilbake.api.dto.VilkårsvurderingsperiodeDto
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.SærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import java.time.YearMonth

object VilkårsvurderingsPeriodeDomainUtil {

    fun lagGrovtUaktsomVilkårsvurderingsperiode(fom: YearMonth, tom: YearMonth) = VilkårsvurderingsperiodeDto(
        periode = Datoperiode(fom, tom),
        begrunnelse = "testverdi",
        aktsomhetDto =
        AktsomhetDto(
            aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
            ileggRenter = true,
            andelTilbakekreves = null,
            begrunnelse = "testverdi",
            særligeGrunnerTilReduksjon = false,
            tilbakekrevSmåbeløp = true,
            særligeGrunnerBegrunnelse = "testverdi",
            særligeGrunner = listOf(SærligGrunnDto(SærligGrunn.ANNET, "testverdi")),
        ),
        vilkårsvurderingsresultat =
        Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
    )
}
