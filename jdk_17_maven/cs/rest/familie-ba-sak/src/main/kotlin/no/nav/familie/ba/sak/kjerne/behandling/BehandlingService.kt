package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.bestemKategoriVedOpprettelse
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.bestemUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingMigreringsinfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingMigreringsinfoRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus.AVSLUTTET
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus.FATTER_VEDTAK
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.initStatus
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatValideringUtils
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.logg.BehandlingLoggRequest
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.FØRSTE_STEG
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class BehandlingService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingstemaService: BehandlingstemaService,
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService,
    private val behandlingMigreringsinfoRepository: BehandlingMigreringsinfoRepository,
    private val behandlingMetrikker: BehandlingMetrikker,
    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
    private val fagsakRepository: FagsakRepository,
    private val vedtakRepository: VedtakRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val loggService: LoggService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val infotrygdService: InfotrygdService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val taskRepository: TaskRepositoryWrapper,
    private val vilkårsvurderingService: VilkårsvurderingService,
) {

    @Transactional
    fun opprettBehandling(nyBehandling: NyBehandling): Behandling {
        val fagsak = fagsakRepository.finnFagsak(nyBehandling.fagsakId) ?: throw FunksjonellFeil(
            melding = "Kan ikke lage behandling på person. Fant ikke fagsak ${nyBehandling.fagsakId}",
            frontendFeilmelding = "Kan ikke lage behandling på person. Fant ikke fagsak.",
        )

        val aktivBehandling = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId = fagsak.id)
        val sisteBehandlingSomErVedtatt =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = fagsak.id)

        return if (aktivBehandling == null || aktivBehandling.status == AVSLUTTET) {
            val kategori = bestemKategoriVedOpprettelse(
                overstyrtKategori = nyBehandling.kategori,
                behandlingType = nyBehandling.behandlingType,
                behandlingÅrsak = nyBehandling.behandlingÅrsak,
                kategoriFraLøpendeBehandling = behandlingstemaService.hentLøpendeKategori(fagsak.id),
            )

            val underkategori = bestemUnderkategori(
                overstyrtUnderkategori = nyBehandling.underkategori,
                underkategoriFraLøpendeBehandling = behandlingstemaService.hentLøpendeUnderkategori(fagsakId = fagsak.id),
                underkategoriFraInneværendeBehandling = behandlingstemaService.hentUnderkategoriFraInneværendeBehandling(
                    fagsak.id,
                ),
            )

            val behandling = Behandling(
                fagsak = fagsak,
                opprettetÅrsak = nyBehandling.behandlingÅrsak,
                type = nyBehandling.behandlingType,
                kategori = kategori,
                underkategori = underkategori,
                skalBehandlesAutomatisk = nyBehandling.skalBehandlesAutomatisk,
            )
                .initBehandlingStegTilstand()

            behandling.validerBehandlingstype(
                sisteBehandlingSomErVedtatt = sisteBehandlingSomErVedtatt,
            )
            val lagretBehandling = lagreNyOgDeaktiverGammelBehandling(behandling).also {
                if (nyBehandling.søknadMottattDato != null) {
                    behandlingSøknadsinfoService.lagreNedSøknadsinfo(
                        mottattDato = nyBehandling.søknadMottattDato,
                        søknadsinfo = nyBehandling.søknadsinfo,
                        behandling = behandling,
                    )
                }
                saksstatistikkEventPublisher.publiserBehandlingsstatistikk(it.id)
            }
            opprettOgInitierNyttVedtakForBehandling(behandling = lagretBehandling)

            loggService.opprettBehandlingLogg(
                BehandlingLoggRequest(behandling = lagretBehandling, barnasIdenter = nyBehandling.barnasIdenter),
            )
            if (lagretBehandling.opprettBehandleSakOppgave()) {
                /**
                 * Oppretter oppgave via task slik at dersom noe feiler i forbindelse med opprettelse
                 * av behandling så rulles også tasken tilbake og vi forhindrer å opprette oppgave
                 */
                taskRepository.save(
                    OpprettOppgaveTask.opprettTask(
                        behandlingId = lagretBehandling.id,
                        oppgavetype = Oppgavetype.BehandleSak,
                        fristForFerdigstillelse = LocalDate.now(),
                        tilordnetRessurs = nyBehandling.navIdent,
                    ),
                )
            }

            lagretBehandling
        } else if (aktivBehandling.steg < StegType.BESLUTTE_VEDTAK) {
            aktivBehandling.leggTilBehandlingStegTilstand(FØRSTE_STEG)
            aktivBehandling.status = initStatus()

            behandlingHentOgPersisterService.lagreEllerOppdater(aktivBehandling)
        } else {
            throw FunksjonellFeil(
                melding = "Kan ikke lage ny behandling. Fagsaken har en aktiv behandling som ikke er ferdigstilt.",
                frontendFeilmelding = "Kan ikke lage ny behandling. Fagsaken har en aktiv behandling som ikke er ferdigstilt.",
            )
        }
    }

    fun nullstillEndringstidspunkt(behandlingId: Long) {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId = behandlingId)
        behandling.overstyrtEndringstidspunkt = null
        behandlingHentOgPersisterService.lagreEllerOppdater(behandling = behandling, sendTilDvh = false)
    }

    @Transactional
    fun opprettOgInitierNyttVedtakForBehandling(
        behandling: Behandling,
        kopierVedtakBegrunnelser: Boolean = false,
        begrunnelseVilkårPekere: List<OriginalOgKopiertVilkårResultat> = emptyList(),
    ) {
        behandling.steg.takeUnless { it !== StegType.BESLUTTE_VEDTAK && it !== StegType.REGISTRERE_PERSONGRUNNLAG }
            ?: error("Forsøker å initiere vedtak på steg ${behandling.steg}")

        val deaktivertVedtak =
            vedtakRepository.findByBehandlingAndAktivOptional(behandlingId = behandling.id)
                ?.let { vedtakRepository.saveAndFlush(it.also { it.aktiv = false }) }

        val nyttVedtak = Vedtak(
            behandling = behandling,
            vedtaksdato = if (behandling.skalBehandlesAutomatisk) LocalDateTime.now() else null,
        )

        if (kopierVedtakBegrunnelser && deaktivertVedtak != null) {
            vedtaksperiodeService.kopierOverVedtaksperioder(
                deaktivertVedtak = deaktivertVedtak,
                aktivtVedtak = nyttVedtak,
            )
        }

        vedtakRepository.save(nyttVedtak)
    }

    fun omgjørTilManuellBehandling(behandling: Behandling): Behandling {
        if (!behandling.skalBehandlesAutomatisk) return behandling

        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} omgjør automatisk behandling $behandling til manuell.")
        behandling.skalBehandlesAutomatisk = false
        return behandlingHentOgPersisterService.lagreEllerOppdater(behandling = behandling, sendTilDvh = true)
    }

    @Transactional
    fun lagreNyOgDeaktiverGammelBehandling(behandling: Behandling): Behandling {
        val aktivBehandling = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId = behandling.fagsak.id)

        if (aktivBehandling != null) {
            behandlingHentOgPersisterService.lagreOgFlush(aktivBehandling.also { it.aktiv = false })
            saksstatistikkEventPublisher.publiserBehandlingsstatistikk(aktivBehandling.id)
        } else if (harAktivInfotrygdSak(behandling)) {
            throw FunksjonellFeil(
                "Kan ikke lage behandling på person med aktiv sak i Infotrygd",
                "Kan ikke lage behandling på person med aktiv sak i Infotrygd",
            )
        }

        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter behandling $behandling")
        return behandlingHentOgPersisterService.lagreEllerOppdater(behandling, false).also {
            arbeidsfordelingService.fastsettBehandlendeEnhet(
                it,
                behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(it.fagsak.id),
            )
            if (it.versjon == 0L) {
                behandlingMetrikker.tellNøkkelTallVedOpprettelseAvBehandling(it)
            }
        }
    }

    fun harAktivInfotrygdSak(behandling: Behandling): Boolean {
        val søkerIdenter = behandling.fagsak.aktør.personidenter.map { it.fødselsnummer }
        return infotrygdService.harÅpenSakIInfotrygd(søkerIdenter) ||
            !behandling.erMigrering() && infotrygdService.harLøpendeSakIInfotrygd(søkerIdenter)
    }

    fun sendBehandlingTilBeslutter(behandling: Behandling) {
        oppdaterStatusPåBehandling(behandlingId = behandling.id, status = FATTER_VEDTAK)
    }

    fun oppdaterStatusPåBehandling(behandlingId: Long, status: BehandlingStatus): Behandling {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} endrer status på behandling $behandlingId fra ${behandling.status} til $status")

        behandling.status = status
        return behandlingHentOgPersisterService.lagreEllerOppdater(behandling)
    }

    fun oppdaterBehandlingsresultat(behandlingId: Long, resultat: Behandlingsresultat): Behandling {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        BehandlingsresultatValideringUtils.validerBehandlingsresultat(behandling, resultat)

        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} endrer resultat på behandling $behandlingId fra ${behandling.resultat} til $resultat")
        loggService.opprettVilkårsvurderingLogg(
            behandling = behandling,
            forrigeBehandlingsresultat = behandling.resultat,
            nyttBehandlingsresultat = resultat,
        )

        behandling.resultat = resultat
        return behandlingHentOgPersisterService.lagreEllerOppdater(behandling)
    }

    fun leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
        behandlingId: Long,
        steg: StegType,
    ): Behandling {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        behandling.leggTilBehandlingStegTilstand(steg)

        return behandlingHentOgPersisterService.lagreEllerOppdater(behandling)
    }

    fun harBehandlingsårsakAlleredeKjørt(
        fagsakId: Long,
        behandlingÅrsak: BehandlingÅrsak,
        måned: YearMonth,
    ): Boolean {
        return Behandlingutils.harBehandlingsårsakAlleredeKjørt(
            behandlinger = behandlingHentOgPersisterService.hentBehandlinger(fagsakId = fagsakId),
            behandlingÅrsak = behandlingÅrsak,
            måned = måned,
        )
    }

    @Transactional
    fun lagreNedMigreringsdato(migreringsdato: LocalDate, behandling: Behandling) {
        val forrigeMigreringsdato: YearMonth? =
            behandlingMigreringsinfoRepository
                .finnSisteMigreringsdatoPåFagsak(fagsakId = behandling.fagsak.id)
                ?.toYearMonth()
                ?: behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = behandling.fagsak.id)
                    ?.takeIf { it.erMigrering() }?.let { // fordi migreringsdato kun kan lagret for migreringsbehandling
                        vilkårsvurderingService.hentTidligsteVilkårsvurderingKnyttetTilMigrering(
                            behandlingId = it.id,
                        )
                    }

        if (behandling.erManuellMigreringForEndreMigreringsdato() &&
            forrigeMigreringsdato != null &&
            migreringsdato.toYearMonth().isSameOrAfter(forrigeMigreringsdato)
        ) {
            throw FunksjonellFeil("Migreringsdatoen du har lagt inn er lik eller senere enn eksisterende migreringsdato. Du må velge en tidligere migreringsdato for å fortsette.")
        }

        val behandlingMigreringsinfo =
            BehandlingMigreringsinfo(behandling = behandling, migreringsdato = migreringsdato)
        behandlingMigreringsinfoRepository.save(behandlingMigreringsinfo)
    }

    fun hentMigreringsdatoIBehandling(behandlingId: Long): LocalDate? {
        return behandlingMigreringsinfoRepository.findByBehandlingId(behandlingId)?.migreringsdato
    }

    fun hentMigreringsdatoPåFagsak(fagsakId: Long): LocalDate? {
        return behandlingMigreringsinfoRepository.finnSisteMigreringsdatoPåFagsak(fagsakId)
    }

    @Transactional
    fun deleteMigreringsdatoVedHenleggelse(behandlingId: Long) {
        behandlingMigreringsinfoRepository.findByBehandlingId(behandlingId)
            ?.let { behandlingMigreringsinfoRepository.delete(it) }
    }

    fun erLøpende(behandling: Behandling): Boolean =
        andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.id)
            .any { it.erLøpende() }

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
    }
}

typealias OriginalOgKopiertVilkårResultat = Pair<VilkårResultat?, VilkårResultat?>
