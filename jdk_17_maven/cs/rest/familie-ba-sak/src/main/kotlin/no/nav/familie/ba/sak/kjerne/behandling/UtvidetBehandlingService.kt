package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.VergeInfo
import no.nav.familie.ba.sak.ekstern.restDomene.tilDto
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestBehandlingStegTilstand
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestFødselshendelsefiltreringResultat
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestKompetanse
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestKorrigertEtterbetaling
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestKorrigertVedtak
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPersonerMedAndeler
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestSettPåVent
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestTotrinnskontroll
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestValutakurs
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestVedtak
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene.FødselshendelsefiltreringResultatRepository
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilRestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.korrigertetterbetaling.KorrigertEtterbetalingService
import no.nav.familie.ba.sak.kjerne.korrigertvedtak.KorrigertVedtakService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.feilutbetaltValuta.FeilutbetaltValutaService
import no.nav.familie.ba.sak.kjerne.vedtak.refusjonEøs.RefusjonEøsService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.springframework.stereotype.Service

@Service
class UtvidetBehandlingService(
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val persongrunnlagService: PersongrunnlagService,
    private val behandlingService: BehandlingService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val vedtakRepository: VedtakRepository,
    private val totrinnskontrollRepository: TotrinnskontrollRepository,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val tilbakekrevingRepository: TilbakekrevingRepository,
    private val fødselshendelsefiltreringResultatRepository: FødselshendelsefiltreringResultatRepository,
    private val settPåVentService: SettPåVentService,
    private val kompetanseRepository: KompetanseRepository,
    private val valutakursRepository: ValutakursRepository,
    private val utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository,
    private val korrigertEtterbetalingService: KorrigertEtterbetalingService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val korrigertVedtakService: KorrigertVedtakService,
    private val feilutbetaltValutaService: FeilutbetaltValutaService,
    private val brevmottakerService: BrevmottakerService,
    private val refusjonEøsService: RefusjonEøsService,
) {
    fun lagRestUtvidetBehandling(behandlingId: Long): RestUtvidetBehandling {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId = behandlingId)

        val søknadsgrunnlag = søknadGrunnlagService.hentAktiv(behandlingId = behandling.id)
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id)
        val personer = personopplysningGrunnlag?.søkerOgBarn

        val arbeidsfordeling = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = behandling.id)

        val vedtak = vedtakRepository.findByBehandlingAndAktivOptional(behandling.id)

        val personResultater = vilkårsvurderingService.hentAktivForBehandling(behandling.id)?.personResultater

        val andelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(listOf(behandling.id))

        val totrinnskontroll =
            totrinnskontrollRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)

        val tilbakekreving = tilbakekrevingRepository.findByBehandlingId(behandling.id)

        val kompetanser: Collection<Kompetanse> = kompetanseRepository.finnFraBehandlingId(behandlingId)

        val valutakurser = valutakursRepository.finnFraBehandlingId(behandlingId)

        val utenlandskePeriodebeløp = utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId)

        val endreteUtbetalingerMedAndeler = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandlingId)

        val feilutbetaltValuta = feilutbetaltValutaService.hentFeilutbetaltValutaPerioder(behandlingId)

        val refusjonEøs = refusjonEøsService.hentRefusjonEøsPerioder(behandlingId)

        val brevmottakere = brevmottakerService.hentRestBrevmottakere(behandlingId)

        return RestUtvidetBehandling(
            behandlingId = behandling.id,
            steg = behandling.steg,
            stegTilstand = behandling.behandlingStegTilstand.map { it.tilRestBehandlingStegTilstand() },
            status = behandling.status,
            resultat = behandling.resultat,
            skalBehandlesAutomatisk = behandling.skalBehandlesAutomatisk,
            type = behandling.type,
            kategori = behandling.kategori,
            underkategori = behandling.underkategori.tilDto(),
            årsak = behandling.opprettetÅrsak,
            opprettetTidspunkt = behandling.opprettetTidspunkt,
            endretAv = behandling.endretAv,
            arbeidsfordelingPåBehandling = arbeidsfordeling.tilRestArbeidsfordelingPåBehandling(),
            søknadsgrunnlag = søknadsgrunnlag?.hentSøknadDto(),
            personer = personer?.map { persongrunnlagService.mapTilRestPersonMedStatsborgerskapLand(it) }
                ?: emptyList(),
            personResultater = personResultater?.map { it.tilRestPersonResultat() } ?: emptyList(),
            fødselshendelsefiltreringResultater = fødselshendelsefiltreringResultatRepository.finnFødselshendelsefiltreringResultater(
                behandlingId = behandling.id,
            ).map { it.tilRestFødselshendelsefiltreringResultat() },
            utbetalingsperioder = vedtaksperiodeService.hentUtbetalingsperioder(behandling, personopplysningGrunnlag),
            personerMedAndelerTilkjentYtelse = personopplysningGrunnlag?.tilRestPersonerMedAndeler(andelerTilkjentYtelse)
                ?: emptyList(),
            endretUtbetalingAndeler = endreteUtbetalingerMedAndeler
                .map { it.tilRestEndretUtbetalingAndel() },
            tilbakekreving = tilbakekreving?.tilRestTilbakekreving(),
            vedtak = vedtak?.tilRestVedtak(),
            kompetanser = kompetanser.map { it.tilRestKompetanse() }.sortedByDescending { it.fom },
            totrinnskontroll = totrinnskontroll?.tilRestTotrinnskontroll(),
            aktivSettPåVent = settPåVentService.finnAktivSettPåVentPåBehandling(behandlingId = behandlingId)
                ?.tilRestSettPåVent(),
            migreringsdato = behandlingService.hentMigreringsdatoIBehandling(behandlingId = behandlingId),
            valutakurser = valutakurser.map { it.tilRestValutakurs() },
            utenlandskePeriodebeløp = utenlandskePeriodebeløp.map { it.tilRestUtenlandskPeriodebeløp() },
            verge = behandling.verge?.let { VergeInfo(it.ident) },
            korrigertEtterbetaling = korrigertEtterbetalingService.finnAktivtKorrigeringPåBehandling(behandlingId)
                ?.tilRestKorrigertEtterbetaling(),
            korrigertVedtak = korrigertVedtakService.finnAktivtKorrigertVedtakPåBehandling(behandlingId)
                ?.tilRestKorrigertVedtak(),
            feilutbetaltValuta = feilutbetaltValuta,
            brevmottakere = brevmottakere,
            refusjonEøs = refusjonEøs,
        )
    }
}
