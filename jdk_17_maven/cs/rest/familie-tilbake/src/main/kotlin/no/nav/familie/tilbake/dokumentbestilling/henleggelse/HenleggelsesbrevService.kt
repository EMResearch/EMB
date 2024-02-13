package no.nav.familie.tilbake.dokumentbestilling.henleggelse

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.dokumentbestilling.DistribusjonshåndteringService
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmetadataUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingService
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.dokumentbestilling.fritekstbrev.Fritekstbrevsdata
import no.nav.familie.tilbake.dokumentbestilling.henleggelse.handlebars.dto.Henleggelsesbrevsdokument
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.organisasjon.OrganisasjonService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class HenleggelsesbrevService(
    private val behandlingRepository: BehandlingRepository,
    private val brevsporingService: BrevsporingService,
    private val fagsakRepository: FagsakRepository,
    private val eksterneDataForBrevService: EksterneDataForBrevService,
    private val pdfBrevService: PdfBrevService,
    private val organisasjonService: OrganisasjonService,
    private val distribusjonshåndteringService: DistribusjonshåndteringService,
    private val brevmetadataUtil: BrevmetadataUtil,
) {

    fun sendHenleggelsebrev(behandlingId: UUID, fritekst: String?, brevmottager: Brevmottager? = null) {
        val behandling: Behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        if (brevmottager == null) {
            distribusjonshåndteringService.sendBrev(behandling, Brevtype.HENLEGGELSE) { brevmottaker, brevmetadata ->
                val henleggelsesbrevSamletInfo = lagHenleggelsebrev(behandling, fagsak, fritekst, brevmottaker, brevmetadata)
                val fritekstbrevData: Fritekstbrevsdata =
                    if (Behandlingstype.TILBAKEKREVING == behandling.type) {
                        lagHenleggelsesbrev(henleggelsesbrevSamletInfo)
                    } else {
                        lagRevurderingHenleggelsebrev(henleggelsesbrevSamletInfo)
                    }
                Brevdata(
                    mottager = brevmottaker,
                    metadata = fritekstbrevData.brevmetadata,
                    overskrift = fritekstbrevData.overskrift,
                    brevtekst = fritekstbrevData.brevtekst,
                )
            }
        } else {
            val henleggelsesbrevSamletInfo = lagHenleggelsebrev(behandling, fagsak, fritekst, brevmottager)
            val fritekstbrevData: Fritekstbrevsdata =
                if (Behandlingstype.TILBAKEKREVING == behandling.type) {
                    lagHenleggelsesbrev(henleggelsesbrevSamletInfo)
                } else {
                    lagRevurderingHenleggelsebrev(henleggelsesbrevSamletInfo)
                }
            pdfBrevService.sendBrev(
                behandling,
                fagsak,
                Brevtype.HENLEGGELSE,
                Brevdata(
                    mottager = brevmottager,
                    metadata = fritekstbrevData.brevmetadata,
                    overskrift = fritekstbrevData.overskrift,
                    brevtekst = fritekstbrevData.brevtekst,
                ),
            )
        }
    }

    fun hentForhåndsvisningHenleggelsesbrev(behandlingUuid: UUID, fritekst: String?): ByteArray {
        val behandling: Behandling = behandlingRepository.findByIdOrThrow(behandlingUuid)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val (metadata, brevmottager) =
            brevmetadataUtil.lagBrevmetadataForMottakerTilForhåndsvisning(behandling.id)
        val henleggelsesbrevSamletInfo = lagHenleggelsebrev(behandling, fagsak, fritekst, brevmottager, metadata)
        val fritekstbrevData: Fritekstbrevsdata =
            if (Behandlingstype.TILBAKEKREVING == behandling.type) {
                lagHenleggelsesbrev(henleggelsesbrevSamletInfo)
            } else {
                lagRevurderingHenleggelsebrev(henleggelsesbrevSamletInfo)
            }
        return pdfBrevService.genererForhåndsvisning(
            Brevdata(
                mottager = brevmottager,
                metadata = fritekstbrevData.brevmetadata,
                overskrift = fritekstbrevData.overskrift,
                brevtekst = fritekstbrevData.brevtekst,
            ),
        )
    }

    private fun lagHenleggelsebrev(
        behandling: Behandling,
        fagsak: Fagsak,
        fritekst: String?,
        brevmottager: Brevmottager,
        forhåndsgenerertMetadata: Brevmetadata? = null,
    ): Henleggelsesbrevsdokument {
        val brevSporing = brevsporingService.finnSisteVarsel(behandling.id)
        if (Behandlingstype.TILBAKEKREVING == behandling.type && brevSporing == null) {
            throw IllegalStateException(
                "Varselbrev er ikke sendt. Kan ikke forhåndsvise/sende " +
                    "henleggelsesbrev for behandlingId=${behandling.id} når varsel ikke er sendt.",
            )
        } else if (Behandlingstype.REVURDERING_TILBAKEKREVING == behandling.type && fritekst.isNullOrEmpty()) {
            throw IllegalStateException(
                "Kan ikke forhåndsvise/sende henleggelsesbrev uten fritekst for " +
                    "Tilbakekreving Revurdering med behandlingsid=${behandling.id}.",
            )
        }

        val ansvarligSaksbehandler = if (behandling.ansvarligSaksbehandler == Constants.BRUKER_ID_VEDTAKSLØSNINGEN) {
            SIGNATUR_AUTOMATISK_HENLEGGELSESBREV
        } else {
            eksterneDataForBrevService.hentPåloggetSaksbehandlernavnMedDefault(behandling.ansvarligSaksbehandler)
        }

        val metadata = forhåndsgenerertMetadata ?: run {
            val personinfo: Personinfo = eksterneDataForBrevService.hentPerson(fagsak.bruker.ident, fagsak.fagsystem)
            val adresseinfo: Adresseinfo = eksterneDataForBrevService.hentAdresse(
                personinfo,
                brevmottager,
                behandling.aktivVerge,
                fagsak.fagsystem,
            )

            val vergenavn: String = BrevmottagerUtil.getVergenavn(behandling.aktivVerge, adresseinfo)
            val gjelderDødsfall = personinfo.dødsdato != null

            Brevmetadata(
                sakspartId = personinfo.ident,
                sakspartsnavn = personinfo.navn,
                finnesVerge = behandling.harVerge,
                vergenavn = vergenavn,
                mottageradresse = adresseinfo,
                behandlendeEnhetId = behandling.behandlendeEnhet,
                behandlendeEnhetsNavn = behandling.behandlendeEnhetsNavn,
                ansvarligSaksbehandler = ansvarligSaksbehandler,
                saksnummer = fagsak.eksternFagsakId,
                språkkode = fagsak.bruker.språkkode,
                ytelsestype = fagsak.ytelsestype,
                gjelderDødsfall = gjelderDødsfall,
                institusjon = fagsak.institusjon?.let {
                    organisasjonService.mapTilInstitusjonForBrevgenerering(it.organisasjonsnummer)
                },
            )
        }

        return Henleggelsesbrevsdokument(
            metadata.copy(
                tittel = TITTEL_HENLEGGELSESBREV,
                behandlingstype = behandling.type,
                ansvarligSaksbehandler = ansvarligSaksbehandler,
            ),
            brevSporing?.sporbar?.opprettetTid?.toLocalDate(),
            fritekst,
        )
    }

    private fun lagHenleggelsesbrev(dokument: Henleggelsesbrevsdokument): Fritekstbrevsdata {
        return Fritekstbrevsdata(
            TekstformatererHenleggelsesbrev.lagOverskrift(dokument.brevmetadata),
            TekstformatererHenleggelsesbrev.lagFritekst(dokument),
            dokument.brevmetadata,
        )
    }

    private fun lagRevurderingHenleggelsebrev(dokument: Henleggelsesbrevsdokument): Fritekstbrevsdata {
        return Fritekstbrevsdata(
            TekstformatererHenleggelsesbrev.lagRevurderingsoverskrift(dokument.brevmetadata),
            TekstformatererHenleggelsesbrev.lagRevurderingsfritekst(dokument),
            dokument.brevmetadata,
        )
    }

    companion object {

        private const val TITTEL_HENLEGGELSESBREV = "Informasjon om at tilbakekrevingssaken er henlagt"
        private const val SIGNATUR_AUTOMATISK_HENLEGGELSESBREV = """
                
Henleggelsen er gjort automatisk. Brevet er derfor ikke underskrevet av saksbehandler."""
    }
}
