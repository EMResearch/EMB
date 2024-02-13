package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold.GrArbeidsforhold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.opphold.GrOpphold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.finnNåværendeMedlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.finnSterkesteMedlemskap
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.Språkkode
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.Period
import java.util.Objects

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Person")
@Table(name = "PO_PERSON")
data class Person(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_person_seq_generator")
    @SequenceGenerator(name = "po_person_seq_generator", sequenceName = "po_person_seq", allocationSize = 50)
    val id: Long = 0,

    // SØKER, BARN, ANNENPART
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    val type: PersonType,

    @Column(name = "foedselsdato", nullable = false)
    val fødselsdato: LocalDate,

    @Column(name = "navn", nullable = false)
    val navn: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "kjoenn", nullable = false)
    val kjønn: Kjønn,

    @Enumerated(EnumType.STRING)
    @Column(name = "maalform", nullable = false)
    val målform: Målform = Målform.NB,

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_gr_personopplysninger_id", nullable = false, updatable = false)
    val personopplysningGrunnlag: PersonopplysningGrunnlag,

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_aktoer_id", nullable = false, updatable = false)
    val aktør: Aktør,

    @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    // Workaround før Hibernatebug https://hibernate.atlassian.net/browse/HHH-1718
    @Fetch(value = FetchMode.SUBSELECT)
    var bostedsadresser: MutableList<GrBostedsadresse> = mutableListOf(),

    @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    // Workaround før Hibernatebug https://hibernate.atlassian.net/browse/HHH-1718
    @Fetch(value = FetchMode.SUBSELECT)
    var statsborgerskap: MutableList<GrStatsborgerskap> = mutableListOf(),

    @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    // Workaround før Hibernatebug https://hibernate.atlassian.net/browse/HHH-1718
    @Fetch(value = FetchMode.SUBSELECT)
    var opphold: MutableList<GrOpphold> = mutableListOf(),

    @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    // Workaround før Hibernatebug https://hibernate.atlassian.net/browse/HHH-1718
    @Fetch(value = FetchMode.SUBSELECT)
    var arbeidsforhold: MutableList<GrArbeidsforhold> = mutableListOf(),

    @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    // Workaround før Hibernatebug https://hibernate.atlassian.net/browse/HHH-1718
    @Fetch(value = FetchMode.SUBSELECT)
    var sivilstander: MutableList<GrSivilstand> = mutableListOf(),

    @OneToOne(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, optional = true)
    var dødsfall: Dødsfall? = null,
) : BaseEntitet() {

    fun tilKopiForNyttPersonopplysningGrunnlag(nyttPersonopplysningGrunnlag: PersonopplysningGrunnlag): Person =
        copy(
            id = 0,
            personopplysningGrunnlag = nyttPersonopplysningGrunnlag,
            bostedsadresser = mutableListOf(),
            statsborgerskap = mutableListOf(),
            opphold = mutableListOf(),
            arbeidsforhold = mutableListOf(),
            sivilstander = mutableListOf(),
        )
            .also {
                it.bostedsadresser.addAll(
                    bostedsadresser.map { grBostedsadresse ->
                        grBostedsadresse.tilKopiForNyPerson(
                            it,
                        )
                    },
                )
                it.statsborgerskap.addAll(
                    statsborgerskap.map { grStatsborgerskap ->
                        grStatsborgerskap.tilKopiForNyPerson(
                            it,
                        )
                    },
                )
                it.opphold.addAll(opphold.map { grOpphold -> grOpphold.tilKopiForNyPerson(it) })
                it.arbeidsforhold.addAll(arbeidsforhold.map { grArbeidsforhold -> grArbeidsforhold.tilKopiForNyPerson(it) })
                it.sivilstander.addAll(sivilstander.map { grSivilstand -> grSivilstand.tilKopiForNyPerson(it) })
                it.dødsfall = dødsfall?.tilKopiForNyPerson(it)
            }

    override fun toString(): String {
        return """Person(aktørId=$aktør,
                        |type=$type
                        |fødselsdato=$fødselsdato)
        """.trimMargin()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val entitet: Person = other as Person
        return Objects.equals(hashCode(), entitet.hashCode())
    }

    override fun hashCode(): Int {
        return Objects.hash(aktør, fødselsdato)
    }

    fun hentAlder(): Int = Period.between(fødselsdato, now()).years

    fun hentSeksårsdag(): LocalDate = fødselsdato.plusYears(6)

    fun fyllerAntallÅrInneværendeMåned(år: Int): Boolean =
        this.fødselsdato.toYearMonth() == now().minusYears(år.toLong()).toYearMonth()

    fun erYngreEnnInneværendeMåned(år: Int): Boolean =
        this.fødselsdato.isAfter(now().minusYears(år.toLong()).sisteDagIMåned())

    fun erDød(): Boolean = dødsfall != null

    fun hentSterkesteMedlemskap(): Medlemskap? {
        val nåværendeMedlemskap = finnNåværendeMedlemskap(statsborgerskap)
        return finnSterkesteMedlemskap(nåværendeMedlemskap)
    }
}

enum class Kjønn {
    MANN,
    KVINNE,
    UKJENT,
}

enum class Medlemskap {
    NORDEN,
    EØS,
    TREDJELANDSBORGER,
    STATSLØS,
    UKJENT,
}

enum class Målform {
    NB,
    NN,
    ;

    fun tilSanityFormat() = when (this) {
        NB -> "bokmaal"
        NN -> "nynorsk"
    }

    fun tilSpråkkode() = when (this) {
        NB -> Språkkode.NB
        NN -> Språkkode.NN
    }
}
