package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrUkjentBosted")
@DiscriminatorValue("ukjentBosted")
data class GrUkjentBosted(
    @Column(name = "bostedskommune")
    val bostedskommune: String,

) : GrBostedsadresse() {

    override fun tilKopiForNyPerson(): GrBostedsadresse =
        GrUkjentBosted(bostedskommune)

    override fun toSecureString(): String {
        return """UkjentadresseDao(bostedskommune=$bostedskommune""".trimMargin()
    }

    override fun tilFrontendString() = """Ukjent adresse, kommune $bostedskommune""".trimMargin()

    override fun toString(): String {
        return "UkjentBostedAdresse(detaljer skjult)"
    }

    companion object {

        fun fraUkjentBosted(ukjentBosted: UkjentBosted): GrUkjentBosted =
            GrUkjentBosted(bostedskommune = ukjentBosted.bostedskommune)
    }
}
