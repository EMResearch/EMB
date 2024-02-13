package no.nav.familie.ba.sak.kjerne.eøs

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseTestController
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingTestController
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class VilkårsvurderingTilValutakursIntegrasjonTest : AbstractSpringIntegrationTest() {

    @Autowired
    lateinit var vilkårsvurderingTestController: VilkårsvurderingTestController

    @Autowired
    lateinit var kompetanseTestController: KompetanseTestController

    @Test
    fun `vilkårsvurdering med EØS-perioder + kompetanser med sekundærland fører til skjemaer med valutakurser`() {
        val søkerStartdato = 1.jan(2020).tilLocalDate()
        val barnStartdato = 2.jan(2020).tilLocalDate()

        val vilkårsvurderingRequest = mapOf(
            søkerStartdato to mapOf(
                Vilkår.BOSATT_I_RIKET to "EEEEEEEEEEEEEEEE",
                Vilkår.LOVLIG_OPPHOLD to "EEEEEEEEEEEEEEEE",
            ),
            barnStartdato to mapOf(
                Vilkår.UNDER_18_ÅR to "++++++++++++++++",
                Vilkår.GIFT_PARTNERSKAP to "++++++++++++++++",
                Vilkår.BOSATT_I_RIKET to "EEEEEEEEEEEEEEEE",
                Vilkår.LOVLIG_OPPHOLD to "EEEEEEEEEEEEEEEE",
                Vilkår.BOR_MED_SØKER to "EEEEEEEEEEEEEEEE",
            ),
        )

        val kompetanseRequest = mapOf(
            barnStartdato to "PPPSSSSSSPPSSS--",
        )

        val utvidetBehandlingFør =
            vilkårsvurderingTestController.opprettBehandlingMedVilkårsvurdering(vilkårsvurderingRequest)
                .body?.data!!

        assertTrue(utvidetBehandlingFør.valutakurser.isEmpty())

        val utvidetBehandlingEtter =
            kompetanseTestController.endreKompetanser(utvidetBehandlingFør.behandlingId, kompetanseRequest)
                .body?.data!!

        assertTrue(utvidetBehandlingEtter.valutakurser.isNotEmpty())
    }
}
