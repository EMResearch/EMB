package no.nav.familie.tilbake.vilkårsvurdering

import io.kotest.matchers.equality.shouldBeEqualToComparingFieldsExcept
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurdering
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VilkårsvurderingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    private val vilkår = Testdata.vilkårsvurdering

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Vilkårsvurdering til basen`() {
        vilkårsvurderingRepository.insert(vilkår)

        val lagretVilkår = vilkårsvurderingRepository.findByIdOrThrow(vilkår.id)
        lagretVilkår.shouldBeEqualToComparingFieldsExcept(vilkår, Vilkårsvurdering::sporbar, Vilkårsvurdering::versjon)
        lagretVilkår.versjon shouldBe 1
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Vilkårsvurdering i basen`() {
        vilkårsvurderingRepository.insert(vilkår)
        var lagretVilkår = vilkårsvurderingRepository.findByIdOrThrow(vilkår.id)
        val oppdatertVilkår = lagretVilkår.copy(aktiv = false)

        vilkårsvurderingRepository.update(oppdatertVilkår)

        lagretVilkår = vilkårsvurderingRepository.findByIdOrThrow(vilkår.id)
        lagretVilkår.shouldBeEqualToComparingFieldsExcept(oppdatertVilkår, Vilkårsvurdering::sporbar, Vilkårsvurdering::versjon)
        lagretVilkår.versjon shouldBe 2
    }
}
