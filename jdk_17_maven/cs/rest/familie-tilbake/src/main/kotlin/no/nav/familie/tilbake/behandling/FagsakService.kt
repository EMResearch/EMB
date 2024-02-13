package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.FinnesBehandlingResponse
import no.nav.familie.kontrakter.felles.tilbakekreving.KanBehandlingOpprettesManueltRespons
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.api.dto.FagsakDto
import no.nav.familie.tilbake.behandling.domain.Bruker
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Institusjon
import no.nav.familie.tilbake.behandling.event.EndretPersonIdentEvent
import no.nav.familie.tilbake.behandling.task.OpprettBehandlingManueltTask
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import no.nav.familie.tilbake.organisasjon.OrganisasjonService
import no.nav.familie.tilbake.person.PersonService
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FagsakService(
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val taskService: TaskService,
    private val økonomiXmlMottattRepository: ØkonomiXmlMottattRepository,
    private val personService: PersonService,
    private val organisasjonService: OrganisasjonService,
) {

    fun hentFagsak(fagsakId: UUID): Fagsak {
        return fagsakRepository.findByIdOrThrow(fagsakId)
    }

    @Transactional(readOnly = true)
    fun hentFagsak(fagsystem: Fagsystem, eksternFagsakId: String): FagsakDto {
        val fagsak = fagsakRepository.findByFagsystemAndEksternFagsakId(
            fagsystem = fagsystem,
            eksternFagsakId = eksternFagsakId,
        )
            ?: throw Feil(
                message = "Fagsak finnes ikke for ${fagsystem.navn} og $eksternFagsakId",
                frontendFeilmelding = "Fagsak finnes ikke for ${fagsystem.navn} og $eksternFagsakId",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        val personInfo = personService.hentPersoninfo(
            personIdent = fagsak.bruker.ident,
            fagsystem = fagsak.fagsystem,
        )
        val behandlinger = behandlingRepository.findByFagsakId(fagsakId = fagsak.id)

        return FagsakMapper.tilRespons(
            fagsak = fagsak,
            personinfo = personInfo,
            behandlinger = behandlinger,
            organisasjonService = organisasjonService,
        )
    }

    @Transactional
    fun finnFagsak(fagsystem: Fagsystem, eksternFagsakId: String): Fagsak? {
        return fagsakRepository.findByFagsystemAndEksternFagsakId(
            fagsystem = fagsystem,
            eksternFagsakId = eksternFagsakId,
        )
    }

    @Transactional
    fun finnFagsystem(fagsakId: UUID): Fagsystem {
        return fagsakRepository.findByIdOrThrow(fagsakId).fagsystem
    }

    @Transactional
    fun finnFagsystemForBehandlingId(behandlingId: UUID): Fagsystem {
        return fagsakRepository.finnFagsakForBehandlingId(behandlingId).fagsystem
    }

    @Transactional
    fun opprettFagsak(
        opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest,
        ytelsestype: Ytelsestype,
        fagsystem: Fagsystem,
    ): Fagsak {
        val bruker = Bruker(
            ident = opprettTilbakekrevingRequest.personIdent,
            språkkode = opprettTilbakekrevingRequest.språkkode,
        )
        val institusjon = opprettTilbakekrevingRequest.institusjon?.let {
            Institusjon(
                organisasjonsnummer = it.organisasjonsnummer,
            )
        }
        return fagsakRepository.insert(
            Fagsak(
                bruker = bruker,
                eksternFagsakId = opprettTilbakekrevingRequest.eksternFagsakId,
                ytelsestype = ytelsestype,
                fagsystem = fagsystem,
                institusjon = institusjon,
            ),
        )
    }

    @Transactional(readOnly = true)
    fun finnesÅpenTilbakekrevingsbehandling(fagsystem: Fagsystem, eksternFagsakId: String): FinnesBehandlingResponse {
        val fagsak = fagsakRepository.findByFagsystemAndEksternFagsakId(
            fagsystem = fagsystem,
            eksternFagsakId = eksternFagsakId,
        )
        var finnesÅpenBehandling = false
        if (fagsak != null) {
            finnesÅpenBehandling =
                behandlingRepository.finnÅpenTilbakekrevingsbehandling(
                    ytelsestype = fagsak.ytelsestype,
                    eksternFagsakId = eksternFagsakId,
                ) != null
        }
        return FinnesBehandlingResponse(finnesÅpenBehandling = finnesÅpenBehandling)
    }

    @Transactional(readOnly = true)
    fun hentBehandlingerForFagsak(
        fagsystem: Fagsystem,
        eksternFagsakId: String,
    ): List<no.nav.familie.kontrakter.felles.tilbakekreving.Behandling> {
        val fagsak = fagsakRepository.findByFagsystemAndEksternFagsakId(
            fagsystem = fagsystem,
            eksternFagsakId = eksternFagsakId,
        )

        return fagsak?.let {
            val behandlinger = behandlingRepository.findByFagsakId(fagsakId = fagsak.id)
            behandlinger.map { BehandlingMapper.tilBehandlingerForFagsystem(it) }
        } ?: emptyList()
    }

    @Transactional(readOnly = true)
    fun hentVedtakForFagsak(
        fagsystem: Fagsystem,
        eksternFagsakId: String,
    ): List<no.nav.familie.kontrakter.felles.klage.FagsystemVedtak> {
        val fagsak = fagsakRepository.findByFagsystemAndEksternFagsakId(
            fagsystem = fagsystem,
            eksternFagsakId = eksternFagsakId,
        )

        return fagsak?.let {
            val behandlinger = behandlingRepository.findByFagsakId(fagsakId = fagsak.id)
            BehandlingMapper.tilVedtakForFagsystem(behandlinger)
        } ?: emptyList()
    }

    @Transactional(readOnly = true)
    fun kanBehandlingOpprettesManuelt(
        eksternFagsakId: String,
        ytelsestype: Ytelsestype,
    ): KanBehandlingOpprettesManueltRespons {
        val finnesÅpenTilbakekreving: Boolean =
            behandlingRepository.finnÅpenTilbakekrevingsbehandling(ytelsestype, eksternFagsakId) != null
        if (finnesÅpenTilbakekreving) {
            return KanBehandlingOpprettesManueltRespons(
                kanBehandlingOpprettes = false,
                melding = "Det finnes allerede en åpen tilbakekrevingsbehandling. " +
                    "Den ligger i saksoversikten.",
            )
        }
        val kravgrunnlagene = økonomiXmlMottattRepository.findByEksternFagsakIdAndYtelsestype(eksternFagsakId, ytelsestype)
        if (kravgrunnlagene.isEmpty()) {
            return KanBehandlingOpprettesManueltRespons(
                kanBehandlingOpprettes = false,
                melding = "Det finnes ingen feilutbetaling på saken, så du kan " +
                    "ikke opprette tilbakekrevingsbehandling.",
            )
        }
        val kravgrunnlagsreferanse = kravgrunnlagene.first().referanse
        val harAlledeMottattForespørselen: Boolean =
            taskService.finnTasksMedStatus(
                listOf(
                    Status.UBEHANDLET,
                    Status.BEHANDLER,
                    Status.KLAR_TIL_PLUKK,
                    Status.PLUKKET,
                    Status.FEILET,
                ),
                Pageable.unpaged(),
            )
                .any {
                    OpprettBehandlingManueltTask.TYPE == it.type &&
                        eksternFagsakId == it.metadata.getProperty("eksternFagsakId") &&
                        ytelsestype.kode == it.metadata.getProperty("ytelsestype")
                    kravgrunnlagsreferanse == it.metadata.getProperty("eksternId")
                }

        if (harAlledeMottattForespørselen) {
            return KanBehandlingOpprettesManueltRespons(
                kanBehandlingOpprettes = false,
                melding = "Det finnes allerede en forespørsel om å opprette " +
                    "tilbakekrevingsbehandling. Behandlingen vil snart bli " +
                    "tilgjengelig i saksoversikten. Dersom den ikke dukker opp, " +
                    "ta kontakt med brukerstøtte for å rapportere feilen.",
            )
        }
        return KanBehandlingOpprettesManueltRespons(
            kanBehandlingOpprettes = true,
            kravgrunnlagsreferanse = kravgrunnlagsreferanse,
            melding = "Det er mulig å opprette behandling manuelt.",
        )
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun oppdaterPersonIdent(endretPersonIdentEvent: EndretPersonIdentEvent) {
        val fagsak = fagsakRepository.findByIdOrThrow(endretPersonIdentEvent.fagsakId)
        fagsakRepository.update(
            fagsak.copy(
                bruker = Bruker(
                    ident = endretPersonIdentEvent.source as String,
                    språkkode = fagsak.bruker.språkkode,
                ),
            ),
        )
    }
}
