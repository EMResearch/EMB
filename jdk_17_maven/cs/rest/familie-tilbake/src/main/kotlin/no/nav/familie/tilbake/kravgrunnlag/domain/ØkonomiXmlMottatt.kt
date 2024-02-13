package no.nav.familie.tilbake.kravgrunnlag.domain

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.math.BigInteger
import java.util.UUID

@Table("okonomi_xml_mottatt")
data class ØkonomiXmlMottatt(
    @Id
    val id: UUID = UUID.randomUUID(),
    val melding: String,
    val kravstatuskode: Kravstatuskode,
    val eksternFagsakId: String,
    val ytelsestype: Ytelsestype,
    val referanse: String,
    val eksternKravgrunnlagId: BigInteger?,
    val vedtakId: BigInteger,
    val kontrollfelt: String?,
    val sperret: Boolean = false,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
