package no.nav.familie.ba.sak.datagenerator.behandling

import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.config.testSanityKlient
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertPersonResultat
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.steg.StatusFraOppdragMedTask
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.steg.domene.JournalførVedtaksbrevDTO
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.tilMinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.tilMinimertePersoner
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.erFørsteVedtaksperiodePåFagsak
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.hentGyldigeBegrunnelserForVedtaksperiodeMinimert
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.hentYtelserForSøkerForrigeMåned
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.ytelseErFraForrigePeriode
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.StatusFraOppdragTask
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.prosessering.domene.Task
import java.time.LocalDate
import java.util.Properties

fun kjørStegprosessForBehandling(
    tilSteg: StegType = StegType.BEHANDLING_AVSLUTTET,
    søkerFnr: String,
    barnasIdenter: List<String>,
    vedtakService: VedtakService,
    underkategori: BehandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
    behandlingÅrsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    overstyrendeVilkårsvurdering: Vilkårsvurdering,
    behandlingstype: BehandlingType,

    vilkårsvurderingService: VilkårsvurderingService,
    stegService: StegService,
    vedtaksperiodeService: VedtaksperiodeService,
    endretUtbetalingAndelHentOgPersisterService: EndretUtbetalingAndelHentOgPersisterService,
    fagsakService: FagsakService,
    persongrunnlagService: PersongrunnlagService,
    andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    brevmalService: BrevmalService,
): Behandling {
    val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)

    val nyBehandling = NyBehandling(
        kategori = BehandlingKategori.NASJONAL,
        underkategori = underkategori,
        søkersIdent = søkerFnr,
        behandlingType = behandlingstype,
        behandlingÅrsak = behandlingÅrsak,
        barnasIdenter = barnasIdenter,
        søknadMottattDato = LocalDate.now(),
        fagsakId = fagsak.id,
    )

    val behandling = stegService.håndterNyBehandling(nyBehandling)

    val behandlingEtterPersongrunnlagSteg =
        if (behandlingÅrsak == BehandlingÅrsak.SØKNAD || behandlingÅrsak == BehandlingÅrsak.FØDSELSHENDELSE) {
            håndterSøknadSteg(stegService, behandling, søkerFnr, barnasIdenter, underkategori)
        } else {
            behandling
        }

    if (tilSteg == StegType.REGISTRERE_PERSONGRUNNLAG || tilSteg == StegType.REGISTRERE_SØKNAD) {
        return behandlingEtterPersongrunnlagSteg
    }

    val behandlingEtterVilkårsvurderingSteg =
        håndterVilkårsvurderingSteg(
            vilkårsvurderingService = vilkårsvurderingService,
            behandling = behandlingEtterPersongrunnlagSteg,
            nyVilkårsvurdering = overstyrendeVilkårsvurdering,
            stegService = stegService,
        )
    if (tilSteg == StegType.VILKÅRSVURDERING) return behandlingEtterVilkårsvurderingSteg

    val behandlingEtterBehandlingsresultat = stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurderingSteg)
    if (tilSteg == StegType.BEHANDLINGSRESULTAT) return behandlingEtterBehandlingsresultat

    val behandlingEtterSimuleringSteg = hånderSilmuleringssteg(stegService, behandlingEtterBehandlingsresultat)
    if (tilSteg == StegType.VURDER_TILBAKEKREVING) return behandlingEtterSimuleringSteg

    val behandlingEtterSendTilBeslutter =
        håndterSendtTilBeslutterSteg(
            behandlingEtterSimuleringSteg = behandlingEtterSimuleringSteg,
            vedtakService = vedtakService,
            vedtaksperiodeService = vedtaksperiodeService,
            stegService = stegService,
            persongrunnlagService = persongrunnlagService,
            andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
            endretUtbetalingAndelHentOgPersisterService = endretUtbetalingAndelHentOgPersisterService,
            sanityBegrunnelser = testSanityKlient.hentBegrunnelserMap(),
            vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingEtterSimuleringSteg.id)!!,
        )
    if (tilSteg == StegType.SEND_TIL_BESLUTTER) return behandlingEtterSendTilBeslutter

    val behandlingEtterBeslutteVedtak =
        håndterBeslutteVedtakSteg(stegService, behandlingEtterSendTilBeslutter)
    if (tilSteg == StegType.BESLUTTE_VEDTAK) return behandlingEtterBeslutteVedtak

    val behandlingEtterIverksetteVedtak =
        håndterIverksetteVedtakSteg(stegService, behandlingEtterBeslutteVedtak, vedtakService)
    if (tilSteg == StegType.IVERKSETT_MOT_OPPDRAG) return behandlingEtterIverksetteVedtak

    val behandlingEtterStatusFraOppdrag =
        håndterStatusFraOppdragSteg(stegService, behandlingEtterIverksetteVedtak, søkerFnr, vedtakService)
    if (tilSteg == StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI) return behandlingEtterStatusFraOppdrag

    val behandlingEtterIverksetteMotTilbake =
        stegService.håndterIverksettMotFamilieTilbake(behandlingEtterStatusFraOppdrag, Properties())
    if (tilSteg == StegType.IVERKSETT_MOT_FAMILIE_TILBAKE) return behandlingEtterIverksetteMotTilbake

    val behandlingEtterJournalførtVedtak =
        håndterJournalførtVedtakSteg(stegService, behandlingEtterIverksetteMotTilbake, vedtakService)
    if (tilSteg == StegType.JOURNALFØR_VEDTAKSBREV) return behandlingEtterJournalførtVedtak

    val behandlingEtterDistribuertVedtak =
        håndterDistribuertVedtakSteg(stegService, behandlingEtterJournalførtVedtak, søkerFnr, brevmalService)
    if (tilSteg == StegType.DISTRIBUER_VEDTAKSBREV) return behandlingEtterDistribuertVedtak

    return stegService.håndterFerdigstillBehandling(behandlingEtterDistribuertVedtak)
}

