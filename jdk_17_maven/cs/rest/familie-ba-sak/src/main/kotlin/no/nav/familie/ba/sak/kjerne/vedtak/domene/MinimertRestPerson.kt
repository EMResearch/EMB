package no.nav.familie.ba.sak.kjerne.vedtak.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.ekstern.restDomene.RestPerson
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevPeriodePersonForLogging
import no.nav.familie.ba.sak.kjerne.brev.domene.EndretUtbetalingAndelPåPersonForLogging
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.brev.domene.RestBehandlingsgrunnlagForBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.UtbetalingPåPersonForLogging
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import java.time.LocalDate

/**
 * NB: Bør ikke brukes internt, men kun ut mot eksterne tjenester siden klassen
 * inneholder aktiv personIdent og ikke aktørId.
 */
data class MinimertRestPerson(
    val personIdent: String,
    val fødselsdato: LocalDate,
    val type: PersonType,
) {
    fun hentSeksårsdag(): LocalDate = fødselsdato.plusYears(6)
}

fun RestPerson.tilMinimertPerson() = MinimertRestPerson(
    personIdent = this.personIdent,
    fødselsdato = fødselsdato ?: throw Feil("Fødselsdato mangler"),
    type = this.type,
)

fun List<MinimertRestPerson>.barnMedSeksårsdagPåFom(fom: LocalDate?): List<MinimertRestPerson> {
    return this
        .filter { it.type == PersonType.BARN }
        .filter { person ->
            person.hentSeksårsdag().toYearMonth() == (
                fom?.toYearMonth()
                    ?: TIDENES_ENDE.toYearMonth()
                )
        }
}

fun Person.tilMinimertPerson() = MinimertRestPerson(
    personIdent = this.aktør.aktivFødselsnummer(),
    fødselsdato = this.fødselsdato,
    type = this.type,
)

fun MinimertRestPerson.tilBrevPeriodeTestPerson(
    brevPeriodeGrunnlag: MinimertVedtaksperiode,
    restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
    barnMedReduksjonFraForrigeBehandlingIdent: List<String>,
): BrevPeriodePersonForLogging {
    val minimertePersonResultater =
        restBehandlingsgrunnlagForBrev.minimertePersonResultater.firstOrNull { it.personIdent == this.personIdent }!!
    val minimerteEndretUtbetalingAndelPåPerson =
        restBehandlingsgrunnlagForBrev.minimerteEndredeUtbetalingAndeler.filter { it.personIdent == this.personIdent }
    val minimerteUtbetalingsperiodeDetaljer = brevPeriodeGrunnlag.minimerteUtbetalingsperiodeDetaljer.filter {
        it.person.personIdent == this.personIdent
    }

    return BrevPeriodePersonForLogging(
        fødselsdato = this.fødselsdato,
        type = this.type,
        overstyrteVilkårresultater = minimertePersonResultater.minimerteVilkårResultater,
        andreVurderinger = minimertePersonResultater.minimerteAndreVurderinger,
        endredeUtbetalinger = minimerteEndretUtbetalingAndelPåPerson.map {
            EndretUtbetalingAndelPåPersonForLogging(
                periode = it.periode,
                årsak = it.årsak,
            )
        },
        utbetalinger = minimerteUtbetalingsperiodeDetaljer.map {
            UtbetalingPåPersonForLogging(
                it.ytelseType,
                it.utbetaltPerMnd,
                it.erPåvirketAvEndring,
                it.prosent,
            )
        },
        harReduksjonFraForrigeBehandling = barnMedReduksjonFraForrigeBehandlingIdent.contains(this.personIdent),
    )
}
