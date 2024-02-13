package no.nav.familie.tilbake.foreldelse.domain

import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.util.UUID

data class Foreldelsesperiode(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val periode: Månedsperiode,
    val foreldelsesvurderingstype: Foreldelsesvurderingstype,
    val begrunnelse: String,
    val foreldelsesfrist: LocalDate? = null,
    val oppdagelsesdato: LocalDate? = null,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {

    fun erForeldet(): Boolean {
        return Foreldelsesvurderingstype.FORELDET == foreldelsesvurderingstype
    }
}

enum class Foreldelsesvurderingstype(val navn: String) {
    IKKE_VURDERT("Perioden er ikke vurdert"),
    FORELDET("Perioden er foreldet"),
    IKKE_FORELDET("Perioden er ikke foreldet"),
    TILLEGGSFRIST("Perioden er ikke foreldet, regel om tilleggsfrist (10 år) benyttes"),
}