private fun håndterSøknadSteg(
    stegService: StegService,
    behandling: Behandling,
    søkerFnr: String,
    barnasIdenter: List<String>,
    behandlingUnderkategori: BehandlingUnderkategori,
) = stegService.håndterSøknad(
    behandling = behandling,
    restRegistrerSøknad = RestRegistrerSøknad(
        søknad = lagSøknadDTO(
            søkerIdent = søkerFnr,
            barnasIdenter = barnasIdenter,
            underkategori = behandlingUnderkategori,
        ),
        bekreftEndringerViaFrontend = true,
    ),
)

private fun håndterSendtTilBeslutterSteg(
    behandlingEtterSimuleringSteg: Behandling,
    vedtakService: VedtakService,
    vedtaksperiodeService: VedtaksperiodeService,
    stegService: StegService,
    persongrunnlagService: PersongrunnlagService,
    andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    endretUtbetalingAndelHentOgPersisterService: EndretUtbetalingAndelHentOgPersisterService,
    sanityBegrunnelser: Map<Standardbegrunnelse, SanityBegrunnelse>,
    vilkårsvurdering: Vilkårsvurdering,
): Behandling {
    val andelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService
        .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId = behandlingEtterSimuleringSteg.id)

    val persongrunnlag =
        persongrunnlagService.hentAktivThrows(behandlingEtterSimuleringSteg.id)

    val endredeUtbetalingAndeler = endretUtbetalingAndelHentOgPersisterService.hentForBehandling(
        behandlingEtterSimuleringSteg.id,
    )
    leggTilAlleGyldigeBegrunnelserPåVedtaksperiodeIBehandling(
        behandling = behandlingEtterSimuleringSteg,
        vedtakService = vedtakService,
        vedtaksperiodeService = vedtaksperiodeService,
        personopplysningGrunnlag = persongrunnlag,
        andelerTilkjentYtelse = andelerTilkjentYtelse,
        endredeUtbetalingAndeler = endredeUtbetalingAndeler,
        sanityBegrunnelser = sanityBegrunnelser,
        vilkårsvurdering = vilkårsvurdering,
    )
    val behandlingEtterSendTilBeslutter = stegService.håndterSendTilBeslutter(behandlingEtterSimuleringSteg, "1234")
    return behandlingEtterSendTilBeslutter
}

