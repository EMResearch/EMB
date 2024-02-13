package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import io.mockk.mockk
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.lagTriggesAv
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertVilkårResultat
import no.nav.familie.ba.sak.kjerne.brev.erFørstePeriodeOgVilkårIkkeOppfylt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårUtilsTest {

    val vedtaksperiode: Periode = Periode(
        fom = LocalDate.now().minusMonths(2),
        tom = LocalDate.now().plusMonths(4),
    )

    val triggesAv = lagTriggesAv(deltbosted = false, vurderingAnnetGrunnlag = false, medlemskap = false)
    val vilkårResultatIkkeOppfylt: VilkårResultat = lagVilkårResultat(
        resultat = Resultat.IKKE_OPPFYLT,
        periodeFom = vedtaksperiode.fom,
        periodeTom = vedtaksperiode.tom,
        personResultat = mockk(relaxed = true),
    )
    val vilkårResultatIkkeOppfyltDelvisOverlapp: VilkårResultat = lagVilkårResultat(
        resultat = Resultat.IKKE_OPPFYLT,
        periodeFom = vedtaksperiode.fom.minusMonths(1),
        periodeTom = vedtaksperiode.tom.plusMonths(1),
        personResultat = mockk(relaxed = true),
    )
    val vilkårResultatUtenforPeriode: VilkårResultat = lagVilkårResultat(
        resultat = Resultat.IKKE_OPPFYLT,
        periodeFom = vedtaksperiode.tom.plusMonths(1),
        periodeTom = vedtaksperiode.tom.plusMonths(3),
        personResultat = mockk(relaxed = true),
    )

    val vilkårResultatOppfylt: VilkårResultat = lagVilkårResultat(
        resultat = Resultat.OPPFYLT,
        periodeFom = vedtaksperiode.fom,
        periodeTom = vedtaksperiode.tom,
        personResultat = mockk(relaxed = true),
    )

    @Test
    fun `Er førte periode dersom resultat ikke er godkjent og det ikke er noen andeler tilkjent ytelse før perioden`() {
        Assertions.assertTrue(
            erFørstePeriodeOgVilkårIkkeOppfylt(
                erFørsteVedtaksperiodePåFagsak = true,
                vilkårResultat = vilkårResultatIkkeOppfylt.tilMinimertVilkårResultat(),
                vedtaksperiode = vedtaksperiode,
                triggesAv = triggesAv,

            ),
        )
        Assertions.assertTrue(
            erFørstePeriodeOgVilkårIkkeOppfylt(
                erFørsteVedtaksperiodePåFagsak = true,
                vilkårResultat = vilkårResultatIkkeOppfyltDelvisOverlapp.tilMinimertVilkårResultat(),
                vedtaksperiode = vedtaksperiode,
                triggesAv = triggesAv,

            ),
        )
    }

    @Test
    fun `Er ikke førte periode dersom det er en andel tilkjent ytelse før perioden`() {
        Assertions.assertFalse(
            erFørstePeriodeOgVilkårIkkeOppfylt(
                erFørsteVedtaksperiodePåFagsak = false,
                vilkårResultat = vilkårResultatIkkeOppfylt.tilMinimertVilkårResultat(),
                vedtaksperiode = vedtaksperiode,
                triggesAv = triggesAv,

            ),
        )
    }

    @Test
    fun `Er ikke førte periode dersom vilkårResultatet er oppfylt`() {
        Assertions.assertFalse(
            erFørstePeriodeOgVilkårIkkeOppfylt(
                erFørsteVedtaksperiodePåFagsak = true,
                vilkårResultat = vilkårResultatOppfylt.tilMinimertVilkårResultat(),
                vedtaksperiode = vedtaksperiode,
                triggesAv = triggesAv,

            ),
        )
    }

    @Test
    fun `Er ikke førte periode dersom vilkårResultatet ikke overlapper med periode`() {
        Assertions.assertFalse(
            erFørstePeriodeOgVilkårIkkeOppfylt(
                erFørsteVedtaksperiodePåFagsak = true,
                vilkårResultat = vilkårResultatUtenforPeriode.tilMinimertVilkårResultat(),
                vedtaksperiode = vedtaksperiode,
                triggesAv = triggesAv,

            ),
        )
    }
}
