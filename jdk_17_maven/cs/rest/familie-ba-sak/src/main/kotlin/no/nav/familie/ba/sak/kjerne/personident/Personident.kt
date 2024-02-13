package no.nav.familie.ba.sak.kjerne.personident

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.Pattern
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDateTime
import java.util.Objects

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Personident")
@Table(name = "PERSONIDENT")
data class Personident(
    @Id
    @Column(name = "foedselsnummer", nullable = false)
    // Lovlige typer er fnr, dnr eller npid
    // Validator kommer virke først i Spring 3.0 grunnet at hibernate tatt i bruke Jakarta.
    @Pattern(regexp = VALID_FØDSELSNUMMER)
    val fødselsnummer: String,

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_aktoer_id", nullable = false, updatable = false)
    val aktør: Aktør,

    @Column(name = "aktiv", nullable = false)
    var aktiv: Boolean = true,

    @Column(name = "gjelder_til", columnDefinition = "DATE")
    var gjelderTil: LocalDateTime? = null,

) : BaseEntitet() {

    init {
        require(VALID.matcher(fødselsnummer).matches()) {
            "Ugyldig fødselsnummer, støtter kun 11 siffer.)"
        }
    }

    override fun toString(): String {
        return """Personident(aktørId=${aktør.aktørId},
                        |aktiv=$aktiv
                        |gjelderTil=$gjelderTil)
        """.trimMargin()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val entitet: Personident = other as Personident
        return fødselsnummer == entitet.fødselsnummer && aktiv == entitet.aktiv
    }

    override fun hashCode(): Int {
        return Objects.hash(fødselsnummer, aktiv)
    }

    companion object {
        private const val VALID_FØDSELSNUMMER = "^\\d{11}$"
        private val VALID =
            java.util.regex.Pattern.compile(VALID_FØDSELSNUMMER)
    }
}
