package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import no.nav.familie.ba.sak.common.Utils.nullableTilString
import no.nav.familie.ba.sak.common.Utils.storForbokstav
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import java.util.Objects

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrVegadresse")
@DiscriminatorValue("Vegadresse")
data class GrVegadresse(
    @Column(name = "matrikkel_id")
    val matrikkelId: Long?,

    @Column(name = "husnummer")
    val husnummer: String?,

    @Column(name = "husbokstav")
    val husbokstav: String?,

    @Column(name = "bruksenhetsnummer")
    val bruksenhetsnummer: String?,

    @Column(name = "adressenavn")
    val adressenavn: String?,

    @Column(name = "kommunenummer")
    val kommunenummer: String?,

    @Column(name = "tilleggsnavn")
    val tilleggsnavn: String?,

    @Column(name = "postnummer")
    val postnummer: String?,

) : GrBostedsadresse() {

    override fun tilKopiForNyPerson(): GrBostedsadresse =
        GrVegadresse(
            matrikkelId,
            husnummer,
            husbokstav,
            bruksenhetsnummer,
            adressenavn,
            kommunenummer,
            tilleggsnavn,
            postnummer,
        )

    override fun toSecureString(): String {
        return """VegadresseDao(husnummer=$husnummer,husbokstav=$husbokstav,matrikkelId=$matrikkelId,bruksenhetsnummer=$bruksenhetsnummer,
|           adressenavn=$adressenavn,kommunenummer=$kommunenummer,tilleggsnavn=$tilleggsnavn,postnummer=$postnummer
        """.trimMargin()
    }

    override fun toString(): String {
        return "Vegadresse(detaljer skjult)"
    }

    override fun tilFrontendString() = """${
        adressenavn.nullableTilString()
            .storForbokstav()
    } ${husnummer.nullableTilString()}${husbokstav.nullableTilString()}${postnummer.let { ", $it" }}""".trimMargin()

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val otherVegadresse = other as GrVegadresse

        return this === other ||
            (
                (matrikkelId != null && matrikkelId == otherVegadresse.matrikkelId) ||
                    (
                        (matrikkelId == null && otherVegadresse.matrikkelId == null) &&
                            postnummer != null &&
                            !(adressenavn == null && husnummer == null && husbokstav == null) &&
                            (adressenavn == otherVegadresse.adressenavn) &&
                            (husnummer == otherVegadresse.husnummer) &&
                            (husbokstav == otherVegadresse.husbokstav) &&
                            (postnummer == otherVegadresse.postnummer)
                        )
                )
    }

    override fun hashCode(): Int = Objects.hash(matrikkelId)

    companion object {

        fun fraVegadresse(vegadresse: Vegadresse): GrVegadresse =
            GrVegadresse(
                matrikkelId = vegadresse.matrikkelId,
                husnummer = vegadresse.husnummer,
                husbokstav = vegadresse.husbokstav,
                bruksenhetsnummer = vegadresse.bruksenhetsnummer,
                adressenavn = vegadresse.adressenavn,
                kommunenummer = vegadresse.kommunenummer,
                tilleggsnavn = vegadresse.tilleggsnavn,
                postnummer = vegadresse.postnummer,
            )
    }
}
