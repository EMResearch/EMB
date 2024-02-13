package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestAnnenVurdering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Optional

class AnnenVurderingServiceTest {

    private val annenVurderingRepository = mockk<AnnenVurderingRepository>(relaxed = true)

    private lateinit var annenVurderingService: AnnenVurderingService
    private lateinit var personResultat: PersonResultat

    @BeforeEach
    fun setUp() {
        annenVurderingService = AnnenVurderingService(annenVurderingRepository = annenVurderingRepository)

        personResultat = lagPersonResultat(
            vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling()),
            person = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 1, 1)),
            resultat = Resultat.OPPFYLT,
            periodeFom = LocalDate.of(2020, 1, 1),
            periodeTom = LocalDate.of(2020, 7, 1),
        )
    }

    @Test
    fun `Verifiser endreAnnenVurdering`() {
        every { annenVurderingRepository.findById(any()) } returns Optional.of(
            AnnenVurdering(
                resultat = Resultat.OPPFYLT,
                type = AnnenVurderingType.OPPLYSNINGSPLIKT,
                begrunnelse = "begrunnelse",
                personResultat = personResultat,
            ),
        )
        val nyAnnenVurering = AnnenVurdering(
            resultat = Resultat.IKKE_OPPFYLT,
            type = AnnenVurderingType.OPPLYSNINGSPLIKT,
            begrunnelse = "begrunnelse to",
            personResultat = personResultat,
        )

        every { annenVurderingRepository.save(any()) } returns nyAnnenVurering

        annenVurderingService.endreAnnenVurdering(
            personResultat.vilkårsvurdering.behandling.id,
            123L,
            RestAnnenVurdering(
                123L,
                Resultat.IKKE_OPPFYLT,
                type = AnnenVurderingType.OPPLYSNINGSPLIKT,
                begrunnelse = "begrunnelse to",
            ),
        )

        verify(exactly = 1) {
            annenVurderingRepository.save(any())
        }
    }
}
