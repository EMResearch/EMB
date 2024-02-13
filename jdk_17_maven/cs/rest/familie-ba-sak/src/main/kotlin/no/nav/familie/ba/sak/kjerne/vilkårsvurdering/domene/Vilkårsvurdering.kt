package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Vilkårsvurdering")
@Table(name = "VILKAARSVURDERING")
data class Vilkårsvurdering(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vilkaarsvurdering_seq_generator")
    @SequenceGenerator(
        name = "vilkaarsvurdering_seq_generator",
        sequenceName = "vilkaarsvurdering_seq",
        allocationSize = 50,
    )
    val id: Long = 0,

    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandling: Behandling,

    @Column(name = "aktiv", nullable = false)
    var aktiv: Boolean = true,

    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "vilkårsvurdering",
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH],
    )
    var personResultater: Set<PersonResultat> = setOf(),

    @Column(name = "ytelse_personer", columnDefinition = "text")
    var ytelsePersoner: String? = null,

) : BaseEntitet() {

    override fun toString(): String {
        return "Vilkårsvurdering(id=$id, behandling=${behandling.id})"
    }

    fun kopier(inkluderAndreVurderinger: Boolean = false): Vilkårsvurdering {
        val nyVilkårsvurdering = Vilkårsvurdering(
            behandling = behandling,
            aktiv = aktiv,
        )

        nyVilkårsvurdering.personResultater = personResultater.map {
            it.kopierMedParent(
                vilkårsvurdering = nyVilkårsvurdering,
                inkluderAndreVurderinger = inkluderAndreVurderinger,
            )
        }.toSet()
        return nyVilkårsvurdering
    }

    fun tilKopiForNyBehandling(
        nyBehandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
    ): Vilkårsvurdering {
        val nyVilkårsvurdering = Vilkårsvurdering(behandling = nyBehandling)

        nyVilkårsvurdering.personResultater =
            personResultater.filter { personopplysningGrunnlag.personer.any { person -> person.aktør.aktørId == it.aktør.aktørId } }
                .map {
                    it.tilKopiForNyVilkårsvurdering(nyVilkårsvurdering = nyVilkårsvurdering)
                }.toSet()

        return nyVilkårsvurdering
    }

    fun finnOpplysningspliktVilkår(): AnnenVurdering? {
        return personResultater.single { it.erSøkersResultater() }
            .andreVurderinger.singleOrNull { it.type == AnnenVurderingType.OPPLYSNINGSPLIKT }
    }

    fun hentPersonResultaterTil(aktørId: String): List<VilkårResultat> =
        personResultater.find { it.aktør.aktørId == aktørId }?.vilkårResultater?.toList()
            ?: throw IllegalStateException("Fant ikke personresultat for $aktørId")
}
