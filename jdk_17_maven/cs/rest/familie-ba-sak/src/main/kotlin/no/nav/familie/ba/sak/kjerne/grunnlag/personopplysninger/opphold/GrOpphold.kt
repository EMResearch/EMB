package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.opphold

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.Utils.storForbokstav
import no.nav.familie.ba.sak.common.erInnenfor
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegisteropplysning
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import java.time.LocalDate

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrOpphold")
@Table(name = "PO_OPPHOLD")
data class GrOpphold(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_opphold_seq_generator")
    @SequenceGenerator(
        name = "po_opphold_seq_generator",
        sequenceName = "po_opphold_seq",
        allocationSize = 50,
    )
    val id: Long = 0,

    @Embedded
    val gyldigPeriode: DatoIntervallEntitet? = null,

    @Column(name = "type", nullable = false)
    val type: OPPHOLDSTILLATELSE,

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_po_person_id", nullable = false, updatable = false)
    val person: Person,
) : BaseEntitet() {

    fun tilKopiForNyPerson(nyPerson: Person): GrOpphold =
        copy(id = 0, person = nyPerson)

    fun gjeldendeNå(): Boolean {
        if (gyldigPeriode == null) return true
        return gyldigPeriode.erInnenfor(LocalDate.now())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GrOpphold

        if (gyldigPeriode != other.gyldigPeriode) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = gyldigPeriode.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    fun tilRestRegisteropplysning() = RestRegisteropplysning(
        fom = this.gyldigPeriode?.fom,
        tom = this.gyldigPeriode?.tom,
        verdi = this.type.name.replace('_', ' ').storForbokstav(),
    )

    companion object {

        fun fraOpphold(opphold: Opphold, person: Person) =
            GrOpphold(
                gyldigPeriode = DatoIntervallEntitet(
                    fom = opphold.oppholdFra,
                    tom = opphold.oppholdTil,
                ),
                type = opphold.type,
                person = person,
            )
    }
}

fun List<GrOpphold>.gyldigGjeldendeOppholdstillatelseFødselshendelse() =
    this.any { it.gjeldendeNå() && it.type != OPPHOLDSTILLATELSE.OPPLYSNING_MANGLER }
