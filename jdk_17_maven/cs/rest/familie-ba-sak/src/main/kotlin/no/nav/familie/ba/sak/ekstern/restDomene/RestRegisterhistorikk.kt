package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import java.time.LocalDate
import java.time.LocalDateTime

data class RestRegisterhistorikk(
    val hentetTidspunkt: LocalDateTime,
    val sivilstand: List<RestRegisteropplysning>? = emptyList(),
    val oppholdstillatelse: List<RestRegisteropplysning>? = emptyList(),
    val statsborgerskap: List<RestRegisteropplysning>? = emptyList(),
    val bostedsadresse: List<RestRegisteropplysning>? = emptyList(),
    val dødsboadresse: List<RestRegisteropplysning>? = emptyList(),
)

fun Person.tilRestRegisterhistorikk() = RestRegisterhistorikk(
    hentetTidspunkt = this.personopplysningGrunnlag.opprettetTidspunkt,
    oppholdstillatelse = opphold.map { it.tilRestRegisteropplysning() },
    statsborgerskap = statsborgerskap.map { it.tilRestRegisteropplysning() },
    bostedsadresse = this.bostedsadresser.map { it.tilRestRegisteropplysning() }.fyllInnTomDatoer(),
    sivilstand = this.sivilstander.map { it.tilRestRegisteropplysning() },
    dødsboadresse = if (this.dødsfall == null) emptyList() else listOf(this.dødsfall!!.tilRestRegisteropplysning()),
)

data class RestRegisteropplysning(
    val fom: LocalDate?,
    val tom: LocalDate?,
    var verdi: String,
)

fun List<RestRegisteropplysning>.fyllInnTomDatoer(): List<RestRegisteropplysning> =
    this
        .sortedBy { it.fom }
        .foldRight(mutableListOf<RestRegisteropplysning>()) { foregående, acc ->
            if (acc.isEmpty() || foregående.tom != null || foregående.fom == null) {
                acc.add(foregående)
            } else {
                acc.add(foregående.copy(tom = acc.last().fom?.minusDays(1)))
            }
            acc
        }
        .reversed()
