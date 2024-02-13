package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.util.Objects

/**
 * Denne mapper p.t Norsk person ident (fødselsnummer, inkl F-nr, D-nr eller FDAT)
 *
 *  * F-nr: http://lovdata.no/forskrift/2007-11-09-1268/%C2%A72-2 (F-nr)
 *
 *  * D-nr: http://lovdata.no/forskrift/2007-11-09-1268/%C2%A72-5 (D-nr), samt hvem som kan utstede
 * (http://lovdata.no/forskrift/2007-11-09-1268/%C2%A72-6)
 *
 *  * FDAT: Personer uten FNR. Disse har fødselsdato + 00000 (normalt) eller fødselsdato + 00001 (dødfødt).
 *
 */
@Embeddable
class PersonIdent(
    @JsonProperty("id")
    @Column(name = "person_ident", updatable = false, length = 50)
    val ident: String,
) : Comparable<PersonIdent> {

    override fun compareTo(other: PersonIdent): Int {
        return ident.compareTo(other.ident)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        } else if (other == null || this.javaClass != other.javaClass) {
            return false
        }
        val otherObject = other as PersonIdent
        return ident == otherObject.ident
    }

    override fun hashCode(): Int {
        return Objects.hash(ident)
    }
}
