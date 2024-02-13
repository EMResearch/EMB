package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate

@EntityListeners(RollestyringMotDatabase::class)
@Entity
@Table(name = "GR_PERSONOPPLYSNINGER")
data class PersonopplysningGrunnlag(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GR_PERSONOPPLYSNINGER_SEQ_GENERATOR")
    @SequenceGenerator(
        name = "GR_PERSONOPPLYSNINGER_SEQ_GENERATOR",
        sequenceName = "GR_PERSONOPPLYSNINGER_SEQ",
        allocationSize = 50,
    )
    val id: Long = 0,

    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    val behandlingId: Long,

    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "personopplysningGrunnlag",
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH],
    )
    val personer: MutableSet<Person> = mutableSetOf(),

    @Column(name = "aktiv", nullable = false)
    var aktiv: Boolean = true,

) : BaseEntitet() {

    val barna: List<Person>
        get() = personer.filter { it.type == PersonType.BARN }

    val yngsteBarnSinFødselsdato: LocalDate
        get() = barna.maxOf { it.fødselsdato }

    val søker: Person
        get() = personer.singleOrNull { it.type == PersonType.SØKER }
            // Vil returnere barnet på EM-saker, som da i prinsippet også er søkeren. Vil også returnere barnet på inst. saker
            ?: personer.singleOrNull()?.takeIf { it.type == PersonType.BARN }
            ?: error("Persongrunnlag mangler søker eller det finnes flere personer i grunnlaget med type=SØKER")

    val annenForelder: Person?
        get() = personer.singleOrNull { it.type == PersonType.ANNENPART }

    val søkerOgBarn: List<Person>
        get() = personer.filter { it.type == PersonType.SØKER || it.type == PersonType.BARN }

    fun harBarnMedSeksårsdagPåFom(fom: LocalDate?) = personer.any { person ->
        person
            .hentSeksårsdag()
            .toYearMonth() == (fom?.toYearMonth() ?: TIDENES_ENDE.toYearMonth())
    }

    fun tilKopiForNyBehandling(
        behandling: Behandling,
        søkerOgBarnMedTilkjentYtelseFraForrigeBehandling: List<Aktør>,
    ): PersonopplysningGrunnlag =
        copy(id = 0, behandlingId = behandling.id, personer = mutableSetOf()).also { it ->
            it.personer
                .addAll(
                    personer.filter { person -> søkerOgBarnMedTilkjentYtelseFraForrigeBehandling.any { søkerEllerBarn -> søkerEllerBarn.aktørId == person.aktør.aktørId } }
                        .map { person -> person.tilKopiForNyttPersonopplysningGrunnlag(it) },
                )
        }

    override fun toString(): String {
        val sb = StringBuilder("PersonopplysningGrunnlagEntitet{")
        sb.append("id=").append(id)
        sb.append(", personer=").append(personer.toString())
        sb.append(", aktiv=").append(aktiv)
        sb.append('}')
        return sb.toString()
    }
}

fun Aktør.tilPerson(personopplysningGrunnlag: PersonopplysningGrunnlag): Person? =
    personopplysningGrunnlag.personer.find { it.aktør == this }
