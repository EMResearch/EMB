package no.nav.familie.tilbake.integration.økonomi

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.simulering.FeilutbetalingerFraSimulering
import no.nav.familie.kontrakter.felles.simulering.FeilutbetaltPeriode
import no.nav.familie.kontrakter.felles.simulering.HentFeilutbetalingerFraSimuleringRequest
import no.nav.familie.tilbake.common.exceptionhandler.IntegrasjonException
import no.nav.familie.tilbake.common.exceptionhandler.KravgrunnlagIkkeFunnetFeil
import no.nav.familie.tilbake.common.exceptionhandler.SperretKravgrunnlagFeil
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagMapper
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagAnnulerRequest
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagAnnulerResponse
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentDetaljRequest
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentDetaljResponse
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagBelopDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagPeriodeDto
import no.nav.tilbakekreving.typer.v1.MmelDto
import no.nav.tilbakekreving.typer.v1.PeriodeDto
import no.nav.tilbakekreving.typer.v1.TypeGjelderDto
import no.nav.tilbakekreving.typer.v1.TypeKlasseDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

interface OppdragClient {

    fun iverksettVedtak(
        behandlingId: UUID,
        tilbakekrevingsvedtakRequest: TilbakekrevingsvedtakRequest,
    ): TilbakekrevingsvedtakResponse

    fun hentKravgrunnlag(kravgrunnlagId: BigInteger, hentKravgrunnlagRequest: KravgrunnlagHentDetaljRequest): DetaljertKravgrunnlagDto

    fun annulerKravgrunnlag(eksternKravgrunnlagId: BigInteger, kravgrunnlagAnnulerRequest: KravgrunnlagAnnulerRequest)

    fun hentFeilutbetalingerFraSimulering(request: HentFeilutbetalingerFraSimuleringRequest): FeilutbetalingerFraSimulering
}

