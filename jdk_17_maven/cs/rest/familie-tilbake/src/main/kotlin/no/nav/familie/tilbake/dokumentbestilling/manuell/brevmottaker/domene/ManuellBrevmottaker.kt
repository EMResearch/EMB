package no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene

import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class ManuellBrevmottaker(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val type: MottakerType,
    var vergetype: Vergetype? = null,
    val navn: String,
    val ident: String? = null,
    @Column("org_nr")
    val orgNr: String? = null,
    @Column("adresselinje_1")
    val adresselinje1: String? = null,
    @Column("adresselinje_2")
    val adresselinje2: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val landkode: String? = null,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {
    override fun toString(): String = "${javaClass.simpleName}(id=$id,behandlingId=$behandlingId)"

    fun hasManuellAdresse(): Boolean {
        return !(
            adresselinje1.isNullOrBlank() ||
                postnummer.isNullOrBlank() ||
                poststed.isNullOrBlank() ||
                landkode.isNullOrBlank()
            )
    }

    val erTilleggsmottaker get() = type == MottakerType.VERGE || type == MottakerType.FULLMEKTIG
}
