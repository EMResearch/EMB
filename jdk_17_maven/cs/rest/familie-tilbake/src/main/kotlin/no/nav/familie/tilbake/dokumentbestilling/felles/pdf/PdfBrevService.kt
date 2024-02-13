package no.nav.familie.tilbake.dokumentbestilling.felles.pdf

import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstidspunkt
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.header.TekstformatererHeader
import no.nav.familie.tilbake.dokumentbestilling.felles.task.PubliserJournalpostTask
import no.nav.familie.tilbake.dokumentbestilling.felles.task.PubliserJournalpostTaskData
import no.nav.familie.tilbake.dokumentbestilling.fritekstbrev.JournalpostIdOgDokumentId
import no.nav.familie.tilbake.integration.pdl.internal.secureLogger
import no.nav.familie.tilbake.micrometer.TellerService
import no.nav.familie.tilbake.pdfgen.Dokumentvariant
import no.nav.familie.tilbake.pdfgen.PdfGenerator
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.Base64
import java.util.Properties

@Service
class PdfBrevService(
    private val journalføringService: JournalføringService,
    private val tellerService: TellerService,
    private val taskService: TaskService,
) {

    private val logger = LoggerFactory.getLogger(PdfBrevService::class.java)
    private val pdfGenerator: PdfGenerator = PdfGenerator()

    fun genererForhåndsvisning(data: Brevdata): ByteArray {
        val html = lagHtml(data)
        return pdfGenerator.genererPDFMedLogo(html, Dokumentvariant.UTKAST)
    }

    fun sendBrev(
        behandling: Behandling,
        fagsak: Fagsak,
        brevtype: Brevtype,
        data: Brevdata,
        varsletBeløp: Long? = null,
        fritekst: String? = null,
    ) {
        valider(brevtype, varsletBeløp)
        val dokumentreferanse: JournalpostIdOgDokumentId = lagOgJournalførBrev(behandling, fagsak, brevtype, data)
        if (data.mottager != Brevmottager.VERGE &&
            !data.metadata.annenMottakersNavn.equals(data.metadata.sakspartsnavn, ignoreCase = true)
        ) {
            // Ikke tell kopier sendt til verge eller fullmektig
            tellerService.tellBrevSendt(fagsak, brevtype)
        }
        lagTaskerForUtsendingOgSporing(behandling, fagsak, brevtype, varsletBeløp, fritekst, data, dokumentreferanse)
    }

    private fun lagTaskerForUtsendingOgSporing(
        behandling: Behandling,
        fagsak: Fagsak,
        brevtype: Brevtype,
        varsletBeløp: Long?,
        fritekst: String?,
        brevdata: Brevdata,
        dokumentreferanse: JournalpostIdOgDokumentId,
    ) {
        val payload = objectMapper.writeValueAsString(
            PubliserJournalpostTaskData(
                behandlingId = behandling.id,
                manuellAdresse = brevdata.metadata.mottageradresse.manuellAdresse,
            ),
        )
        val properties: Properties = Properties().apply {
            setProperty("journalpostId", dokumentreferanse.journalpostId)
            setProperty(PropertyName.FAGSYSTEM, fagsak.fagsystem.name)
            setProperty("dokumentId", dokumentreferanse.dokumentId)
            setProperty("mottager", brevdata.mottager.name)
            setProperty("brevtype", brevtype.name)
            setProperty("ansvarligSaksbehandler", behandling.ansvarligSaksbehandler)
            setProperty("distribusjonstype", utledDistribusjonstype(brevtype).name)
            setProperty("distribusjonstidspunkt", distribusjonstidspunkt)
            varsletBeløp?.also { setProperty("varselbeløp", varsletBeløp.toString()) }
            fritekst?.also { setProperty("fritekst", Base64.getEncoder().encodeToString(fritekst.toByteArray())) }
            brevdata.tittel?.also { setProperty("tittel", it) }
        }
        logger.info(
            "Oppretter task for publisering av brev for behandlingId=${behandling.id}, eksternFagsakId=${fagsak.eksternFagsakId}",
        )
        taskService.save(Task(PubliserJournalpostTask.TYPE, payload, properties))
    }

    private fun lagOgJournalførBrev(
        behandling: Behandling,
        fagsak: Fagsak,
        brevtype: Brevtype,
        data: Brevdata,
    ): JournalpostIdOgDokumentId {
        val html = lagHtml(data)

        val pdf = try {
            pdfGenerator.genererPDFMedLogo(html, Dokumentvariant.ENDELIG)
        } catch (e: Exception) {
            secureLogger.info("Feil ved generering av brev: brevData=$data, html=$html", e)
            throw e
        }

        val dokumentkategori = mapBrevtypeTilDokumentkategori(brevtype)
        val eksternReferanseId = lagEksternReferanseId(behandling, brevtype, data.mottager)

        try {
            return journalføringService.journalførUtgåendeBrev(
                behandling,
                fagsak,
                dokumentkategori,
                data.metadata,
                data.mottager,
                pdf,
                eksternReferanseId,
            )
        } catch (ressursException: RessursException) {
            if (ressursException.httpStatus == HttpStatus.CONFLICT) {
                logger.info("Dokarkiv svarte med 409 CONFLICT. Forsøker å hente eksisterende journalpost for $dokumentkategori")
                val journalpost =
                    journalføringService.hentJournalposter(behandling.id).find { it.eksternReferanseId == eksternReferanseId }
                        ?: error("Klarte ikke finne igjen opprettet journalpost med eksternReferanseId $eksternReferanseId")

                return JournalpostIdOgDokumentId(
                    journalpostId = journalpost.journalpostId,
                    dokumentId = journalpost.dokumenter?.first()?.dokumentInfoId ?: error(
                        "Feil ved Journalføring av $dokumentkategori til ${data.mottager} for behandlingId=${behandling.id}",
                    ),
                )
            }
            throw ressursException
        }
    }

    private fun lagEksternReferanseId(behandling: Behandling, brevtype: Brevtype, mottager: Brevmottager): String {
        // alle brev kan potensielt bli sendt til både bruker og kopi verge. 2 av breva kan potensielt bli sendt flere gonger
        val callId = MDC.get(MDCConstants.MDC_CALL_ID)
        return "${behandling.eksternBrukId}_${brevtype.name.lowercase()}_${mottager.name.lowercase()}_$callId"
    }

    private fun mapBrevtypeTilDokumentkategori(brevtype: Brevtype): Dokumentkategori {
        return if (Brevtype.VEDTAK === brevtype) {
            Dokumentkategori.VEDTAKSBREV
        } else {
            Dokumentkategori.BREV
        }
    }

    private fun lagHtml(data: Brevdata): String {
        val header = lagHeader(data)
        val innholdHtml = lagInnhold(data)
        return header + innholdHtml + data.vedleggHtml
    }

    private fun lagInnhold(data: Brevdata): String {
        return DokprodTilHtml.dokprodInnholdTilHtml(data.brevtekst)
    }

    private fun lagHeader(data: Brevdata): String {
        return TekstformatererHeader.lagHeader(
            brevmetadata = data.metadata,
            overskrift = data.overskrift,
        )
    }

    private fun utledDistribusjonstype(brevtype: Brevtype): Distribusjonstype {
        return when (brevtype) {
            Brevtype.VARSEL, Brevtype.KORRIGERT_VARSEL, Brevtype.INNHENT_DOKUMENTASJON -> Distribusjonstype.VIKTIG
            Brevtype.VEDTAK -> Distribusjonstype.VEDTAK
            Brevtype.HENLEGGELSE -> Distribusjonstype.ANNET
        }
    }

    private val distribusjonstidspunkt = Distribusjonstidspunkt.KJERNETID.name

    companion object {

        private fun valider(brevtype: Brevtype, varsletBeløp: Long?) {
            val harVarsletBeløp = varsletBeløp != null
            require(brevtype.gjelderVarsel() == harVarsletBeløp) {
                "Utvikler-feil: Varslet beløp skal brukes hvis, og bare hvis, brev gjelder varsel"
            }
        }
    }
}
