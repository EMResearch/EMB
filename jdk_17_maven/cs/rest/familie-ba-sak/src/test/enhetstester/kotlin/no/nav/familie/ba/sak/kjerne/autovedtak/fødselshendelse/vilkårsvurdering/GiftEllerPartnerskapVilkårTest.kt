package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering

import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class GiftEllerPartnerskapVilkårTest {

    @Test
    fun `Gift-vilkår gir resultat JA for fødselshendelse når sivilstand er uoppgitt`() {
        val evaluering = vilkår.vurderVilkår(barn)
        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
    }

    companion object {

        val vilkår = Vilkår.GIFT_PARTNERSKAP
        val barn =
            tilfeldigPerson(personType = PersonType.BARN).apply {
                sivilstander = mutableListOf(GrSivilstand(type = SIVILSTAND.UOPPGITT, person = this))
            }
    }
}