@Service
@Profile("!e2e & !mock-økonomi")
class DefaultOppdragClient(
    @Qualifier("azure") restOperations: RestOperations,
    @Value("\${FAMILIE_OPPDRAG_URL}") private val familieOppdragUrl: URI,
) :
    AbstractPingableRestClient(restOperations, "familie.oppdrag"), OppdragClient {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override val pingUri: URI = UriComponentsBuilder.fromUri(familieOppdragUrl)
        .path(PING_PATH).build().toUri()

    private fun iverksettelseUri(behandlingId: UUID): URI = UriComponentsBuilder.fromUri(familieOppdragUrl)
        .pathSegment(IVERKSETTELSE_PATH, behandlingId.toString()).build().toUri()

    private fun hentKravgrunnlagUri(kravgrunnlagId: BigInteger): URI = UriComponentsBuilder.fromUri(familieOppdragUrl)
        .pathSegment(HENT_KRAVGRUNNLAG_PATH, kravgrunnlagId.toString()).build().toUri()

    private fun annulerKravgrunnlagUri(kravgrunnlagId: BigInteger): URI = UriComponentsBuilder.fromUri(familieOppdragUrl)
        .pathSegment(ANNULER_KRAVGRUNNLAG_PATH, kravgrunnlagId.toString()).build().toUri()

    private val hentFeilutbetalingerFraSimuleringUri: URI = UriComponentsBuilder.fromUri(familieOppdragUrl)
        .pathSegment(HENT_FEILUTBETALINGER_PATH).build().toUri()

    override fun iverksettVedtak(behandlingId: UUID, tilbakekrevingsvedtakRequest: TilbakekrevingsvedtakRequest): TilbakekrevingsvedtakResponse {
        logger.info("Sender tilbakekrevingsvedtak til økonomi for behandling $behandlingId")
        try {
            val respons = postForEntity<Ressurs<TilbakekrevingsvedtakResponse>>(
                uri = iverksettelseUri(behandlingId),
                payload = tilbakekrevingsvedtakRequest,
            )
                .getDataOrThrow()
            if (!erResponsOk(respons.mmel)) {
                logger.error(
                    "Fikk feil respons fra økonomi ved iverksetting av behandling=$behandlingId." +
                        "Mottatt respons:${lagRespons(respons.mmel)}",
                )
                throw IntegrasjonException(
                    msg = "Fikk feil respons fra økonomi ved iverksetting av behandling=$behandlingId." +
                        "Mottatt respons:${lagRespons(respons.mmel)}",
                )
            }
            logger.info("Mottatt respons: ${lagRespons(respons.mmel)} fra økonomi ved iverksetting av behandling=$behandlingId.")
            return respons
        } catch (exception: Exception) {
            logger.error(
                "tilbakekrevingsvedtak kan ikke sende til økonomi for behandling=$behandlingId. " +
                    "Feiler med ${exception.message}.",
            )
            throw IntegrasjonException(
                msg = "Noe gikk galt ved iverksetting av behandling=$behandlingId",
                throwable = exception,
            )
        }
    }

    override fun hentKravgrunnlag(kravgrunnlagId: BigInteger, hentKravgrunnlagRequest: KravgrunnlagHentDetaljRequest): DetaljertKravgrunnlagDto {
        logger.info("Henter kravgrunnlag fra økonomi for kravgrunnlagId=$kravgrunnlagId")
        try {
            val respons = postForEntity<Ressurs<KravgrunnlagHentDetaljResponse>>(
                uri = hentKravgrunnlagUri(kravgrunnlagId),
                payload = hentKravgrunnlagRequest,
            )
                .getDataOrThrow()
            validerHentKravgrunnlagRespons(respons.mmel, kravgrunnlagId)
            logger.info("Mottatt respons: ${lagRespons(respons.mmel)} fra økonomi til kravgrunnlagId=$kravgrunnlagId.")
            return respons.detaljertkravgrunnlag
        } catch (exception: Exception) {
            logger.error(
                "Kravgrunnlag kan ikke hentes fra økonomi for eksternKravgrunnlagId=$kravgrunnlagId. " +
                    "Feiler med ${exception.message}",
            )
            if (exception.message?.contains("Kravgrunnlag ikke funnet") == true) {
                throw KravgrunnlagIkkeFunnetFeil(exception.message!!)
            }
            throw IntegrasjonException(
                msg = "Noe gikk galt ved henting av kravgrunnlag for kravgrunnlagId=$kravgrunnlagId",
                throwable = exception,
            )
        }
    }

    override fun annulerKravgrunnlag(
        eksternKravgrunnlagId: BigInteger,
        kravgrunnlagAnnulerRequest: KravgrunnlagAnnulerRequest,
    ) {
        logger.info("Annulerer kravgrunnlag for kravgrunnlagId=$eksternKravgrunnlagId")
        try {
            val respons = postForEntity<Ressurs<KravgrunnlagAnnulerResponse>>(
                uri = annulerKravgrunnlagUri(eksternKravgrunnlagId),
                payload = kravgrunnlagAnnulerRequest,
            )
                .getDataOrThrow()
            if (!erResponsOk(respons.mmel)) {
                logger.error(
                    "Fikk feil respons fra økonomi ved annulering " +
                        "av kravgrunnlag med eksternKravgrunnlagId=$eksternKravgrunnlagId." +
                        "Mottatt respons:${lagRespons(respons.mmel)}",
                )
                throw IntegrasjonException(
                    msg = "Fikk feil respons fra økonomi ved annulering " +
                        "av kravgrunnlag med eksternKravgrunnlagId=$eksternKravgrunnlagId." +
                        "Mottatt respons:${lagRespons(respons.mmel)}",
                )
            }
            logger.info("Mottatt respons: ${lagRespons(respons.mmel)} fra økonomi til kravgrunnlagId=$eksternKravgrunnlagId.")
        } catch (exception: Exception) {
            logger.error(
                "Kravgrunnlag kan ikke hentes fra økonomi for eksternKravgrunnlagId=$eksternKravgrunnlagId. " +
                    "Feiler med ${exception.message}",
            )
            throw IntegrasjonException(
                "Noe gikk galt ved henting av kravgrunnlag for kravgrunnlagId=$eksternKravgrunnlagId",
                exception,
            )
        }
    }

    override fun hentFeilutbetalingerFraSimulering(request: HentFeilutbetalingerFraSimuleringRequest): FeilutbetalingerFraSimulering {
        logger.info(
            "Henter feilubetalinger fra simulering for ytelsestype=${request.ytelsestype}, " +
                "eksternFagsakId=${request.eksternFagsakId} og eksternId=${request.fagsystemsbehandlingId}",
        )
        try {
            return postForEntity<Ressurs<FeilutbetalingerFraSimulering>>(
                uri = hentFeilutbetalingerFraSimuleringUri,
                payload = request,
            )
                .getDataOrThrow()
        } catch (exception: Exception) {
            logger.error(
                "Feilutbetalinger kan ikke hentes fra simulering for for ytelsestype=${request.ytelsestype}, " +
                    "eksternFagsakId=${request.eksternFagsakId} og eksternId=${request.fagsystemsbehandlingId} " +
                    "Feiler med ${exception.message}",
            )
            throw IntegrasjonException(
                msg = "Noe gikk galt ved henting av feilutbetalinger fra simulering",
                throwable = exception,
            )
        }
    }

    private fun validerHentKravgrunnlagRespons(mmelDto: MmelDto, kravgrunnlagId: BigInteger) {
        if (!erResponsOk(mmelDto) || erKravgrunnlagIkkeFinnes(mmelDto)) {
            logger.error(
                "Fikk feil respons:${lagRespons(mmelDto)} fra økonomi ved henting av kravgrunnlag " +
                    "for kravgrunnlagId=$kravgrunnlagId.",
            )
            throw IntegrasjonException(
                msg = "Fikk feil respons:${lagRespons(mmelDto)} fra økonomi " +
                    "ved henting av kravgrunnlag for kravgrunnlagId=$kravgrunnlagId.",
            )
        } else if (erKravgrunnlagSperret(mmelDto)) {
            logger.warn("Hentet kravgrunnlag for kravgrunnlagId=$kravgrunnlagId er sperret")
            throw SperretKravgrunnlagFeil(melding = "Hentet kravgrunnlag for kravgrunnlagId=$kravgrunnlagId er sperret")
        }
    }

    private fun erResponsOk(mmelDto: MmelDto): Boolean {
        return mmelDto.alvorlighetsgrad in setOf("00", "04")
    }

    private fun erKravgrunnlagSperret(mmelDto: MmelDto): Boolean {
        return KODE_MELDING_SPERRET_KRAVGRUNNLAG == mmelDto.kodeMelding
    }

    private fun erKravgrunnlagIkkeFinnes(mmelDto: MmelDto): Boolean {
        return KODE_MELDING_KRAVGRUNNLAG_IKKE_FINNES == mmelDto.kodeMelding
    }

    private fun lagRespons(mmelDto: MmelDto): String {
        return objectMapper.writeValueAsString(mmelDto)
    }

    companion object {

        const val KODE_MELDING_SPERRET_KRAVGRUNNLAG = "B420012I"
        const val KODE_MELDING_KRAVGRUNNLAG_IKKE_FINNES = "B420010I"

        const val IVERKSETTELSE_PATH = "api/tilbakekreving/iverksett"
        const val HENT_KRAVGRUNNLAG_PATH = "api/tilbakekreving/kravgrunnlag"
        const val ANNULER_KRAVGRUNNLAG_PATH = "api/tilbakekreving/annuler/kravgrunnlag"
        const val HENT_FEILUTBETALINGER_PATH = "api/simulering/feilutbetalinger"
        const val PING_PATH = "internal/status/alive"
    }
}

