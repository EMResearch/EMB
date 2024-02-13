package no.nav.familie.tilbake.dokumentbestilling.varsel

import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.simulering.HentFeilutbetalingerFraSimuleringRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.FeilutbetaltePerioderDto
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingDto
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.beregning.KravgrunnlagsberegningService
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.varsel.handlebars.dto.FeilutbetaltPeriode
import no.nav.familie.tilbake.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument
import no.nav.familie.tilbake.dokumentbestilling.varsel.handlebars.dto.Vedleggsdata
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.integration.økonomi.OppdragClient
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.organisasjon.OrganisasjonService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
class VarselbrevUtil(
    private val eksterneDataForBrevService: EksterneDataForBrevService,
    private val oppdragClient: OppdragClient,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val organisasjonService: OrganisasjonService,
) {

    companion object {

        const val TITTEL_KORRIGERT_VARSEL_TILBAKEBETALING = "Korrigert Varsel tilbakebetaling "
        const val TITTEL_VARSEL_TILBAKEBETALING = "Varsel tilbakebetaling "
    }

    fun sammenstillInfoForForhåndvisningVarselbrev(
        adresseinfo: Adresseinfo,
        request: ForhåndsvisVarselbrevRequest,
        personinfo: Personinfo,
    ): Varselbrevsdokument {
        val tittel = getTittelForVarselbrev(request.ytelsestype.navn[request.språkkode]!!, false)
        val vergenavn = BrevmottagerUtil.getVergenavn(request.verge, adresseinfo)
        val ansvarligSaksbehandler =
            eksterneDataForBrevService.hentPåloggetSaksbehandlernavnMedDefault(ContextService.hentSaksbehandler())

        val metadata = Brevmetadata(
            sakspartId = personinfo.ident,
            sakspartsnavn = personinfo.navn,
            finnesVerge = request.verge != null,
            vergenavn = vergenavn,
            mottageradresse = adresseinfo,
            behandlendeEnhetId = request.behandlendeEnhetId,
            behandlendeEnhetsNavn = request.behandlendeEnhetsNavn,
            ansvarligSaksbehandler = ansvarligSaksbehandler,
            saksnummer = request.eksternFagsakId,
            språkkode = request.språkkode,
            ytelsestype = request.ytelsestype,
            tittel = tittel,
            gjelderDødsfall = personinfo.dødsdato != null,
            institusjon = request.institusjon?.let {
                organisasjonService.mapTilInstitusjonForBrevgenerering(it.organisasjonsnummer)
            },
        )

        return Varselbrevsdokument(
            brevmetadata = metadata,
            beløp = request.feilutbetaltePerioderDto.sumFeilutbetaling,
            revurderingsvedtaksdato = request.vedtaksdato ?: LocalDate.now(),
            fristdatoForTilbakemelding = Constants.brukersSvarfrist(),
            varseltekstFraSaksbehandler = request.varseltekst,
            feilutbetaltePerioder = mapFeilutbetaltePerioder(request.feilutbetaltePerioderDto),
        )
    }

    fun sammenstillInfoForBrevmetadata(
        behandling: Behandling,
        personinfo: Personinfo,
        adresseinfo: Adresseinfo,
        fagsak: Fagsak,
        vergenavn: String?,
        erKorrigert: Boolean,
        gjelderDødsfall: Boolean,
    ): Brevmetadata {
        val ansvarligSaksbehandler =
            eksterneDataForBrevService.hentPåloggetSaksbehandlernavnMedDefault(behandling.ansvarligSaksbehandler)

        return Brevmetadata(
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
            tittel = getTittelForVarselbrev(fagsak.ytelsesnavn, erKorrigert),
            gjelderDødsfall = gjelderDødsfall,
            institusjon = fagsak.institusjon?.let {
                organisasjonService.mapTilInstitusjonForBrevgenerering(it.organisasjonsnummer)
            },
        )
    }

    fun sammenstillInfoFraFagsystemerForSendingManueltVarselBrev(
        metadata: Brevmetadata,
        fritekst: String?,
        feilutbetalingsfakta: FaktaFeilutbetalingDto,
        varsel: Varsel?,
    ): Varselbrevsdokument {
        return Varselbrevsdokument(
            brevmetadata = metadata,
            beløp = feilutbetalingsfakta.totaltFeilutbetaltBeløp.toLong(),
            revurderingsvedtaksdato = feilutbetalingsfakta.revurderingsvedtaksdato,
            fristdatoForTilbakemelding = Constants.brukersSvarfrist(),
            varseltekstFraSaksbehandler = fritekst,
            feilutbetaltePerioder = mapFeilutbetaltePerioder(feilutbetalingsfakta),
            erKorrigert = varsel != null,
            varsletDato = varsel?.sporbar?.opprettetTid?.toLocalDate(),
            varsletBeløp = varsel?.varselbeløp,
        )
    }

    private fun sammenstillInfoFraSimuleringForVedlegg(
        varselbrevsdokument: Varselbrevsdokument,
        eksternBehandlingId: String,
        varsletTotalbeløp: Long,
    ): Vedleggsdata {
        val request = HentFeilutbetalingerFraSimuleringRequest(
            varselbrevsdokument.ytelsestype,
            varselbrevsdokument.brevmetadata.saksnummer,
            eksternBehandlingId,
        )

        val feilutbetalingerFraSimulering = oppdragClient.hentFeilutbetalingerFraSimulering(request)

        val perioder = feilutbetalingerFraSimulering.feilutbetaltePerioder.map {
            FeilutbetaltPeriode(
                YearMonth.from(it.fom),
                it.nyttBeløp,
                it.tidligereUtbetaltBeløp,
                it.feilutbetaltBeløp,
            )
        }

        validerKorrektTotalbeløp(
            perioder,
            varsletTotalbeløp,
            varselbrevsdokument.ytelsestype,
            varselbrevsdokument.brevmetadata.saksnummer,
            eksternBehandlingId,
        )
        return Vedleggsdata(varselbrevsdokument.språkkode, varselbrevsdokument.isYtelseMedSkatt, perioder)
    }

    private fun sammenstillInfoFraKravgrunnlag(
        varselbrevsdokument: Varselbrevsdokument,
        behandlingId: UUID,
    ): Vedleggsdata {
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)

        val beregningsresultat = KravgrunnlagsberegningService.summerKravgrunnlagBeløpForPerioder(kravgrunnlag)

        val perioder = beregningsresultat.map {
            FeilutbetaltPeriode(
                YearMonth.from(it.key.fom),
                it.value.riktigYtelsesbeløp,
                it.value.utbetaltYtelsesbeløp,
                it.value.feilutbetaltBeløp,
            )
        }

        return Vedleggsdata(varselbrevsdokument.språkkode, varselbrevsdokument.isYtelseMedSkatt, perioder)
    }

    fun lagVedlegg(
        varselbrevsdokument: Varselbrevsdokument,
        fagsystemsbehandlingId: String?,
        varsletTotalbeløp: Long,
    ): String {
        return if (varselbrevsdokument.harVedlegg) {
            if (fagsystemsbehandlingId == null) {
                error(
                    "fagsystemsbehandlingId mangler for forhåndsvisning av varselbrev. " +
                        "Saksnummer ${varselbrevsdokument.brevmetadata.saksnummer}",
                )
            }

            val vedleggsdata =
                sammenstillInfoFraSimuleringForVedlegg(varselbrevsdokument, fagsystemsbehandlingId, varsletTotalbeløp)
            TekstformatererVarselbrev.lagVarselbrevsvedleggHtml(vedleggsdata)
        } else {
            ""
        }
    }

    fun lagVedlegg(varselbrevsdokument: Varselbrevsdokument, behandlingId: UUID): String {
        return if (varselbrevsdokument.harVedlegg) {
            val vedleggsdata = sammenstillInfoFraKravgrunnlag(varselbrevsdokument, behandlingId)
            TekstformatererVarselbrev.lagVarselbrevsvedleggHtml(vedleggsdata)
        } else {
            ""
        }
    }

    private fun validerKorrektTotalbeløp(
        feilutbetaltePerioder: List<FeilutbetaltPeriode>,
        varsletTotalFeilutbetaltBeløp: Long,
        ytelsestype: Ytelsestype,
        eksternFagsakId: String,
        eksternId: String,
    ) {
        if (feilutbetaltePerioder.sumOf { it.feilutbetaltBeløp.toLong() } != varsletTotalFeilutbetaltBeløp) {
            throw Feil(
                "Varslet totalFeilutbetaltBeløp matcher ikke med hentet totalFeilutbetaltBeløp fra " +
                    "simulering for ytelsestype=$ytelsestype, eksternFagsakId=$eksternFagsakId og eksternId=$eksternId",
            )
        }
    }

    private fun getTittelForVarselbrev(ytelsesnavn: String, erKorrigert: Boolean): String {
        return if (erKorrigert) {
            TITTEL_KORRIGERT_VARSEL_TILBAKEBETALING + ytelsesnavn
        } else {
            TITTEL_VARSEL_TILBAKEBETALING + ytelsesnavn
        }
    }

    private fun mapFeilutbetaltePerioder(feilutbetaltePerioderDto: FeilutbetaltePerioderDto): List<Datoperiode> {
        return feilutbetaltePerioderDto.perioder.map { Datoperiode(it.fom, it.tom) }
    }

    private fun mapFeilutbetaltePerioder(feilutbetalingsfakta: FaktaFeilutbetalingDto): List<Datoperiode> {
        return feilutbetalingsfakta.feilutbetaltePerioder.map { Datoperiode(it.periode.fom, it.periode.tom) }
    }
}
