package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottattArkiv
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottattIdOgYtelse
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.time.LocalDate
import java.util.UUID

@Service
class ØkonomiXmlMottattService(
    private val mottattXmlRepository: ØkonomiXmlMottattRepository,
    private val mottattXmlArkivRepository: ØkonomiXmlMottattArkivRepository,
) {

    fun lagreMottattXml(
        kravgrunnlagXml: String,
        kravgrunnlag: DetaljertKravgrunnlagDto,
        ytelsestype: Ytelsestype,
    ) {
        mottattXmlRepository.insert(
            ØkonomiXmlMottatt(
                melding = kravgrunnlagXml,
                kravstatuskode = Kravstatuskode.fraKode(kravgrunnlag.kodeStatusKrav),
                eksternFagsakId = kravgrunnlag.fagsystemId,
                ytelsestype = ytelsestype,
                referanse = kravgrunnlag.referanse,
                eksternKravgrunnlagId = kravgrunnlag.kravgrunnlagId,
                vedtakId = kravgrunnlag.vedtakId,
                kontrollfelt = kravgrunnlag.kontrollfelt,
            ),
        )
    }

    fun hentMottattKravgrunnlag(eksternKravgrunnlagId: BigInteger, vedtakId: BigInteger): List<ØkonomiXmlMottatt> {
        return mottattXmlRepository.findByEksternKravgrunnlagIdAndVedtakId(eksternKravgrunnlagId, vedtakId)
    }

    fun arkiverEksisterendeGrunnlag(kravgrunnlag: DetaljertKravgrunnlagDto) {
        val eksisterendeKravgrunnlag: List<ØkonomiXmlMottatt> =
            hentMottattKravgrunnlag(
                eksternKravgrunnlagId = kravgrunnlag.kravgrunnlagId,
                vedtakId = kravgrunnlag.vedtakId,
            )
        eksisterendeKravgrunnlag.forEach {
            arkiverMottattXml(
                mottattXml = it.melding,
                fagsystemId = it.eksternFagsakId,
                ytelsestype = it.ytelsestype,
            )
        }
        eksisterendeKravgrunnlag.forEach { slettMottattXml(it.id) }
    }

    fun hentMottattKravgrunnlag(
        eksternFagsakId: String,
        ytelsestype: Ytelsestype,
        vedtakId: BigInteger,
    ): List<ØkonomiXmlMottatt> {
        val mottattXmlListe = mottattXmlRepository
            .findByEksternFagsakIdAndYtelsestypeAndVedtakId(eksternFagsakId, ytelsestype, vedtakId)
        val kravgrunnlagXmlListe = mottattXmlListe.filter { it.melding.contains(Constants.kravgrunnlagXmlRootElement) }
        if (kravgrunnlagXmlListe.isEmpty()) {
            throw Feil(
                message = "Det finnes intet kravgrunnlag for fagsystemId=$eksternFagsakId og " +
                    "ytelsestype=$ytelsestype",
            )
        }
        return kravgrunnlagXmlListe
    }

    fun hentFrakobletGamleMottattXmlIds(
        barnetrygdBestemtDato: LocalDate,
        barnetilsynBestemtDato: LocalDate,
        overgangsstønadBestemtDato: LocalDate,
        skolePengerBestemtDato: LocalDate,
        kontantStøtteBestemtDato: LocalDate,
    ): List<ØkonomiXmlMottattIdOgYtelse> {
        return mottattXmlRepository.hentFrakobletGamleMottattXmlIds(
            barnetrygdBestemtDato = barnetrygdBestemtDato,
            barnetilsynBestemtDato = barnetilsynBestemtDato,
            overgangsstonadbestemtdato = overgangsstønadBestemtDato,
            skolePengerBestemtDato = skolePengerBestemtDato,
            kontantstottebestemtdato = kontantStøtteBestemtDato,
        )
    }

    fun hentMottattKravgrunnlag(mottattXmlId: UUID): ØkonomiXmlMottatt {
        return mottattXmlRepository.findByIdOrThrow(mottattXmlId)
    }

    fun oppdaterMottattXml(mottattXml: ØkonomiXmlMottatt) {
        mottattXmlRepository.update(mottattXml)
    }

    fun slettMottattXml(mottattXmlId: UUID) {
        mottattXmlRepository.deleteById(mottattXmlId)
    }

    fun arkiverMottattXml(
        mottattXml: String,
        fagsystemId: String,
        ytelsestype: Ytelsestype,
    ) {
        mottattXmlArkivRepository.insert(
            ØkonomiXmlMottattArkiv(
                melding = mottattXml,
                eksternFagsakId = fagsystemId,
                ytelsestype = ytelsestype,
            ),
        )
    }

    fun hentArkiverteMottattXml(
        eksternFagsakId: String,
        ytelsestype: Ytelsestype,
    ): List<ØkonomiXmlMottattArkiv> {
        return mottattXmlArkivRepository.findByEksternFagsakIdAndYtelsestype(eksternFagsakId, ytelsestype)
    }
}
