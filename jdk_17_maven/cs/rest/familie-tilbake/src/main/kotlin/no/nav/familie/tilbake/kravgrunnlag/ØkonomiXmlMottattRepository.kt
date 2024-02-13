package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottattIdOgYtelse
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.time.LocalDate
import java.util.UUID

@Repository
@Transactional
interface ØkonomiXmlMottattRepository : RepositoryInterface<ØkonomiXmlMottatt, UUID>, InsertUpdateRepository<ØkonomiXmlMottatt> {

    fun findByEksternKravgrunnlagIdAndVedtakId(eksternKravgrunnlagId: BigInteger, vedtakId: BigInteger): List<ØkonomiXmlMottatt>

    fun findByEksternFagsakIdAndYtelsestypeAndVedtakId(
        eksternFagsakId: String,
        ytelsestype: Ytelsestype,
        vedtakId: BigInteger,
    ): List<ØkonomiXmlMottatt>

    fun findByEksternFagsakIdAndYtelsestype(
        eksternFagsakId: String,
        ytelsestype: Ytelsestype,
    ): List<ØkonomiXmlMottatt>

    fun existsByEksternFagsakIdAndYtelsestypeAndReferanse(
        eksternFagsakId: String,
        ytelsestype: Ytelsestype,
        referanse: String,
    ): Boolean

    fun findByEksternKravgrunnlagId(eksternKravgrunnlagId: BigInteger): ØkonomiXmlMottatt?

    // language=PostgreSQL
    @Query(
        """ 
        SELECT oko.id, oko.ytelsestype
        FROM okonomi_xml_mottatt oko
        WHERE CASE (ytelsestype)
                    WHEN 'BARNETRYGD' THEN opprettet_tid < :barnetrygdBestemtDato
                    WHEN 'BARNETILSYN' THEN opprettet_tid < :barnetilsynBestemtDato
                    WHEN 'OVERGANGSSTØNAD' THEN opprettet_tid < :overgangsstonadbestemtdato
                    WHEN 'SKOLEPENGER' THEN opprettet_tid < :skolePengerBestemtDato
                    WHEN 'KONTANTSTØTTE' THEN opprettet_tid < :kontantstottebestemtdato
               END
      """,
    )
    fun hentFrakobletGamleMottattXmlIds(
        barnetrygdBestemtDato: LocalDate,
        barnetilsynBestemtDato: LocalDate,
        overgangsstonadbestemtdato: LocalDate,
        skolePengerBestemtDato: LocalDate,
        kontantstottebestemtdato: LocalDate,
    ): List<ØkonomiXmlMottattIdOgYtelse>
}
