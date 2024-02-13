package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.util.Objects

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "AnnenVurdering")
@Table(name = "ANNEN_VURDERING")
data class AnnenVurdering(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "annen_vurdering_seq_generator")
    @SequenceGenerator(
        name = "annen_vurdering_seq_generator",
        sequenceName = "annen_vurdering_seq",
        allocationSize = 50,
    )
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "fk_person_resultat_id")
    var personResultat: PersonResultat,

    @Enumerated(EnumType.STRING)
    @Column(name = "resultat")
    var resultat: Resultat = Resultat.IKKE_VURDERT,

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    var type: AnnenVurderingType,

    @Column(name = "begrunnelse")
    var begrunnelse: String? = null,
) : BaseEntitet() {

    fun kopierMedParent(nyPersonResultat: PersonResultat? = null): AnnenVurdering {
        return AnnenVurdering(
            personResultat = nyPersonResultat ?: personResultat,
            type = type,
            resultat = resultat,
            begrunnelse = begrunnelse,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnnenVurdering

        return type == other.type
    }

    override fun hashCode(): Int {
        return Objects.hash(type)
    }

    override fun toString(): String {
        return "AnnenVurdering(id=$id, type=$type, personident=${personResultat.aktør.aktørId})"
    }

    fun toSecureString(): String {
        return "AnnenVurdering(id=$id, type=$type, personident=${personResultat.aktør.aktivFødselsnummer()})"
    }
}

enum class AnnenVurderingType {
    OPPLYSNINGSPLIKT,
}