private fun håndterDistribuertVedtakSteg(
    stegService: StegService,
    behandling: Behandling,
    søkerFnr: String,
    brevmalService: BrevmalService,
): Behandling {
    val behandlingEtterDistribuertVedtak =
        stegService.håndterDistribuerVedtaksbrev(
            behandling,
            DistribuerDokumentDTO(
                behandlingId = behandling.id,
                journalpostId = "1234",
                personEllerInstitusjonIdent = søkerFnr,
                brevmal = brevmalService.hentBrevmal(behandling),
                erManueltSendt = false,
            ),
        )
    return behandlingEtterDistribuertVedtak
}

private fun håndterJournalførtVedtakSteg(
    stegService: StegService,
    behandlingEtterIverksetteMotTilbake: Behandling,
    vedtakService: VedtakService,
): Behandling {
    val vedtak = vedtakService.hentAktivForBehandling(behandlingEtterIverksetteMotTilbake.id)
    return stegService.håndterJournalførVedtaksbrev(
        behandlingEtterIverksetteMotTilbake,
        JournalførVedtaksbrevDTO(
            vedtakId = vedtak!!.id,
            task = Task(type = JournalførVedtaksbrevTask.TASK_STEP_TYPE, payload = ""),
        ),
    )
}

private fun håndterStatusFraOppdragSteg(
    stegService: StegService,
    behandlingEtterIverksetteVedtak: Behandling,
    søkerFnr: String,
    vedtakService: VedtakService,
): Behandling {
    val vedtak = vedtakService.hentAktivForBehandling(behandlingEtterIverksetteVedtak.id)
    return stegService.håndterStatusFraØkonomi(
        behandlingEtterIverksetteVedtak,
        StatusFraOppdragMedTask(
            statusFraOppdragDTO = StatusFraOppdragDTO(
                fagsystem = FAGSYSTEM,
                personIdent = søkerFnr,
                aktørId = behandlingEtterIverksetteVedtak.fagsak.aktør.aktørId,
                behandlingsId = behandlingEtterIverksetteVedtak.id,
                vedtaksId = vedtak!!.id,
            ),
            task = Task(type = StatusFraOppdragTask.TASK_STEP_TYPE, payload = ""),
        ),
    )
}

private fun håndterIverksetteVedtakSteg(
    stegService: StegService,
    behandlingEtterBeslutteVedtak: Behandling,
    vedtakService: VedtakService,
): Behandling {
    val vedtak = vedtakService.hentAktivForBehandling(behandlingEtterBeslutteVedtak.id)
    return stegService.håndterIverksettMotØkonomi(
        behandlingEtterBeslutteVedtak,
        IverksettingTaskDTO(
            behandlingsId = behandlingEtterBeslutteVedtak.id,
            vedtaksId = vedtak!!.id,
            saksbehandlerId = "System",
            personIdent = behandlingEtterBeslutteVedtak.fagsak.aktør.aktivFødselsnummer(),
        ),
    )
}

private fun håndterBeslutteVedtakSteg(
    stegService: StegService,
    behandlingEtterSendTilBeslutter: Behandling,
): Behandling {
    val behandlingEtterBeslutteVedtak =
        stegService.håndterBeslutningForVedtak(
            behandlingEtterSendTilBeslutter,
            RestBeslutningPåVedtak(beslutning = Beslutning.GODKJENT),
        )
    return behandlingEtterBeslutteVedtak
}

private fun hånderSilmuleringssteg(
    stegService: StegService,
    behandlingEtterBehandlingsresultat: Behandling,
): Behandling {
    return stegService.håndterVurderTilbakekreving(
        behandlingEtterBehandlingsresultat,
        if (behandlingEtterBehandlingsresultat.resultat != Behandlingsresultat.FORTSATT_INNVILGET) {
            RestTilbakekreving(
                valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                begrunnelse = "Begrunnelse",
            )
        } else {
            null
        },
    )
}

private fun håndterVilkårsvurderingSteg(
    vilkårsvurderingService: VilkårsvurderingService,
    behandling: Behandling,
    nyVilkårsvurdering: Vilkårsvurdering,
    stegService: StegService,
): Behandling {
    val vilkårsvurderingForBehandling = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)!!
    vilkårsvurderingForBehandling.oppdaterMedDataFra(nyVilkårsvurdering)

    vilkårsvurderingService.oppdater(vilkårsvurderingForBehandling)

    return stegService.håndterVilkårsvurdering(behandling)
}

