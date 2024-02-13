package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.feilHvis
import no.nav.familie.ba.sak.ekstern.restDomene.RestAnnenVurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AnnenVurderingService(
    private val annenVurderingRepository: AnnenVurderingRepository,
) {

    fun hent(personResultat: PersonResultat, annenVurderingType: AnnenVurderingType): AnnenVurdering? =
        annenVurderingRepository.findBy(
            personResultat = personResultat,
            type = annenVurderingType,
        )

    fun hent(annenVurderingId: Long): AnnenVurdering = annenVurderingRepository.findById(annenVurderingId)
        .orElseThrow { error("Annen vurdering med id $annenVurderingId finnes ikke i db") }

    @Transactional
    fun endreAnnenVurdering(
        behandlingId: Long,
        annenVurderingId: Long,
        restAnnenVurdering: RestAnnenVurdering,
    ) {
        val vurdering = hent(annenVurderingId = annenVurderingId)
        val behandling = vurdering.personResultat.vilkårsvurdering.behandling

        val behandlingIdForVurdering = behandling.id
        feilHvis(behandlingIdForVurdering != behandlingId) {
            "Prøver å oppdatere en vurdering=$annenVurderingId koblet til en annen($behandlingIdForVurdering) behandling enn $behandlingId"
        }
        annenVurderingRepository.save(
            vurdering.also {
                it.resultat = restAnnenVurdering.resultat
                it.begrunnelse = restAnnenVurdering.begrunnelse
                it.type = restAnnenVurdering.type
            },
        )
    }
}

fun PersonResultat.leggTilBlankAnnenVurdering(annenVurderingType: AnnenVurderingType) {
    this.andreVurderinger.add(
        AnnenVurdering(
            personResultat = this,
            type = annenVurderingType,
        ),
    )
}
