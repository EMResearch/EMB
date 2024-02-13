package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.erInnenfor
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegisteropplysning
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import java.time.LocalDate

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrBostedsadresse")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@Table(name = "PO_BOSTEDSADRESSE")
abstract class GrBostedsadresse(
    // Alle attributter må være open ellers kastes feil ved oppsrart.
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_bostedsadresse_seq_generator")
    @SequenceGenerator(
        name = "po_bostedsadresse_seq_generator",
        sequenceName = "po_bostedsadresse_seq",
        allocationSize = 50,
    )
    open val id: Long = 0,

    @Embedded
    open var periode: DatoIntervallEntitet? = null,

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "fk_po_person_id")
    open var person: Person? = null,
) : BaseEntitet() {

    abstract fun toSecureString(): String

    abstract fun tilFrontendString(): String

    protected abstract fun tilKopiForNyPerson(): GrBostedsadresse

    fun tilKopiForNyPerson(nyPerson: Person): GrBostedsadresse =
        tilKopiForNyPerson().also {
            it.periode = periode
            it.person = nyPerson
        }

    fun gjeldendeNå(): Boolean {
        if (periode == null) return true
        return periode!!.erInnenfor(LocalDate.now())
    }

    fun tilRestRegisteropplysning() = RestRegisteropplysning(
        fom = this.periode?.fom.takeIf { it != fregManglendeFlytteDato },
        tom = this.periode?.tom,
        verdi = this.tilFrontendString(),
    )

    fun harGyldigFom() = this.periode?.fom != null && this.periode?.fom != fregManglendeFlytteDato

    companion object {

        // Når flyttedato er satt til 0001-01-01, så mangler den egentlig.
        // Det er en feil i Freg, som har arvet mangelfulle data fra DSF.
        val fregManglendeFlytteDato = LocalDate.of(1, 1, 1)

        fun MutableList<GrBostedsadresse>.sisteAdresse(): GrBostedsadresse? {
            if (this.filter { it.periode?.fom == null || it.periode?.fom == fregManglendeFlytteDato }.size > 1) {
                throw Feil(
                    "Finnes flere bostedsadresser uten fom-dato",
                )
            }
            return this.sortedBy { it.periode?.fom }.lastOrNull()
        }

        fun fraBostedsadresse(bostedsadresse: Bostedsadresse, person: Person): GrBostedsadresse {
            val mappetAdresse = when {
                bostedsadresse.vegadresse != null -> {
                    GrVegadresse.fraVegadresse(bostedsadresse.vegadresse!!)
                }

                bostedsadresse.matrikkeladresse != null -> {
                    GrMatrikkeladresse.fraMatrikkeladresse(bostedsadresse.matrikkeladresse!!)
                }

                bostedsadresse.ukjentBosted != null -> {
                    GrUkjentBosted.fraUkjentBosted(bostedsadresse.ukjentBosted!!)
                }

                else -> throw Feil("Vegadresse, matrikkeladresse og ukjent bosted har verdi null ved mapping fra bostedadresse")
            }
            return mappetAdresse.also {
                it.person = person
                it.periode = DatoIntervallEntitet(bostedsadresse.angittFlyttedato, bostedsadresse.gyldigTilOgMed)
            }
        }

        fun erSammeAdresse(adresse: GrBostedsadresse?, andreAdresse: GrBostedsadresse?): Boolean {
            return adresse != null &&
                adresse !is GrUkjentBosted &&
                adresse == andreAdresse
        }
    }
}

fun List<GrBostedsadresse>.filtrerGjeldendeNå(): List<GrBostedsadresse> {
    return this.filter { it.gjeldendeNå() }
}

fun vurderOmPersonerBorSammen(adresser: List<GrBostedsadresse>, andreAdresser: List<GrBostedsadresse>): Boolean {
    return adresser.isNotEmpty() && adresser.any {
        andreAdresser.any { søkerAdresse ->
            val søkerAdresseFom = søkerAdresse.periode?.fom ?: TIDENES_MORGEN
            val søkerAdresseTom = søkerAdresse.periode?.tom ?: TIDENES_ENDE

            val barnAdresseFom = it.periode?.fom ?: TIDENES_MORGEN
            val barnAdresseTom = it.periode?.tom ?: TIDENES_ENDE

            søkerAdresseFom.isSameOrBefore(barnAdresseFom) &&
                søkerAdresseTom.isSameOrAfter(barnAdresseTom) &&
                GrBostedsadresse.erSammeAdresse(søkerAdresse, it)
        }
    }
}
