package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class VilkårTest {

    @Nested
    inner class `Hent relevante vilkår for persontype BARN` {
        @Test
        fun `For ordinær nasjonal sak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(
                personType = PersonType.BARN,
                fagsakType = FagsakType.NORMAL,
                behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
            )
            val vilkårForBarn = setOf(
                Vilkår.UNDER_18_ÅR,
                Vilkår.BOR_MED_SØKER,
                Vilkår.GIFT_PARTNERSKAP,
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD,
            )
            Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
        }

        @Test
        fun `For utvidet nasjonal sak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(
                personType = PersonType.BARN,
                fagsakType = FagsakType.NORMAL,
                behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
            )
            val vilkårForBarn = setOf(
                Vilkår.UNDER_18_ÅR,
                Vilkår.BOR_MED_SØKER,
                Vilkår.GIFT_PARTNERSKAP,
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD,
            )
            Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
        }

        @Test
        fun `For ordinær institusjonssak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(
                personType = PersonType.BARN,
                fagsakType = FagsakType.INSTITUSJON,
                behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
            )
            val vilkårForBarn = setOf(
                Vilkår.UNDER_18_ÅR,
                Vilkår.BOR_MED_SØKER,
                Vilkår.GIFT_PARTNERSKAP,
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD,
            )
            Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
        }

        @Test
        fun `For utvidet institusjonssak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(
                personType = PersonType.BARN,
                fagsakType = FagsakType.INSTITUSJON,
                behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
            )
            val vilkårForBarn = setOf(
                Vilkår.UNDER_18_ÅR,
                Vilkår.BOR_MED_SØKER,
                Vilkår.GIFT_PARTNERSKAP,
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD,
            )
            Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
        }

        @Test
        fun `For ordinær enslig mindreårig sak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(
                personType = PersonType.BARN,
                fagsakType = FagsakType.BARN_ENSLIG_MINDREÅRIG,
                behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
            )
            val vilkårForBarn = setOf(
                Vilkår.UNDER_18_ÅR,
                Vilkår.BOR_MED_SØKER,
                Vilkår.GIFT_PARTNERSKAP,
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD,
            )
            Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
        }

        @Test
        fun `For utvidet enslig mindreårig sak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(
                personType = PersonType.BARN,
                fagsakType = FagsakType.BARN_ENSLIG_MINDREÅRIG,
                behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
            )
            val vilkårForBarn = setOf(
                Vilkår.UNDER_18_ÅR,
                Vilkår.BOR_MED_SØKER,
                Vilkår.GIFT_PARTNERSKAP,
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD,
                Vilkår.UTVIDET_BARNETRYGD,
            )
            Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
        }
    }

    @Nested
    inner class `Hent relevante vilkår for persontype SØKER` {
        @Test
        fun `For ordinær nasjonal sak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(
                personType = PersonType.SØKER,
                fagsakType = FagsakType.NORMAL,
                behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
            )
            val vilkårForBarn = setOf(
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD,
            )
            Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
        }

        @Test
        fun `For utvidet nasjonal sak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(
                personType = PersonType.SØKER,
                fagsakType = FagsakType.NORMAL,
                behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
            )
            val vilkårForBarn = setOf(
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD,
                Vilkår.UTVIDET_BARNETRYGD,
            )
            Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
        }

        @Test
        fun `For ordinær institusjonssak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(
                personType = PersonType.SØKER,
                fagsakType = FagsakType.INSTITUSJON,
                behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
            )
            val vilkårForBarn = emptySet<Vilkår>()
            Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
        }

        @Test
        fun `For utvidet institusjonssak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(
                personType = PersonType.SØKER,
                fagsakType = FagsakType.INSTITUSJON,
                behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
            )
            val vilkårForBarn = emptySet<Vilkår>()
            Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
        }

        @Test
        fun `For ordinær enslig mindreårig sak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(
                personType = PersonType.SØKER,
                fagsakType = FagsakType.BARN_ENSLIG_MINDREÅRIG,
                behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
            )
            val vilkårForBarn = emptySet<Vilkår>()
            Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
        }

        @Test
        fun `For utvidet enslig mindreårig sak`() {
            val relevanteVilkår = Vilkår.hentVilkårFor(
                personType = PersonType.SØKER,
                fagsakType = FagsakType.BARN_ENSLIG_MINDREÅRIG,
                behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
            )
            val vilkårForBarn = emptySet<Vilkår>()
            Assertions.assertEquals(vilkårForBarn, relevanteVilkår)
        }
    }
}
