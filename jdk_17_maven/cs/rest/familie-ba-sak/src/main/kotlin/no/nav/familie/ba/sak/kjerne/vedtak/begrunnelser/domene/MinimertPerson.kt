package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson
import java.time.LocalDate

class MinimertPerson(
    val type: PersonType,
    val fødselsdato: LocalDate,
    val aktørId: String,
    val aktivPersonIdent: String,
    val dødsfallsdato: LocalDate?,
) {
    val erDød = {
        dødsfallsdato != null
    }
    fun hentSeksårsdag(): LocalDate = fødselsdato.plusYears(6)

    fun tilMinimertRestPerson() = MinimertRestPerson(
        personIdent = aktivPersonIdent,
        fødselsdato = fødselsdato,
        type = type,
    )
}

fun PersonopplysningGrunnlag.tilMinimertePersoner(): List<MinimertPerson> =
    this.søkerOgBarn.tilMinimertePersoner()

fun List<Person>.tilMinimertePersoner(): List<MinimertPerson> =
    this.map {
        MinimertPerson(
            it.type,
            it.fødselsdato,
            it.aktør.aktørId,
            it.aktør.aktivFødselsnummer(),
            it.dødsfall?.dødsfallDato,
        )
    }

fun List<MinimertPerson>.harBarnMedSeksårsdagPåFom(fom: LocalDate?) = this.any { person ->
    person
        .hentSeksårsdag()
        .toYearMonth() == (fom?.toYearMonth() ?: TIDENES_ENDE.toYearMonth())
}
