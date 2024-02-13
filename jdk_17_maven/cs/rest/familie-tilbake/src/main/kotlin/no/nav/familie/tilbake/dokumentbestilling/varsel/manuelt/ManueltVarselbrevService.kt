package no.nav.familie.tilbake.dokumentbestilling.varsel.manuelt

import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.dokumentbestilling.DistribusjonshåndteringService
import no.nav.familie.tilbake.dokumentbestilling.brevmaler.Dokumentmalstype
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmetadataUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.dokumentbestilling.varsel.TekstformatererVarselbrev.lagFritekst
import no.nav.familie.tilbake.dokumentbestilling.varsel.TekstformatererVarselbrev.lagVarselbrevsoverskrift
import no.nav.familie.tilbake.dokumentbestilling.varsel.VarselbrevUtil
import no.nav.familie.tilbake.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ManueltVarselbrevService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val eksterneDataForBrevService: EksterneDataForBrevService,
    private val pdfBrevService: PdfBrevService,
    private val faktaFeilutbetalingService: FaktaFeilutbetalingService,
    private val varselbrevUtil: VarselbrevUtil,
    private val distribusjonshåndteringService: DistribusjonshåndteringService,
    private val brevmetadataUtil: BrevmetadataUtil,
) {

    fun sendManueltVarselBrev(behandling: Behandling, fritekst: String, brevmottager: Brevmottager) {
        sendVarselBrev(behandling, fritekst, brevmottager, false)
    }

    fun sendKorrigertVarselBrev(behandling: Behandling, fritekst: String, brevmottager: Brevmottager) {
        sendVarselBrev(behandling, fritekst, brevmottager, true)
    }

    fun sendVarselbrev(behandling: Behandling, fritekst: String, erKorrigert: Boolean) {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val feilutbetalingsfakta = faktaFeilutbetalingService.hentFaktaomfeilutbetaling(behandling.id)
        val varsletFeilutbetaling = feilutbetalingsfakta.totaltFeilutbetaltBeløp.toLong()

        distribusjonshåndteringService.sendBrev(
            behandling = behandling,
            brevtype = if (erKorrigert) Brevtype.KORRIGERT_VARSEL else Brevtype.VARSEL,
            varsletBeløp = varsletFeilutbetaling,
            fritekst = fritekst,
        ) { brevmottager, brevmetadata ->

            val varselbrevsdokument = lagVarselbrev(
                behandling = behandling,
                fagsak = fagsak,
                brevmottager = brevmottager,
                fritekst = fritekst,
                erKorrigert = erKorrigert,
                feilutbetalingsfakta = feilutbetalingsfakta,
                aktivtVarsel = behandling.aktivtVarsel,
                forhåndsgenerertMetadata = brevmetadata,
            )
            Brevdata(
                mottager = brevmottager,
                overskrift = lagVarselbrevsoverskrift(varselbrevsdokument.brevmetadata, erKorrigert),
                brevtekst = lagFritekst(varselbrevsdokument, erKorrigert),
                metadata = varselbrevsdokument.brevmetadata,
                vedleggHtml = varselbrevUtil.lagVedlegg(varselbrevsdokument, behandling.id),
            )
        }
    }

    fun sendVarselBrev(behandling: Behandling, fritekst: String, brevmottager: Brevmottager, erKorrigert: Boolean) {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val feilutbetalingsfakta = faktaFeilutbetalingService.hentFaktaomfeilutbetaling(behandling.id)
        val varselbrevsdokument =
            lagVarselbrev(fritekst, behandling, fagsak, brevmottager, erKorrigert, feilutbetalingsfakta, behandling.aktivtVarsel)
        val overskrift =
            lagVarselbrevsoverskrift(varselbrevsdokument.brevmetadata, erKorrigert)
        val brevtekst = lagFritekst(varselbrevsdokument, erKorrigert)
        val varsletFeilutbetaling = varselbrevsdokument.beløp
        val vedlegg = varselbrevUtil.lagVedlegg(varselbrevsdokument, behandling.id)
        pdfBrevService.sendBrev(
            behandling,
            fagsak,
            if (erKorrigert) Brevtype.KORRIGERT_VARSEL else Brevtype.VARSEL,
            Brevdata(
                mottager = brevmottager,
                overskrift = overskrift,
                brevtekst = brevtekst,
                metadata = varselbrevsdokument.brevmetadata,
                vedleggHtml = vedlegg,
            ),
            varsletFeilutbetaling,
            fritekst,
        )
    }

    fun hentForhåndsvisningManueltVarselbrev(
        behandlingId: UUID,
        maltype: Dokumentmalstype,
        fritekst: String,
    ): ByteArray {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val (metadata, brevmottager) =
            brevmetadataUtil.lagBrevmetadataForMottakerTilForhåndsvisning(behandlingId)
        val erKorrigert = maltype == Dokumentmalstype.KORRIGERT_VARSEL
        val feilutbetalingsfakta = faktaFeilutbetalingService.hentFaktaomfeilutbetaling(behandling.id)
        val aktivtVarsel = behandling.aktivtVarsel

        val varselbrevsdokument =
            lagVarselbrev(fritekst, behandling, fagsak, brevmottager, erKorrigert, feilutbetalingsfakta, aktivtVarsel, metadata)
        val overskrift =
            lagVarselbrevsoverskrift(varselbrevsdokument.brevmetadata, erKorrigert)
        val brevtekst = lagFritekst(varselbrevsdokument, erKorrigert)
        val vedlegg = varselbrevUtil.lagVedlegg(varselbrevsdokument, behandlingId)
        return pdfBrevService.genererForhåndsvisning(
            Brevdata(
                mottager = brevmottager,
                overskrift = overskrift,
                brevtekst = brevtekst,
                metadata = varselbrevsdokument.brevmetadata,
                vedleggHtml = vedlegg,
            ),
        )
    }

    private fun lagVarselbrev(
        fritekst: String,
        behandling: Behandling,
        fagsak: Fagsak,
        brevmottager: Brevmottager,
        erKorrigert: Boolean,
        feilutbetalingsfakta: FaktaFeilutbetalingDto,
        aktivtVarsel: Varsel? = null,
        forhåndsgenerertMetadata: Brevmetadata? = null,
    ): Varselbrevsdokument {
        val metadata = forhåndsgenerertMetadata ?: run {
            // Henter data fra pdl
            val personinfo = eksterneDataForBrevService.hentPerson(fagsak.bruker.ident, fagsak.fagsystem)
            val adresseinfo: Adresseinfo =
                eksterneDataForBrevService.hentAdresse(personinfo, brevmottager, behandling.aktivVerge, fagsak.fagsystem)
            val vergenavn: String = BrevmottagerUtil.getVergenavn(behandling.aktivVerge, adresseinfo)
            varselbrevUtil.sammenstillInfoForBrevmetadata(
                behandling,
                personinfo,
                adresseinfo,
                fagsak,
                vergenavn,
                erKorrigert,
                personinfo.dødsdato != null,
            )
        }

        return varselbrevUtil.sammenstillInfoFraFagsystemerForSendingManueltVarselBrev(
            metadata.copy(tittel = getTittelForVarselbrev(fagsak.ytelsesnavn, erKorrigert)),
            fritekst,
            feilutbetalingsfakta,
            aktivtVarsel,
        )
    }
    private fun getTittelForVarselbrev(ytelsesnavn: String, erKorrigert: Boolean): String {
        return if (erKorrigert) {
            VarselbrevUtil.TITTEL_KORRIGERT_VARSEL_TILBAKEBETALING + ytelsesnavn
        } else {
            VarselbrevUtil.TITTEL_VARSEL_TILBAKEBETALING + ytelsesnavn
        }
    }
}
