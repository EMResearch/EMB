package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottattArkiv
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
@Transactional
interface ØkonomiXmlMottattArkivRepository :
    RepositoryInterface<ØkonomiXmlMottattArkiv, UUID>,
    InsertUpdateRepository<ØkonomiXmlMottattArkiv> {

    fun findByEksternFagsakIdAndYtelsestype(eksternFagsakId: String, ytelsestype: Ytelsestype): List<ØkonomiXmlMottattArkiv>
}
