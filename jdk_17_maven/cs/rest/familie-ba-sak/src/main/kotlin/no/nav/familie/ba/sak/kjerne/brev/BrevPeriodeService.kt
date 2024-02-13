package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.endringstidspunkt.Beløpsdifferanse
import no.nav.familie.ba.sak.kjerne.beregning.endringstidspunkt.hentPerioderMedEndringerFra
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevperiodeData
import no.nav.familie.ba.sak.kjerne.brev.domene.RestBehandlingsgrunnlagForBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertKompetanse
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertUregistrertBarn
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.tilMinimertePersoner
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.dødeBarnForrigePeriode
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BrevBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.erFørsteVedtaksperiodePåFagsak
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.ytelseErFraForrigePeriode
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import org.springframework.stereotype.Service

@Service
class BrevPeriodeService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val persongrunnlagService: PersongrunnlagService,
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    private val sanityService: SanityService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val personidentService: PersonidentService,
    private val kompetanseService: KompetanseService,
    private val integrasjonClient: IntegrasjonClient,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val featureToggleService: FeatureToggleService,
) {

    fun hentBrevperioderData(
        vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
        behandling: Behandling,
        skalLogge: Boolean = true,
    ): List<BrevperiodeData> {
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)
            ?: error("Finner ikke vilkårsvurdering ved begrunning av vedtak")

        val endredeUtbetalingAndeler = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandlingId = behandling.id)

        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = behandling.id)

        val andelerMedEndringer = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

        val uregistrerteBarn =
            søknadGrunnlagService.hentAktiv(behandlingId = behandling.id)?.hentUregistrerteBarn()
                ?: emptyList()

        val forrigeBehandling =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksattFraBehandlingsId(behandlingId = behandling.id)

        val kompetanser = kompetanseService.hentKompetanser(behandlingId = BehandlingId(behandling.id))
            .filter {
                if (forrigeBehandling?.erAutomatiskEøsMigrering() == true && behandling.skalBehandlesAutomatisk) {
                    it.erObligatoriskeFelterSatt()
                } else {
                    true
                }
            }

        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser()
        val sanityEØSBegrunnelser = sanityService.hentSanityEØSBegrunnelser()

        val restBehandlingsgrunnlagForBrev = hentRestBehandlingsgrunnlagForBrev(
            vilkårsvurdering = vilkårsvurdering,
            endredeUtbetalingAndeler = endredeUtbetalingAndeler,
            persongrunnlag = personopplysningGrunnlag,
        )

        return vedtaksperioder.map {
            hentBrevperiodeData(
                vedtaksperiodeMedBegrunnelser = it,
                restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
                personopplysningGrunnlag = personopplysningGrunnlag,
                andelerTilkjentYtelse = andelerMedEndringer,
                uregistrerteBarn = uregistrerteBarn,
                skalLogge = skalLogge,
                kompetanser = kompetanser.toList(),
                sanityBegrunnelser = sanityBegrunnelser,
                sanityEØSBegrunnelser = sanityEØSBegrunnelser,
            )
        }
    }

    private fun hentBrevperiodeData(
        vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
        restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        uregistrerteBarn: List<BarnMedOpplysninger>,
        kompetanser: List<Kompetanse>,
        sanityBegrunnelser: Map<Standardbegrunnelse, SanityBegrunnelse>,
        sanityEØSBegrunnelser: Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse>,

        skalLogge: Boolean = true,
    ): BrevperiodeData {
        val minimerteUregistrerteBarn = uregistrerteBarn.map { it.tilMinimertUregistrertBarn() }

        val utvidetVedtaksperiodeMedBegrunnelse = vedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelser(
            personopplysningGrunnlag = personopplysningGrunnlag,
            andelerTilkjentYtelse = andelerTilkjentYtelse,
        )

        val ytelserForrigePeriode =
            andelerTilkjentYtelse.filter { ytelseErFraForrigePeriode(it, utvidetVedtaksperiodeMedBegrunnelse) }

        val dødeBarnForrigePeriode =
            dødeBarnForrigePeriode(ytelserForrigePeriode, personopplysningGrunnlag.barna.tilMinimertePersoner())

        val minimertVedtaksperiode =
            utvidetVedtaksperiodeMedBegrunnelse.tilMinimertVedtaksperiode(
                sanityBegrunnelser = sanityBegrunnelser,
                sanityEØSBegrunnelser = sanityEØSBegrunnelser,
            )

        val landkoderISO2 = integrasjonClient.hentLandkoderISO2()

        val brevperiodeData = BrevperiodeData(
            minimertVedtaksperiode = minimertVedtaksperiode,
            restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
            uregistrerteBarn = minimerteUregistrerteBarn,
            brevMålform = personopplysningGrunnlag.søker.målform,
            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak(
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                periodeFom = utvidetVedtaksperiodeMedBegrunnelse.fom,
            ),
            barnMedReduksjonFraForrigeBehandlingIdent = hentBarnsPersonIdentMedRedusertPeriode(
                vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                andelerTilkjentYtelse = andelerTilkjentYtelse,
            ),
            minimerteKompetanserForPeriode = hentMinimerteKompetanserForPeriode(
                kompetanser = kompetanser,
                fom = vedtaksperiodeMedBegrunnelser.fom?.toYearMonth(),
                tom = vedtaksperiodeMedBegrunnelser.tom?.toYearMonth(),
                personopplysningGrunnlag = personopplysningGrunnlag,
                landkoderISO2 = landkoderISO2,
            ),
            minimerteKompetanserSomStopperRettFørPeriode = hentKompetanserSomStopperRettFørPeriode(
                kompetanser = kompetanser,
                periodeFom = minimertVedtaksperiode.fom?.toYearMonth(),
            ).filter {
                it.erObligatoriskeFelterSatt()
            }.map {
                it.tilMinimertKompetanse(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    landkoderISO2 = landkoderISO2,
                )
            },
            dødeBarnForrigePeriode = dødeBarnForrigePeriode,
        )

        if (skalLogge) {
            secureLogger.info(
                "Data for brevperiode på behandling ${vedtaksperiodeMedBegrunnelser.vedtak.behandling}: \n" +
                    brevperiodeData.tilBrevperiodeForLogging().convertDataClassToJson(),
            )
        }

        return brevperiodeData
    }

    private fun hentBarnsPersonIdentMedRedusertPeriode(
        vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
        andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    ): List<String> {
        val forrigeBehandling =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(vedtaksperiodeMedBegrunnelser.vedtak.behandling)
        return if (forrigeBehandling != null) {
            val forrigeAndelerMedEndringer = andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(forrigeBehandling.id)
            val endringerITilkjentYtelsePerBarn =
                andelerTilkjentYtelse.hentPerioderMedEndringerFra(forrigeAndelerMedEndringer)
            endringerITilkjentYtelsePerBarn.keys.filter { barn ->
                endringerITilkjentYtelsePerBarn.getValue(barn).any {
                    it.overlapper(
                        LocalDateSegment(
                            vedtaksperiodeMedBegrunnelser.fom,
                            vedtaksperiodeMedBegrunnelser.tom,
                            null,
                        ),
                    )
                }
            }.mapNotNull { barn ->
                val result: LocalDateTimeline<Beløpsdifferanse> = endringerITilkjentYtelsePerBarn.getValue(barn)
                if (!result.filterValue { beløp -> beløp < 0 }.isEmpty) {
                    personidentService.hentAktør(barn).aktivFødselsnummer()
                } else {
                    null
                }
            }
        } else {
            emptyList()
        }
    }

    fun genererBrevBegrunnelserForPeriode(vedtaksperiodeId: Long): List<BrevBegrunnelse> {
        val vedtaksperiodeMedBegrunnelser =
            vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(vedtaksperiodeId)

        val begrunnelseDataForVedtaksperiode =
            hentBrevperioderData(
                vedtaksperioder = listOf(vedtaksperiodeMedBegrunnelser),
                behandling = vedtaksperiodeMedBegrunnelser.vedtak.behandling,
            ).single()
        return begrunnelseDataForVedtaksperiode.hentBegrunnelserOgFritekster()
    }
}
