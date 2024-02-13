package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegisteropplysning
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlKontaktinformasjonForDødsboAdresse
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Dødsfall")
@Table(name = "po_doedsfall")
data class Dødsfall(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_doedsfall_seq_generator")
    @SequenceGenerator(name = "po_doedsfall_seq_generator", sequenceName = "po_doedsfall_seq", allocationSize = 50)
    val id: Long = 0,

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "fk_po_person_id", referencedColumnName = "id", nullable = false)
    val person: Person,

    @Column(name = "doedsfall_dato", nullable = false)
    val dødsfallDato: LocalDate,

    @Column(name = "doedsfall_adresse", nullable = true)
    val dødsfallAdresse: String? = null,

    @Column(name = "doedsfall_postnummer", nullable = true)
    val dødsfallPostnummer: String? = null,

    @Column(name = "doedsfall_poststed", nullable = true)
    val dødsfallPoststed: String? = null,

    @Column(name = "manuell_registrert", nullable = false)
    val manuellRegistrert: Boolean = false,
) : BaseEntitet() {

    fun tilKopiForNyPerson(nyPerson: Person): Dødsfall =
        copy(id = 0, person = nyPerson)

    fun hentAdresseToString(): String {
        return """$dødsfallAdresse, $dødsfallPostnummer $dødsfallPoststed"""
    }

    fun tilRestRegisteropplysning() = RestRegisteropplysning(
        fom = this.dødsfallDato,
        tom = null,
        verdi = if (dødsfallAdresse == null) "-" else hentAdresseToString(),
    )
}

fun lagDødsfallFraPdl(
    person: Person,
    dødsfallDatoFraPdl: String?,
    dødsfallAdresseFraPdl: PdlKontaktinformasjonForDødsboAdresse?,
): Dødsfall? {
    if (dødsfallDatoFraPdl.isNullOrBlank()) {
        return null
    }
    return Dødsfall(
        person = person,
        dødsfallDato = LocalDate.parse(dødsfallDatoFraPdl),
        dødsfallAdresse = dødsfallAdresseFraPdl?.adresselinje1,
        dødsfallPostnummer = dødsfallAdresseFraPdl?.postnummer,
        dødsfallPoststed = dødsfallAdresseFraPdl?.poststedsnavn,
        manuellRegistrert = false,
    )
}

fun lagDødsfall(
    person: Person,
    dødsfallDato: LocalDate,
    dødsfallAdresse: String? = null,
    dødsfallPostnummer: String? = null,
    dødsfallPoststed: String? = null,

): Dødsfall {
    return Dødsfall(
        person = person,
        dødsfallDato = dødsfallDato,
        dødsfallAdresse = dødsfallAdresse,
        dødsfallPostnummer = dødsfallPostnummer,
        dødsfallPoststed = dødsfallPoststed,
    )
}