fun Vilkårsvurdering.oppdaterMedDataFra(vilkårsvurdering: Vilkårsvurdering) {
    this.personResultater.forEach { personResultatSomSkalOppdateres ->
        val nyttPersonresultat =
            vilkårsvurdering.personResultater.find { it.aktør.aktørId == personResultatSomSkalOppdateres.aktør.aktørId }!!

        personResultatSomSkalOppdateres.vilkårResultater.forEach { vilkårResultatSomSkalOppdateres ->
            val nyttVilkårResultat = nyttPersonresultat.vilkårResultater
                .find { it.vilkårType == vilkårResultatSomSkalOppdateres.vilkårType }
                ?: error(
                    "Fant ikke ${vilkårResultatSomSkalOppdateres.vilkårType} i vilkårene som ble sendt med for " +
                        "${personResultatSomSkalOppdateres.aktør.aktivFødselsnummer()}.",
                )

            vilkårResultatSomSkalOppdateres.resultat = nyttVilkårResultat.resultat
            vilkårResultatSomSkalOppdateres.periodeFom = nyttVilkårResultat.periodeFom
            vilkårResultatSomSkalOppdateres.periodeTom = nyttVilkårResultat.periodeTom
        }
    }
}

fun leggTilAlleGyldigeBegrunnelserPåVedtaksperiodeIBehandling(
    behandling: Behandling,
    vedtakService: VedtakService,
    vedtaksperiodeService: VedtaksperiodeService,
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    endredeUtbetalingAndeler: List<EndretUtbetalingAndel>,
    sanityBegrunnelser: Map<Standardbegrunnelse, SanityBegrunnelse>,
    vilkårsvurdering: Vilkårsvurdering,
) {
    val aktivtVedtak = vedtakService.hentAktivForBehandling(behandling.id)!!

    val perisisterteVedtaksperioder =
        vedtaksperiodeService.hentPersisterteVedtaksperioder(aktivtVedtak)

    val vedtaksperiode = perisisterteVedtaksperioder.first()

    val utvidetVedtaksperiodeMedBegrunnelser = vedtaksperiode.tilUtvidetVedtaksperiodeMedBegrunnelser(
        personopplysningGrunnlag = personopplysningGrunnlag,
        andelerTilkjentYtelse = andelerTilkjentYtelse,
    )

    val aktørerMedUtbetaling =
        utvidetVedtaksperiodeMedBegrunnelser
            .utbetalingsperiodeDetaljer
            .map { personMedUtbetaling ->
                personopplysningGrunnlag.søkerOgBarn.find {
                    it.aktør.aktivFødselsnummer() == personMedUtbetaling.person.personIdent
                }!!.aktør
            }

    val gyldigebegrunnelser = hentGyldigeBegrunnelserForVedtaksperiodeMinimert(
        minimertVedtaksperiode = utvidetVedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(),
        sanityBegrunnelser = sanityBegrunnelser,
        minimertePersoner = personopplysningGrunnlag.tilMinimertePersoner(),
        minimertePersonresultater = vilkårsvurdering.personResultater
            .map { it.tilMinimertPersonResultat() },
        aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
        minimerteEndredeUtbetalingAndeler = endredeUtbetalingAndeler
            .map { it.tilMinimertEndretUtbetalingAndel() },
        erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak(
            andelerTilkjentYtelse,
            utvidetVedtaksperiodeMedBegrunnelser.fom,
        ),
        ytelserForSøkerForrigeMåned = hentYtelserForSøkerForrigeMåned(
            andelerTilkjentYtelse,
            utvidetVedtaksperiodeMedBegrunnelser,
        ),
        ytelserForrigePerioder = andelerTilkjentYtelse.filter {
            ytelseErFraForrigePeriode(
                it,
                utvidetVedtaksperiodeMedBegrunnelser,
            )
        },
    )

    vedtaksperiodeService.oppdaterVedtaksperiodeMedStandardbegrunnelser(
        vedtaksperiodeId = vedtaksperiode.id,
        standardbegrunnelserFraFrontend = gyldigebegrunnelser.toList(),
        eøsStandardbegrunnelserFraFrontend = emptyList(),
    )
}
