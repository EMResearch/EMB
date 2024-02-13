package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

fun lagKompetanse(
    behandlingId: Long = lagBehandling().id,
    fom: YearMonth? = YearMonth.now(),
    tom: YearMonth? = null,
    barnAktører: Set<Aktør> = emptySet(),
    søkersAktivitet: KompetanseAktivitet? = null,
    annenForeldersAktivitet: KompetanseAktivitet? = null,
    annenForeldersAktivitetsland: String? = null,
    barnetsBostedsland: String? = null,
    kompetanseResultat: KompetanseResultat? = null,
    søkersAktivitetsland: String? = null,
) = Kompetanse(
    fom = fom,
    tom = tom,
    barnAktører = barnAktører,
    søkersAktivitet = søkersAktivitet,
    annenForeldersAktivitet = annenForeldersAktivitet,
    annenForeldersAktivitetsland = annenForeldersAktivitetsland,
    barnetsBostedsland = barnetsBostedsland,
    resultat = kompetanseResultat,
    søkersAktivitetsland = søkersAktivitetsland,
).also { it.behandlingId = behandlingId }

fun lagValutakurs(
    behandlingId: Long = lagBehandling().id,
    fom: YearMonth? = null,
    tom: YearMonth? = null,
    barnAktører: Set<Aktør> = emptySet(),
    valutakursdato: LocalDate? = null,
    valutakode: String? = null,
    kurs: BigDecimal? = null,
) = Valutakurs(
    fom = fom,
    tom = tom,
    barnAktører = barnAktører,
    valutakursdato = valutakursdato,
    valutakode = valutakode,
    kurs = kurs,
).also { it.behandlingId = behandlingId }

fun lagUtenlandskPeriodebeløp(
    behandlingId: Long = lagBehandling().id,
    fom: YearMonth? = null,
    tom: YearMonth? = null,
    barnAktører: Set<Aktør> = emptySet(),
    beløp: BigDecimal? = null,
    valutakode: String? = null,
    intervall: Intervall? = null,
    utbetalingsland: String = "",
) = UtenlandskPeriodebeløp(
    fom = fom,
    tom = tom,
    barnAktører = barnAktører,
    valutakode = valutakode,
    beløp = beløp,
    intervall = intervall,
    utbetalingsland = utbetalingsland,
).also { it.behandlingId = behandlingId }
