package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.IUtfyltEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.UtfyltKompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Objects

sealed interface VedtaksperiodeGrunnlagForPerson {
    val person: Person
    val vilkårResultaterForVedtaksperiode: List<VilkårResultatForVedtaksperiode>

    fun erEksplisittAvslag(): Boolean =
        this is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget && this.erEksplisittAvslag

    fun erInnvilget() = this is VedtaksperiodeGrunnlagForPersonVilkårInnvilget && this.erInnvilgetEndretUtbetaling()

    fun hentInnvilgedeYtelsestyper() =
        if (this is VedtaksperiodeGrunnlagForPersonVilkårInnvilget) {
            this.andeler.filter { it.prosent > BigDecimal.ZERO }
                .map { it.type }.toSet()
        } else {
            emptySet()
        }

    fun kopier(
        person: Person = this.person,
        vilkårResultaterForVedtaksperiode: List<VilkårResultatForVedtaksperiode> = this.vilkårResultaterForVedtaksperiode,
    ): VedtaksperiodeGrunnlagForPerson {
        return when (this) {
            is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget -> this.copy(
                person,
                vilkårResultaterForVedtaksperiode,
            )

            is VedtaksperiodeGrunnlagForPersonVilkårInnvilget -> this.copy(person, vilkårResultaterForVedtaksperiode)
        }
    }
}

data class VedtaksperiodeGrunnlagForPersonVilkårInnvilget(
    override val person: Person,
    override val vilkårResultaterForVedtaksperiode: List<VilkårResultatForVedtaksperiode>,
    val andeler: Iterable<AndelForVedtaksperiode>,
    val kompetanse: KompetanseForVedtaksperiode? = null,
    val endretUtbetalingAndel: EndretUtbetalingAndelForVedtaksperiode? = null,
    val overgangsstønad: OvergangsstønadForVedtaksperiode? = null,
) : VedtaksperiodeGrunnlagForPerson {
    fun erInnvilgetEndretUtbetaling() =
        endretUtbetalingAndel?.prosent != BigDecimal.ZERO || endretUtbetalingAndel?.årsak == Årsak.DELT_BOSTED
}

data class VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget(
    override val person: Person,
    override val vilkårResultaterForVedtaksperiode: List<VilkårResultatForVedtaksperiode>,
) : VedtaksperiodeGrunnlagForPerson {
    val erEksplisittAvslag: Boolean = vilkårResultaterForVedtaksperiode.inneholderEksplisittAvslag()

    fun List<VilkårResultatForVedtaksperiode>.inneholderEksplisittAvslag() =
        this.any { it.erEksplisittAvslagPåSøknad == true }
}

data class VilkårResultatForVedtaksperiode(
    val vilkårType: Vilkår,
    val resultat: Resultat,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering>,
    val vurderesEtter: Regelverk?,
    val erEksplisittAvslagPåSøknad: Boolean?,
    val standardbegrunnelser: List<IVedtakBegrunnelse>,
    val aktørId: AktørId,
    val fom: LocalDate?,
    val tom: LocalDate?,
) {
    constructor(vilkårResultat: VilkårResultat) : this(
        vilkårType = vilkårResultat.vilkårType,
        resultat = vilkårResultat.resultat,
        utdypendeVilkårsvurderinger = vilkårResultat.utdypendeVilkårsvurderinger,
        vurderesEtter = vilkårResultat.vurderesEtter,
        erEksplisittAvslagPåSøknad = vilkårResultat.erEksplisittAvslagPåSøknad,
        standardbegrunnelser = vilkårResultat.standardbegrunnelser,
        fom = vilkårResultat.periodeFom,
        tom = vilkårResultat.periodeTom,
        aktørId = vilkårResultat.personResultat?.aktør?.aktørId
            ?: throw Feil("$vilkårResultat er ikke knyttet til personResultat"),
    )
}

fun List<VilkårResultatForVedtaksperiode>.erLikUtenFomOgTom(other: List<VilkårResultatForVedtaksperiode>): Boolean {
    return this.map { it.copy(fom = null, tom = null) }.toSet() == other.map { it.copy(fom = null, tom = null) }.toSet()
}

data class EndretUtbetalingAndelForVedtaksperiode(
    val prosent: BigDecimal,
    val årsak: Årsak,
    val søknadstidspunkt: LocalDate,
) {
    constructor(endretUtbetalingAndel: IUtfyltEndretUtbetalingAndel) : this(
        prosent = endretUtbetalingAndel.prosent,
        årsak = endretUtbetalingAndel.årsak,
        søknadstidspunkt = endretUtbetalingAndel.søknadstidspunkt,
    )
}

data class AndelForVedtaksperiode(
    val kalkulertUtbetalingsbeløp: Int,
    val nasjonaltPeriodebeløp: Int?,
    val type: YtelseType,
    val prosent: BigDecimal,
    val sats: Int,
) {
    constructor(andelTilkjentYtelse: AndelTilkjentYtelse) : this(
        kalkulertUtbetalingsbeløp = andelTilkjentYtelse.kalkulertUtbetalingsbeløp,
        nasjonaltPeriodebeløp = andelTilkjentYtelse.nasjonaltPeriodebeløp,
        type = andelTilkjentYtelse.type,
        prosent = andelTilkjentYtelse.prosent,
        sats = andelTilkjentYtelse.sats,
    )

    override fun equals(other: Any?): Boolean {
        if (other !is AndelForVedtaksperiode) {
            return false
        } else if (this === other) {
            return true
        }

        val annen = other
        return Objects.equals(kalkulertUtbetalingsbeløp, annen.kalkulertUtbetalingsbeløp) &&
            Objects.equals(type, annen.type) &&
            Objects.equals(prosent, annen.prosent) &&
            satsErlik(annen.sats)
    }

    private fun satsErlik(annen: Int): Boolean {
        return if (kalkulertUtbetalingsbeløp == 0) {
            true
        } else {
            Objects.equals(sats, annen)
        }
    }

    override fun hashCode(): Int {
        return if (kalkulertUtbetalingsbeløp == 0) {
            Objects.hash(
                kalkulertUtbetalingsbeløp,
                type,
                prosent,
            )
        } else {
            Objects.hash(
                kalkulertUtbetalingsbeløp,
                type,
                prosent,
                sats,
            )
        }
    }
}

data class KompetanseForVedtaksperiode(
    val søkersAktivitet: KompetanseAktivitet,
    val annenForeldersAktivitet: KompetanseAktivitet,
    val annenForeldersAktivitetsland: String?,
    val søkersAktivitetsland: String,
    val barnetsBostedsland: String,
    val resultat: KompetanseResultat,
    val barnAktører: Set<Aktør>,
) {
    constructor(kompetanse: UtfyltKompetanse) : this(
        søkersAktivitet = kompetanse.søkersAktivitet,
        annenForeldersAktivitet = kompetanse.annenForeldersAktivitet,
        annenForeldersAktivitetsland = kompetanse.annenForeldersAktivitetsland,
        søkersAktivitetsland = kompetanse.søkersAktivitetsland,
        barnetsBostedsland = kompetanse.barnetsBostedsland,
        resultat = kompetanse.resultat,
        barnAktører = kompetanse.barnAktører,
    )
}

data class OvergangsstønadForVedtaksperiode(
    val fom: LocalDate,
    val tom: LocalDate,
) {
    constructor(periodeOvergangsstønad: InternPeriodeOvergangsstønad) : this(
        fom = periodeOvergangsstønad.fomDato,
        tom = periodeOvergangsstønad.tomDato,
    )
}
