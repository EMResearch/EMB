package no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker

import no.nav.familie.kontrakter.felles.dokdist.AdresseType
import no.nav.familie.kontrakter.felles.dokdist.AdresseType.norskPostadresse
import no.nav.familie.kontrakter.felles.dokdist.AdresseType.utenlandskPostadresse
import no.nav.familie.kontrakter.felles.dokdist.ManuellAdresse
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE
import no.nav.familie.tilbake.api.dto.ManuellBrevmottakerRequestDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakService
import no.nav.familie.tilbake.behandling.ValiderBrevmottakerService
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.integration.pdl.PdlClient
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Service
class ManuellBrevmottakerService(
    private val manuellBrevmottakerRepository: ManuellBrevmottakerRepository,
    private val historikkService: HistorikkService,
    private val behandlingRepository: BehandlingRepository,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val fagsakService: FagsakService,
    private val pdlClient: PdlClient,
    private val integrasjonerClient: IntegrasjonerClient,
    private val validerBrevmottakerService: ValiderBrevmottakerService,
) {

    @Transactional
    fun leggTilBrevmottaker(behandlingId: UUID, requestDto: ManuellBrevmottakerRequestDto): UUID {
        val navnFraRegister: String? = hentPersonEllerOrganisasjonNavnFraRegister(requestDto, behandlingId)
        val manuellBrevmottaker = ManuellBrevmottakerMapper.tilDomene(behandlingId, requestDto, navnFraRegister)
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        validerBrevmottakerService.validerAtBehandlingenIkkeInneholderStrengtFortroligPerson(behandlingId = behandling.id, fagsakId = behandling.fagsakId)
        val id = manuellBrevmottakerRepository.insert(manuellBrevmottaker).id
        historikkService.lagHistorikkinnslag(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BREVMOTTAKER_LAGT_TIL,
            aktør = Aktør.SAKSBEHANDLER,
            opprettetTidspunkt = LocalDateTime.now(),
            beskrivelse = lagHistorikkBeskrivelseForBrevmottaker(manuellBrevmottaker),
            tittel = "${manuellBrevmottaker.type.visningsnavn} er lagt til som brevmottaker",
        )
        return id
    }

    fun hentBrevmottakere(behandlingId: UUID) = manuellBrevmottakerRepository.findByBehandlingId(behandlingId)

    @Transactional
    fun oppdaterBrevmottaker(
        behandlingId: UUID,
        manuellBrevmottakerId: UUID,
        manuellBrevmottakerRequestDto: ManuellBrevmottakerRequestDto,
    ) {
        val manuellBrevmottaker = manuellBrevmottakerRepository.findById(manuellBrevmottakerId).getOrNull()
            ?: throw Feil("Finnes ikke brevmottakere med id=$manuellBrevmottakerId")

        val oppdatertBrevmottaker = manuellBrevmottaker.copy(
            type = manuellBrevmottakerRequestDto.type,
            navn = hentPersonEllerOrganisasjonNavnFraRegister(manuellBrevmottakerRequestDto, behandlingId)
                ?: manuellBrevmottakerRequestDto.navn,
            ident = manuellBrevmottakerRequestDto.personIdent,
            orgNr = manuellBrevmottakerRequestDto.organisasjonsnummer,
            adresselinje1 = manuellBrevmottakerRequestDto.manuellAdresseInfo?.adresselinje1,
            adresselinje2 = manuellBrevmottakerRequestDto.manuellAdresseInfo?.adresselinje2,
            postnummer = manuellBrevmottakerRequestDto.manuellAdresseInfo?.postnummer,
            poststed = manuellBrevmottakerRequestDto.manuellAdresseInfo?.poststed,
            landkode = manuellBrevmottakerRequestDto.manuellAdresseInfo?.landkode,
            vergetype = manuellBrevmottakerRequestDto.vergetype,
        )

        val historikkinnslagtittel =
            if (manuellBrevmottaker.type == oppdatertBrevmottaker.type) {
                "${manuellBrevmottaker.type.visningsnavn} er endret"
            } else {
                "${manuellBrevmottaker.type.visningsnavn} er endret til ${oppdatertBrevmottaker.type.visningsnavn}"
            }

        historikkService.lagHistorikkinnslag(
            behandlingId = manuellBrevmottaker.behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BREVMOTTAKER_ENDRET,
            aktør = Aktør.SAKSBEHANDLER,
            opprettetTidspunkt = LocalDateTime.now(),
            beskrivelse = lagHistorikkBeskrivelseForBrevmottaker(oppdatertBrevmottaker),
            tittel = historikkinnslagtittel,
        )

        manuellBrevmottakerRepository.update(oppdatertBrevmottaker)
    }

    @Transactional
    fun fjernBrevmottaker(behandlingId: UUID, manuellBrevmottakerId: UUID) {
        val manuellBrevmottakere = manuellBrevmottakerRepository.findByBehandlingId(behandlingId)
        if (manuellBrevmottakere.none { it.id == manuellBrevmottakerId }) {
            throw Feil("Finnes ikke brevmottakere med id=$manuellBrevmottakerId for behandlingId=$behandlingId")
        }
        fjernBrevmottakerOgLagHistorikkinnslag(
            manuellBrevmottakere.single { it.id == manuellBrevmottakerId },
            behandlingId,
        )
    }

    @Transactional
    fun opprettBrevmottakerSteg(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        validerBrevmottakerStegopprettelse(behandling)
        behandlingskontrollService.behandleBrevmottakerSteg(behandlingId)
    }

    @Transactional
    fun fjernManuelleBrevmottakereOgTilbakeførSteg(behandlingId: UUID) {
        hentBrevmottakere(behandlingId).forEach { manuellBrevmottaker ->
            fjernBrevmottakerOgLagHistorikkinnslag(manuellBrevmottaker, behandlingId)
        }

        behandlingskontrollService.oppdaterBehandlingsstegStatus(
            behandlingId,
            Behandlingsstegsinfo(
                Behandlingssteg.BREVMOTTAKER,
                Behandlingsstegstatus.TILBAKEFØRT,
            ),
        )
        behandlingskontrollService.fortsettBehandling(behandlingId)
    }

    private fun fjernBrevmottakerOgLagHistorikkinnslag(manuellBrevmottaker: ManuellBrevmottaker, behandlingId: UUID) {
        historikkService.lagHistorikkinnslag(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BREVMOTTAKER_FJERNET,
            aktør = Aktør.SAKSBEHANDLER,
            opprettetTidspunkt = LocalDateTime.now(),
            beskrivelse = lagHistorikkBeskrivelseForBrevmottaker(manuellBrevmottaker),
            tittel = "${manuellBrevmottaker.type.visningsnavn} er fjernet som brevmottaker",
        )

        manuellBrevmottakerRepository.deleteById(manuellBrevmottaker.id)
    }

    private fun validerBrevmottakerStegopprettelse(behandling: Behandling) {
        if (behandling.erSaksbehandlingAvsluttet) {
            throw Feil(
                "Behandling med id=${behandling.id} er allerede ferdig behandlet.",
                frontendFeilmelding = "Behandling med id=${behandling.id} er allerede ferdig behandlet.",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        if (behandlingskontrollService.erBehandlingPåVent(behandling.id)) {
            throw Feil(
                "Behandling med id=${behandling.id} er på vent.",
                frontendFeilmelding = "Behandling med id=${behandling.id} er på vent.",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        validerBrevmottakerService.validerAtBehandlingenIkkeInneholderStrengtFortroligPerson(behandlingId = behandling.id, fagsakId = behandling.fagsakId)
    }

    private fun lagHistorikkBeskrivelseForBrevmottaker(brevmottaker: ManuellBrevmottaker) =
        listOfNotNull(
            brevmottaker.navn,
            brevmottaker.adresselinje1,
            brevmottaker.adresselinje2,
            brevmottaker.postnummer,
            brevmottaker.poststed,
            brevmottaker.landkode,
        ).joinToString(separator = System.lineSeparator())

    private fun hentPersonEllerOrganisasjonNavnFraRegister(
        dto: ManuellBrevmottakerRequestDto,
        behandlingId: UUID,
    ): String? {
        return dto.personIdent?.let {
            pdlClient.hentPersoninfo(
                ident = it,
                fagsystem = fagsakService.finnFagsystemForBehandlingId(behandlingId),
            ).navn
        } ?: dto.organisasjonsnummer?.let {
            if (!integrasjonerClient.validerOrganisasjon(it)) {
                throw Feil(
                    message = "Organisasjon $it er ikke gyldig",
                    frontendFeilmelding = "Organisasjon $it er ikke gyldig",
                )
            }
            integrasjonerClient.hentOrganisasjon(it).navn + if (dto.navn.isNotBlank()) " v/ ${dto.navn}" else ""
        }
    }
}

private fun findAdresseType(brevmottaker: ManuellBrevmottaker): AdresseType {
    return when {
        brevmottaker.landkode == "NO" && brevmottaker.type != BRUKER_MED_UTENLANDSK_ADRESSE -> norskPostadresse
        brevmottaker.landkode != "NO" && brevmottaker.type == BRUKER_MED_UTENLANDSK_ADRESSE -> utenlandskPostadresse
        else -> throw Feil("landkode stemmer ikke overens med type for brevmottaker ${brevmottaker.id}")
    }
}

fun List<ManuellBrevmottaker>.toManuelleAdresser(): List<ManuellAdresse> =
    this.mapNotNull { manuellBrevmottaker ->
        if (manuellBrevmottaker.hasManuellAdresse()) {
            ManuellAdresse(
                adresseType = findAdresseType(manuellBrevmottaker),
                adresselinje1 = manuellBrevmottaker.adresselinje1,
                adresselinje2 = manuellBrevmottaker.adresselinje2,
                postnummer = manuellBrevmottaker.postnummer,
                poststed = manuellBrevmottaker.poststed,
                land = manuellBrevmottaker.landkode!!,
            )
        } else {
            null
        }
    }