@Service
@Profile("e2e", "mock-økonomi")
class MockOppdragClient(
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val økonomiXmlMottattRepository: ØkonomiXmlMottattRepository,
) : OppdragClient {

    override fun iverksettVedtak(
        behandlingId: UUID,
        tilbakekrevingsvedtakRequest: TilbakekrevingsvedtakRequest,
    ): TilbakekrevingsvedtakResponse {
        logger.info("Sender mock iverksettelse respons i e2e-profil")
        val mmelDto = MmelDto()
        mmelDto.alvorlighetsgrad = "00"
        val response = TilbakekrevingsvedtakResponse()
        response.mmel = mmelDto
        return response
    }

    override fun hentKravgrunnlag(
        kravgrunnlagId: BigInteger,
        hentKravgrunnlagRequest: KravgrunnlagHentDetaljRequest,
    ): DetaljertKravgrunnlagDto {
        logger.info("Henter kravgrunnlag fra økonomi for kravgrunnlagId=$kravgrunnlagId")
        val respons = lagKravgrunnlagRespons(hentKravgrunnlagRequest)
        logger.info("Mottatt respons: ${lagRespons(respons.mmel)} fra økonomi til kravgrunnlagId=$kravgrunnlagId.")
        return respons.detaljertkravgrunnlag
    }

    override fun annulerKravgrunnlag(eksternKravgrunnlagId: BigInteger, kravgrunnlagAnnulerRequest: KravgrunnlagAnnulerRequest) {
        logger.info("Kaller mock annulering request i e2e-profil")
    }

    override fun hentFeilutbetalingerFraSimulering(request: HentFeilutbetalingerFraSimuleringRequest): FeilutbetalingerFraSimulering {
        logger.info(
            "Henter feilubetalinger fra simulering i e2e-profil for ytelsestype=${request.ytelsestype}, " +
                "eksternFagsakId=${request.eksternFagsakId} og eksternId=${request.fagsystemsbehandlingId}",
        )
        val feilutbetaltPeriode = FeilutbetaltPeriode(
            fom = YearMonth.now().minusMonths(2).atDay(1),
            tom = YearMonth.now().minusMonths(1).atDay(1),
            feilutbetaltBeløp = BigDecimal("20000"),
            tidligereUtbetaltBeløp = BigDecimal("30000"),
            nyttBeløp = BigDecimal("10000"),
        )
        return FeilutbetalingerFraSimulering(feilutbetaltePerioder = listOf(feilutbetaltPeriode))
    }

    fun lagKravgrunnlagRespons(request: KravgrunnlagHentDetaljRequest): KravgrunnlagHentDetaljResponse {
        val hentKravgrunnlagRequest = request.hentkravgrunnlag
        val eksisterendeKravgrunnlag = kravgrunnlagRepository
            .findByEksternKravgrunnlagIdAndAktivIsTrue(
                hentKravgrunnlagRequest
                    .kravgrunnlagId,
            )
            ?: hentMottattKravgrunnlag(hentKravgrunnlagRequest.kravgrunnlagId)

        val respons = KravgrunnlagHentDetaljResponse()
        respons.mmel = lagMmelDto()

        respons.detaljertkravgrunnlag = DetaljertKravgrunnlagDto().apply {
            kravgrunnlagId = hentKravgrunnlagRequest.kravgrunnlagId
            enhetAnsvarlig = hentKravgrunnlagRequest.enhetAnsvarlig
            enhetBehandl = hentKravgrunnlagRequest.enhetAnsvarlig
            enhetBosted = hentKravgrunnlagRequest.enhetAnsvarlig
            saksbehId = hentKravgrunnlagRequest.saksbehId
            kodeFagomraade = Fagområdekode.BA.name
            vedtakId = eksisterendeKravgrunnlag?.vedtakId ?: BigInteger.ZERO
            kodeStatusKrav = Kravstatuskode.NYTT.kode
            fagsystemId = eksisterendeKravgrunnlag?.fagsystemId ?: "0"
            datoVedtakFagsystem = eksisterendeKravgrunnlag?.fagsystemVedtaksdato ?: LocalDate.now()
            vedtakIdOmgjort = eksisterendeKravgrunnlag?.omgjortVedtakId ?: BigInteger.ZERO
            vedtakGjelderId = eksisterendeKravgrunnlag?.gjelderVedtakId ?: "1234"
            typeGjelderId = TypeGjelderDto.PERSON
            utbetalesTilId = eksisterendeKravgrunnlag?.utbetalesTilId ?: "1234"
            typeUtbetId = TypeGjelderDto.PERSON
            kontrollfelt = eksisterendeKravgrunnlag?.kontrollfelt ?: LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("YYYY-MM-dd-HH.mm.ss.SSSSSS"))
            referanse = eksisterendeKravgrunnlag?.referanse ?: "0"
            tilbakekrevingsPeriode.addAll(mapPeriode(eksisterendeKravgrunnlag?.perioder!!))
        }
        return respons
    }

    private fun lagMmelDto(): MmelDto {
        val mmelDto = MmelDto()
        mmelDto.alvorlighetsgrad = "00"
        mmelDto.kodeMelding = "OK"
        return mmelDto
    }

    private fun mapPeriode(perioder: Set<Kravgrunnlagsperiode432>): List<DetaljertKravgrunnlagPeriodeDto> {
        return perioder.map {
            DetaljertKravgrunnlagPeriodeDto().apply {
                periode = PeriodeDto().apply {
                    fom = it.periode.fomDato
                    tom = it.periode.tomDato
                }
                belopSkattMnd = it.månedligSkattebeløp
                tilbakekrevingsBelop.addAll(mapBeløp(it.beløp))
            }
        }
    }

    private fun mapBeløp(beløper: Set<Kravgrunnlagsbeløp433>): List<DetaljertKravgrunnlagBelopDto> {
        return beløper.map {
            DetaljertKravgrunnlagBelopDto().apply {
                kodeKlasse = it.klassekode.name
                typeKlasse = TypeKlasseDto.fromValue(it.klassetype.name)
                belopNy = it.nyttBeløp
                belopOpprUtbet = it.opprinneligUtbetalingsbeløp
                belopUinnkrevd = it.uinnkrevdBeløp
                belopTilbakekreves = it.tilbakekrevesBeløp
                skattProsent = it.skatteprosent
            }
        }
    }

    private fun lagRespons(mmelDto: MmelDto): String {
        return objectMapper.writeValueAsString(mmelDto)
    }

    private fun hentMottattKravgrunnlag(eksternKravgrunnlagId: BigInteger): Kravgrunnlag431? {
        val mottattXml = økonomiXmlMottattRepository
            .findByEksternKravgrunnlagId(eksternKravgrunnlagId)?.melding
        return mottattXml?.let {
            KravgrunnlagMapper.tilKravgrunnlag431(
                KravgrunnlagUtil.unmarshalKravgrunnlag(it),
                UUID.randomUUID(),
            )
        }
    }

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
