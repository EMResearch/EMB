package no.nav.familie.tilbake.beregning

import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.api.dto.BeregnetPeriodeDto
import no.nav.familie.tilbake.api.dto.BeregnetPerioderDto
import no.nav.familie.tilbake.api.dto.BeregningsresultatDto
import no.nav.familie.tilbake.api.dto.BeregningsresultatsperiodeDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Saksbehandlingstype
import no.nav.familie.tilbake.beregning.modell.Beregningsresultat
import no.nav.familie.tilbake.beregning.modell.Beregningsresultatsperiode
import no.nav.familie.tilbake.beregning.modell.FordeltKravgrunnlagsbeløp
import no.nav.familie.tilbake.beregning.modell.GrunnlagsperiodeMedSkatteprosent
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.FeatureToggleConfig.Companion.BRUK_6_DESIMALER_I_SKATTEBEREGNING
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.foreldelse.VurdertForeldelseRepository
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesperiode
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import no.nav.familie.tilbake.vilkårsvurdering.domain.AnnenVurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class TilbakekrevingsberegningService(
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val vurdertForeldelseRepository: VurdertForeldelseRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val behandlingRepository: BehandlingRepository,
    private val kravgrunnlagsberegningService: KravgrunnlagsberegningService,
    private val featureToggleService: FeatureToggleService,
) {

    fun hentBeregningsresultat(behandlingId: UUID): BeregningsresultatDto {
        val beregningsresultat = beregn(behandlingId)
        val beregningsresultatsperioder = beregningsresultat.beregningsresultatsperioder.map {
            BeregningsresultatsperiodeDto(
                periode = it.periode.toDatoperiode(),
                vurdering = it.vurdering,
                feilutbetaltBeløp = it.feilutbetaltBeløp,
                andelAvBeløp = it.andelAvBeløp,
                renteprosent = it.renteprosent,
                tilbakekrevingsbeløp = it.tilbakekrevingsbeløp,
                tilbakekrevesBeløpEtterSkatt = it.tilbakekrevingsbeløpEtterSkatt,
            )
        }
        return BeregningsresultatDto(
            beregningsresultatsperioder = beregningsresultatsperioder,
            vedtaksresultat = beregningsresultat.vedtaksresultat,
        )
    }

    fun beregn(behandlingId: UUID): Beregningsresultat {
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        val vurdertForeldelse = hentVurdertForeldelse(behandlingId)
        val vilkårsvurdering = hentVilkårsvurdering(behandlingId)
        val vurderingsperioder: List<Månedsperiode> = finnPerioder(vurdertForeldelse, vilkårsvurdering)
        val perioderMedBeløp: Map<Månedsperiode, FordeltKravgrunnlagsbeløp> =
            kravgrunnlagsberegningService.fordelKravgrunnlagBeløpPåPerioder(kravgrunnlag, vurderingsperioder)
        val beregningsresultatperioder =
            beregn(
                kravgrunnlag,
                vurdertForeldelse,
                vilkårsvurdering,
                perioderMedBeløp,
                skalBeregneRenter(kravgrunnlag.fagområdekode),
            )
        val totalTilbakekrevingsbeløp = beregningsresultatperioder.sumOf { it.tilbakekrevingsbeløp }
        val totalFeilutbetaltBeløp = beregningsresultatperioder.sumOf { it.feilutbetaltBeløp }
        return Beregningsresultat(
            vedtaksresultat = bestemVedtakResultat(
                behandlingId,
                totalTilbakekrevingsbeløp,
                totalFeilutbetaltBeløp,
            ),
            beregningsresultatsperioder = (beregningsresultatperioder),
        )
    }

    fun beregnBeløp(behandlingId: UUID, perioder: List<Datoperiode>): BeregnetPerioderDto {
        // Alle familieytelsene er månedsytelser. Så periode som skal lagres bør være innenfor en måned.
        KravgrunnlagsberegningService.validatePerioder(perioder)
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)

        return BeregnetPerioderDto(
            beregnetPerioder = perioder.map {
                val feilutbetaltBeløp =
                    KravgrunnlagsberegningService.beregnFeilutbetaltBeløp(kravgrunnlag, it.toMånedsperiode())
                BeregnetPeriodeDto(
                    periode = it,
                    feilutbetaltBeløp = feilutbetaltBeløp,
                )
            },
        )
    }

    private fun hentVilkårsvurdering(behandlingId: UUID): Vilkårsvurdering? {
        return vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
    }

    private fun hentVurdertForeldelse(behandlingId: UUID): VurdertForeldelse? {
        return vurdertForeldelseRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
    }

    private fun finnPerioder(
        vurdertForeldelse: VurdertForeldelse?,
        vilkårsvurdering: Vilkårsvurdering?,
    ): List<Månedsperiode> {
        return finnForeldedePerioder(vurdertForeldelse) + finnIkkeForeldedePerioder(vilkårsvurdering)
    }

    private fun beregn(
        kravgrunnlag: Kravgrunnlag431,
        vurdertForeldelse: VurdertForeldelse?,
        vilkårsvurdering: Vilkårsvurdering?,
        perioderMedBeløp: Map<Månedsperiode, FordeltKravgrunnlagsbeløp>,
        beregnRenter: Boolean,
    ): List<Beregningsresultatsperiode> {
        return (
            beregnForForeldedePerioder(vurdertForeldelse, perioderMedBeløp) +
                beregnForIkkeForeldedePerioder(kravgrunnlag, vilkårsvurdering, perioderMedBeløp, beregnRenter)
            )
            .sortedBy { it.periode.fom }
    }

    private fun finnIkkeForeldedePerioder(vilkårsvurdering: Vilkårsvurdering?): List<Månedsperiode> {
        return vilkårsvurdering?.perioder?.map(Vilkårsvurderingsperiode::periode)
            ?: emptyList()
    }

    private fun finnForeldedePerioder(vurdertForeldelse: VurdertForeldelse?): List<Månedsperiode> {
        return vurdertForeldelse?.foreldelsesperioder
            ?.filter(Foreldelsesperiode::erForeldet)
            ?.map(Foreldelsesperiode::periode)
            ?: emptyList()
    }

    private fun beregnForIkkeForeldedePerioder(
        kravgrunnlag: Kravgrunnlag431,
        vilkårsvurdering: Vilkårsvurdering?,
        kravbeløpPerPeriode: Map<Månedsperiode, FordeltKravgrunnlagsbeløp>,
        beregnRenter: Boolean,
    ): Collection<Beregningsresultatsperiode> {
        return vilkårsvurdering?.perioder
            ?.map { beregnIkkeForeldetPeriode(kravgrunnlag, it, kravbeløpPerPeriode, beregnRenter) }
            ?: emptyList()
    }

    private fun beregnForForeldedePerioder(
        vurdertForeldelse: VurdertForeldelse?,
        kravbeløpPerPeriode: Map<Månedsperiode, FordeltKravgrunnlagsbeløp>,
    ): Collection<Beregningsresultatsperiode> {
        return vurdertForeldelse?.foreldelsesperioder
            ?.filter { Foreldelsesvurderingstype.FORELDET == it.foreldelsesvurderingstype }
            ?.map { beregnForeldetPeriode(kravbeløpPerPeriode, it) }
            ?: emptyList()
    }

    private fun beregnForeldetPeriode(
        beløpPerPeriode: Map<Månedsperiode, FordeltKravgrunnlagsbeløp>,
        foreldelsePeriode: Foreldelsesperiode,
    ): Beregningsresultatsperiode {
        val periode: Månedsperiode = foreldelsePeriode.periode
        val delresultat: FordeltKravgrunnlagsbeløp =
            beløpPerPeriode[periode] ?: throw IllegalStateException("Periode i finnes ikke i map beløpPerPeriode")

        return Beregningsresultatsperiode(
            periode = periode,
            feilutbetaltBeløp = delresultat.feilutbetaltBeløp,
            riktigYtelsesbeløp = delresultat.riktigYtelsesbeløp,
            utbetaltYtelsesbeløp = delresultat.utbetaltYtelsesbeløp,
            tilbakekrevingsbeløp = BigDecimal.ZERO,
            tilbakekrevingsbeløpUtenRenter = BigDecimal.ZERO,
            rentebeløp = BigDecimal.ZERO,
            andelAvBeløp = BigDecimal.ZERO,
            vurdering = AnnenVurdering.FORELDET,
            skattebeløp = BigDecimal.ZERO,
            tilbakekrevingsbeløpEtterSkatt = BigDecimal.ZERO,
        )
    }

    private fun beregnIkkeForeldetPeriode(
        kravgrunnlag: Kravgrunnlag431,
        vurdering: Vilkårsvurderingsperiode,
        kravbeløpPerPeriode: Map<Månedsperiode, FordeltKravgrunnlagsbeløp>,
        beregnRenter: Boolean,
    ): Beregningsresultatsperiode {
        val delresultat = kravbeløpPerPeriode[vurdering.periode]
            ?: throw IllegalStateException("Periode i finnes ikke i map kravbeløpPerPeriode")
        val perioderMedSkattProsent = lagGrunnlagPeriodeMedSkattProsent(vurdering.periode, kravgrunnlag)

        val bruk6desimalerISkatteberegning = featureToggleService.isEnabled(BRUK_6_DESIMALER_I_SKATTEBEREGNING)
        return TilbakekrevingsberegningVilkår.beregn(
            vurdering,
            delresultat,
            perioderMedSkattProsent,
            beregnRenter,
            bruk6desimalerISkatteberegning,
        )
    }

    private fun lagGrunnlagPeriodeMedSkattProsent(
        vurderingsperiode: Månedsperiode,
        kravgrunnlag: Kravgrunnlag431,
    ): List<GrunnlagsperiodeMedSkatteprosent> {
        return kravgrunnlag.perioder
            .sortedBy { it.periode.fom }
            .map {
                it.beløp.map { kgBeløp ->
                    val maksTilbakekrevesBeløp: BigDecimal =
                        BeløpsberegningUtil.beregnBeløpForPeriode(
                            kgBeløp.tilbakekrevesBeløp,
                            vurderingsperiode,
                            it.periode,
                        )
                    GrunnlagsperiodeMedSkatteprosent(it.periode, maksTilbakekrevesBeløp, kgBeløp.skatteprosent)
                }
            }.flatten()
    }

    private fun skalBeregneRenter(fagområdekode: Fagområdekode): Boolean =
        when (fagområdekode) {
            Fagområdekode.BA, Fagområdekode.KS -> false
            else -> true
        }

    private fun bestemVedtakResultat(
        behandlingId: UUID,
        tilbakekrevingsbeløp: BigDecimal,
        feilutbetaltBeløp: BigDecimal?,
    ): Vedtaksresultat {
        val behandling: Behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP == behandling.saksbehandlingstype) {
            return Vedtaksresultat.INGEN_TILBAKEBETALING
        }
        if (tilbakekrevingsbeløp.compareTo(BigDecimal.ZERO) == 0) {
            return Vedtaksresultat.INGEN_TILBAKEBETALING
        }
        if (tilbakekrevingsbeløp < feilutbetaltBeløp) {
            return Vedtaksresultat.DELVIS_TILBAKEBETALING
        }
        return Vedtaksresultat.FULL_TILBAKEBETALING
    }
}
